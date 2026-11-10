package com.pickmedt.car.data.remote

import android.util.Log
import com.pickmedt.car.core.model.EtaReading
import com.pickmedt.car.core.model.GeoPoint
import com.pickmedt.car.core.model.MenuOption
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.core.model.PickupTiming
import com.pickmedt.car.core.model.QuickOrder
import com.pickmedt.car.core.model.StoreSummary
import com.pickmedt.car.core.rule.pickupTiming
import com.pickmedt.car.core.time.Clock
import com.pickmedt.car.data.order.OrderRepository
import com.pickmedt.car.data.order.Quote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.time.Duration

/**
 * 실서버 주문 저장소 (pick-me-be d43e2d2, 1차 기획 기준 API).
 *
 * 2차 기획과 다른 점은 앱에서 메운다. 서버가 2차 기획을 반영하면 지운다.
 * - 제조 타이밍: 서버 응답에 예상 수령·시작 추천이 없어, 앱이 보낸 ETA로 2차 기획 식을 계산한다
 *   (입구 이후 [afterEntrance], 준비 시간은 서버의 estPrepSec)
 * - 텔레메트리: 서버가 위도·경도·속도를 필수로 받아 [position]·[speedKmh]를 함께 보낸다. 위치를 모르면 보내지 않는다
 *
 * 등록 매장·메뉴는 서버의 자주 주문 메뉴(/v1/me/favorites) 중 매장이 정해진 것이다.
 */
class RemoteOrderRepository(
    private val session: RemoteSession,
    private val clock: Clock,
    private val scope: CoroutineScope,
    private val position: suspend () -> GeoPoint?,
    private val speedKmh: () -> Float?,
    private val afterEntrance: Duration = Duration.ofMinutes(2),
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
    private var timing: PickupTiming? = null
    private var prepSec: Int = 0
    private var pickupAtPrepStart: java.time.Instant? = null

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

    override suspend fun quote(quickOrderId: String): Quote {
        val favorite = favorite(quickOrderId)
        val quote = session.authorized { http ->
            val body = http.json.encodeToJsonElement(OrderRequest(storeId = favorite.storeId!!, favoriteId = favorite.id))
            http.json.decodeFromJsonElement<QuoteDto>(http.post("/v1/orders/quote", body))
        }
        val store = StoreSummary(quote.storeId, quote.storeName, RemoteCatalog.BRAND)
        return Quote(quickOrderId, store, quote.items.map { it.toLine() }, com.pickmedt.car.core.model.Money(quote.totalAmount), issue = null)
    }

    override suspend fun reserve(quickOrderId: String, idempotencyKey: String): Order {
        val favorite = favorite(quickOrderId)
        val response = session.authorized { http ->
            val body = http.json.encodeToJsonElement(ReserveRequest(source = "SCREEN", storeId = favorite.storeId!!, favoriteId = favorite.id))
            http.json.decodeFromJsonElement<ReserveResponseDto>(http.post("/v1/orders", body, mapOf("Idempotency-Key" to idempotencyKey)))
        }
        var order = response.order
        // 화면 주문은 확인 대기가 생기지 않지만, 생기면 바로 확인한다 (2차 기획: 주차 중 화면에서 예약)
        if (order.status == "NEEDS_CONFIRMATION") {
            order = session.authorized { http ->
                http.json.decodeFromJsonElement<ReserveResponseDto>(http.post("/v1/orders/${order.id}/confirm")).order
            }
        }
        lock.withLock { startTracking(order) }
        return publish(order)
    }

    override suspend fun reportEta(orderId: String, reading: EtaReading) {
        val here = position() ?: run {
            Log.w(TAG, "차량 위치를 몰라 텔레메트리를 보내지 않음")
            return
        }
        val response = session.authorized { http ->
            val body = http.json.encodeToJsonElement(
                TelemetryRequest(lat = here.lat, lng = here.lng, speedKmh = (speedKmh() ?: 0f).toDouble(), etaSec = reading.etaSec),
            )
            http.post("/v1/orders/$orderId/telemetry", body)
        }
        lock.withLock {
            if (orderId == trackedOrderId) {
                timing = pickupTiming(clock.now(), reading.etaSec, afterEntrance, Duration.ofSeconds(prepSec.toLong()), timing, pickupAtPrepStart)
            }
        }
        val order = (response as? JsonObject)?.get("order")?.let { session.http.json.decodeFromJsonElement<OrderDto>(it) }
        if (order != null) publish(order) else refresh()
    }

    override suspend fun requestCancel(orderId: String) {
        val response = session.authorized { http ->
            http.post("/v1/orders/$orderId/cancel", http.json.encodeToJsonElement(CancelRequest()))
        }
        val order = (response as? JsonObject)?.get("order")?.let { session.http.json.decodeFromJsonElement<OrderDto>(it) }
            ?: runCatching { session.http.json.decodeFromJsonElement<OrderDto>(response) }.getOrNull()
        if (order != null) publish(order) else refresh()
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
        if (tracked == null) lock.withLock { startTracking(order) }
        publish(order)
    }

    override fun clearFinished() {
        val order = state.value ?: return
        if (order.stage.isActive) return
        trackedOrderId = null
        timing = null
        pickupAtPrepStart = null
        state.value = null
    }

    private fun startTracking(order: OrderDto) {
        if (trackedOrderId == order.id) return
        trackedOrderId = order.id
        prepSec = order.estPrepSec ?: 0
        timing = null
        pickupAtPrepStart = null
    }

    private suspend fun publish(dto: OrderDto): Order = lock.withLock {
        if (dto.id != trackedOrderId) return@withLock dto.toOrder(timing = null)
        val stage = stageOf(dto.status)
        // 제조를 시작한 순간의 예상 수령을 기준으로 이후 지연을 계산한다 (2차 기획 §3)
        if (stage == OrderStage.PREPARING && pickupAtPrepStart == null) pickupAtPrepStart = timing?.expectedPickupAt
        val order = dto.toOrder(timing)
        state.value = order
        order
    }

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
            com.pickmedt.car.core.model.OrderLine(item.id, item.name, option = null, unitPrice = com.pickmedt.car.core.model.Money(item.price), qty = line.qty)
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
