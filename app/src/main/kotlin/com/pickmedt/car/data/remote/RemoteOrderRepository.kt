package com.pickmedt.car.data.remote

import android.util.Log
import com.pickmedt.car.core.model.EtaReading
import com.pickmedt.car.core.model.GeoPoint
import com.pickmedt.car.core.model.MenuOption
import com.pickmedt.car.core.model.Money
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.model.OrderLine
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.core.model.QuickOrder
import com.pickmedt.car.core.model.StoreSummary
import com.pickmedt.car.data.order.OrderRepository
import com.pickmedt.car.data.order.Quote
import com.pickmedt.car.data.order.QuoteChangedException
import com.pickmedt.car.data.order.QuoteIssue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

/**
 * 실서버 주문 저장소 (pick-me-be 2차 기획 반영, PR #11 1ef2b1c).
 *
 * - 상태·제조 타이밍·주문번호·응답 지연은 서버 값을 그대로 쓴다
 * - 취소 거절은 서버 응답에 남지 않아(cancelRequest를 지움), 「취소 확인 중 → 진행 상태」 변화를 보고 판단한다
 * - 텔레메트리는 etaSec만 필수. 위치·속도는 알면 함께 보낸다(서버 기록용)
 * - 등록 매장·메뉴는 서버의 자주 주문 메뉴(/v1/me/favorites) 중 매장이 정해진 것이다
 */
class RemoteOrderRepository(
    private val session: RemoteSession,
    private val scope: CoroutineScope,
    private val position: suspend () -> GeoPoint?,
    private val speedKmh: () -> Float?,
) : OrderRepository {
    private val state = MutableStateFlow<Order?>(null)
    override val currentOrder: StateFlow<Order?> = state.asStateFlow()

    private val realtime = RealtimeClient(
        baseUrl = session.baseUrl,
        onOrderEvent = { scope.launch { runCatching { refresh() } } },
        onConnected = { scope.launch { runCatching { refresh() } } },
    )

    private val lock = Mutex()
    private var trackedOrderId: String? = null
    private var lastStage: OrderStage? = null

    /** 매장이 취소 요청을 거절했는지. 새 취소 요청을 보내면 지운다 */
    private var cancelRejected = false

    /** 마지막으로 불러온 자주 주문 메뉴. 견적·예약에 매장 ID가 필요하다 */
    private var favorites: Map<String, FavoriteDto> = emptyMap()
    private val menus = mutableMapOf<String, MenuDto>()

    /** 방금 등록한 메뉴를 홈 첫 줄에 보여 준다 */
    private var preferredQuickId: String? = null

    override suspend fun quickOrders(): List<QuickOrder> {
        connectRealtime()
        val list = session.authorized { http -> http.json.decodeFromJsonElement<List<FavoriteDto>>(http.get("/v1/me/favorites")) }
            .filter { it.storeId != null }
        favorites = list.associateBy { it.id }
        val quick = list.mapNotNull { favorite -> runCatching { favorite.toQuickOrder() }.getOrNull() }
        return quick.sortedByDescending { it.id == preferredQuickId }
    }

    override suspend fun saveQuickOrder(store: StoreSummary, menu: MenuOption): QuickOrder {
        val saved = session.authorized { http ->
            val body = http.json.encodeToJsonElement(CreateFavoriteRequest(menu.name, store.id, listOf(OrderLineDto(menu.id, 1))))
            http.json.decodeFromJsonElement<FavoriteDto>(http.post("/v1/me/favorites", body))
        }
        favorites = favorites + (saved.id to saved)
        preferredQuickId = saved.id
        return saved.toQuickOrder()
    }

    /** 견적. 품절이면 SOLD_OUT, 영업 중이 아니면 STORE_CLOSED (서버 400 「영업 중인 매장이 아닙니다」, 코드 없음) */
    override suspend fun quote(quickOrderId: String): Quote {
        val favorite = favorite(quickOrderId)
        return try {
            val dto = session.authorized { http ->
                val body = http.json.encodeToJsonElement(OrderRequest(storeId = favorite.storeId!!, favoriteId = favorite.id))
                http.json.decodeFromJsonElement<QuoteDto>(http.post("/v1/orders/quote", body))
            }
            dto.toQuote(quickOrderId, issue = if (dto.available) null else QuoteIssue.SOLD_OUT)
        } catch (e: ApiException) {
            if (!isStoreClosed(e)) throw e
            val quick = favorite.toQuickOrder()
            Quote(quickOrderId, quick.store, quick.lines, quick.total, issue = QuoteIssue.STORE_CLOSED)
        }
    }

    override suspend fun reserve(quickOrderId: String, idempotencyKey: String, expectedTotal: Money?): Order {
        val favorite = favorite(quickOrderId)
        val response = try {
            session.authorized { http ->
                val body = http.json.encodeToJsonElement(
                    ReserveRequest(storeId = favorite.storeId!!, favoriteId = favorite.id, expectedTotal = expectedTotal?.won),
                )
                http.json.decodeFromJsonElement<ReserveResponseDto>(http.post("/v1/orders", body, mapOf("Idempotency-Key" to idempotencyKey)))
            }
        } catch (e: ApiException) {
            throw reserveConflict(e, quickOrderId, expectedTotal) ?: e
        }
        return publish(response.order, track = true)
    }

    override suspend fun reportEta(orderId: String, reading: EtaReading) {
        val here = position()
        val response = session.authorized { http ->
            val body = http.json.encodeToJsonElement(
                TelemetryRequest(
                    etaSec = reading.etaSec,
                    lat = here?.lat,
                    lng = here?.lng,
                    speedKmh = speedKmh()?.toDouble(),
                ),
            )
            http.json.decodeFromJsonElement<OrderEnvelopeDto>(http.post("/v1/orders/$orderId/telemetry", body))
        }
        publish(response.order)
    }

    /** 수락 전이면 바로 취소, 수락 후면 취소 확인 중 (서버가 판단) */
    override suspend fun requestCancel(orderId: String) {
        val order = session.authorized { http ->
            http.json.decodeFromJsonElement<OrderDto>(http.post("/v1/orders/$orderId/cancel", http.json.encodeToJsonElement(CancelRequest())))
        }
        publish(order)
    }

    override suspend fun refresh() {
        // 첫 이용에서 차량번호를 등록하기 전에는 서버 사용자가 없으므로 진행 중 주문도 없다
        if (!session.canLogin) return
        connectRealtime()
        val tracked = trackedOrderId
        val order = session.authorized { http ->
            if (tracked != null) {
                http.json.decodeFromJsonElement<OrderDto>(http.get("/v1/orders/$tracked"))
            } else {
                http.json.decodeFromJsonElement<List<OrderDto>>(http.get("/v1/orders?active=true")).firstOrNull()
            }
        } ?: return
        publish(order, track = tracked == null)
    }

    override fun clearFinished() {
        val order = state.value ?: return
        if (order.stage.isActive) return
        trackedOrderId = null
        lastStage = null
        cancelRejected = false
        state.value = null
    }

    private suspend fun publish(dto: OrderDto, track: Boolean = false): Order = lock.withLock {
        if (track && trackedOrderId != dto.id) {
            trackedOrderId = dto.id
            lastStage = null
            cancelRejected = false
        }
        if (dto.id != trackedOrderId) return@withLock dto.toOrder()
        val stage = stageOf(dto.status)
        cancelRejected = cancelRejectedAfter(lastStage, stage, cancelRejected)
        lastStage = stage
        val order = dto.toOrder(cancelRejected = cancelRejected)
        state.value = order
        order
    }

    /** 409 SOLD_OUT·PRICE_CHANGED → 새 견적으로 다시 확인, ACTIVE_ORDER_EXISTS → 진행 중 주문을 불러온다 */
    private suspend fun reserveConflict(e: ApiException, quickOrderId: String, expectedTotal: Money?): Exception? {
        if (e.status != 409) return null
        val conflict = e.body?.let { runCatching { session.http.json.decodeFromJsonElement<ReserveConflictDto>(it) }.getOrNull() } ?: return null
        val quote = conflict.quote
        return when (conflict.code) {
            "SOLD_OUT" -> quote?.let { QuoteChangedException(it.toQuote(quickOrderId, QuoteIssue.SOLD_OUT)) }
            "PRICE_CHANGED" -> quote?.let { QuoteChangedException(it.toQuote(quickOrderId, QuoteIssue.PRICE_CHANGED, previousTotal = expectedTotal)) }
            "ACTIVE_ORDER_EXISTS" -> {
                runCatching { refresh() }
                null
            }
            else -> null
        }
    }

    private fun QuoteDto.toQuote(quickOrderId: String, issue: QuoteIssue?, previousTotal: Money? = null) = Quote(
        quickOrderId = quickOrderId,
        store = StoreSummary(storeId, storeName, RemoteCatalog.BRAND),
        lines = items.map { it.toLine() },
        total = Money(totalAmount),
        issue = issue,
        previousTotal = previousTotal,
    )

    private fun isStoreClosed(e: ApiException): Boolean = e.status == 400 && e.message.orEmpty().contains("영업 중인 매장이 아닙니다")

    private suspend fun connectRealtime() {
        runCatching { realtime.connect(session.ensureLoggedIn()) }
            .onFailure { Log.w(TAG, "실시간 연결 실패 ${it.message}") }
    }

    private fun favorite(quickOrderId: String) =
        checkNotNull(favorites[quickOrderId]) { "등록 메뉴를 다시 불러와 주세요: $quickOrderId" }

    private suspend fun FavoriteDto.toQuickOrder(): QuickOrder {
        val menu = menuOf(storeId!!)
        val byId = menu.items.associateBy { it.id }
        val lines = items.map { line ->
            val item = checkNotNull(byId[line.menuItemId]) { "메뉴에 없는 항목: ${line.menuItemId}" }
            OrderLine(item.id, item.name, option = null, unitPrice = Money(item.price), qty = line.qty)
        }
        return QuickOrder(id = id, store = StoreSummary(menu.storeId, menu.storeName, RemoteCatalog.BRAND), lines = lines)
    }

    private suspend fun menuOf(storeId: String): MenuDto = menus[storeId] ?: session.authorized { http ->
        http.json.decodeFromJsonElement<MenuDto>(http.get("/v1/stores/$storeId/menu"))
    }.also { menus[storeId] = it }

    private companion object {
        const val TAG = "PickmeRemote"
    }
}
