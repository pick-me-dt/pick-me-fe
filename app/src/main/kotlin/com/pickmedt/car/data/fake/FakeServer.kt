package com.pickmedt.car.data.fake

import com.pickmedt.car.core.model.CancelReason
import com.pickmedt.car.core.model.MenuOption
import com.pickmedt.car.core.model.Money
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.core.model.OrderStage.AWAITING_STORE
import com.pickmedt.car.core.model.OrderStage.CANCELLED
import com.pickmedt.car.core.model.OrderStage.CANCEL_REQUESTED
import com.pickmedt.car.core.model.OrderStage.PICKED_UP
import com.pickmedt.car.core.model.OrderStage.PREPARING
import com.pickmedt.car.core.model.OrderStage.READY
import com.pickmedt.car.core.model.OrderStage.WAITING_TO_PREPARE
import com.pickmedt.car.core.model.PickupTiming
import com.pickmedt.car.core.model.QuickOrder
import com.pickmedt.car.core.model.StoreSummary
import com.pickmedt.car.core.model.toOrderLine
import com.pickmedt.car.core.rule.pickupTiming
import com.pickmedt.car.core.time.Clock
import com.pickmedt.car.data.order.Quote
import com.pickmedt.car.data.order.QuoteChangedException
import com.pickmedt.car.data.order.QuoteIssue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Duration
import java.io.IOException
import java.time.Instant
import java.util.Locale

/**
 * 서버 없이 2차 기획 흐름을 재현하는 가짜 서버.
 * 차량 쪽 호출(예약·ETA·취소 요청)과 매장 쪽 조작(수락·제조·준비·수령·취소 판단),
 * 예약 전 확인 조건(품절·가격 변경·접수 종료)을 함께 제공한다.
 * 제조 타이밍은 2차 기획 §5 식을 따른다. 기본값 2분·3분은 같은 절의 시연용 가정이다.
 */
class FakeServer(
    private val clock: Clock,
    private val afterEntrance: Duration = Duration.ofMinutes(2),
    private val prepTime: Duration = Duration.ofMinutes(3),
    firstDisplayNo: Int = 21,
    private val plateNumber: String = SampleData.PLATE_NUMBER,
) {
    private val state = MutableStateFlow<Order?>(null)
    val order: StateFlow<Order?> = state.asStateFlow()

    var quickOrders: List<QuickOrder> = listOf(SampleData.usualIcedAmericano)
        private set

    private var nextQuickNo = 1
    private val soldOutMenuIds = mutableSetOf<String>()
    private val closedStoreIds = mutableSetOf<String>()
    private val priceByMenuId = mutableMapOf<String, Money>()
    private var failNextRefresh = false

    private val orderIdByKey = mutableMapOf<String, String>()
    private var nextDisplayNo = firstDisplayNo
    private var stageBeforeCancel: OrderStage? = null

    /** 제조를 시작한 시점의 수령 예상. 이후 지연 계산의 기준 */
    private var pickupAtPrepStart: Instant? = null

    // ── 차량 쪽 ─────────────────────────────

    fun quote(quickOrderId: String): Quote {
        val quick = quickOrder(quickOrderId)
        val lines = currentLines(quick)
        val total = lines.fold(Money(0)) { sum, line -> sum + line.price }
        val issue = when {
            quick.store.id in closedStoreIds -> QuoteIssue.STORE_CLOSED
            quick.lines.any { it.menuItemId in soldOutMenuIds } -> QuoteIssue.SOLD_OUT
            total != quick.total -> QuoteIssue.PRICE_CHANGED
            else -> null
        }
        return Quote(
            quickOrderId = quick.id,
            store = quick.store,
            lines = lines,
            total = total,
            issue = issue,
            previousTotal = if (issue == QuoteIssue.PRICE_CHANGED) quick.total else null,
        )
    }

    /** 등록 매장·메뉴를 바꾼다. 등록 메뉴는 한 건만 둔다 */
    fun saveQuickOrder(store: StoreSummary, menu: MenuOption): QuickOrder {
        val quick = QuickOrder(id = "fake-quick-${nextQuickNo++}", store = store, lines = listOf(menu.toOrderLine()))
        quickOrders = listOf(quick)
        return quick
    }

    /** 재연결·앱 시작 시 서버 상태 조회. 가짜 서버는 상태를 이미 들고 있어 실패 시연만 한다 */
    fun refresh() {
        if (failNextRefresh) {
            failNextRefresh = false
            throw IOException("시연용 조회 실패")
        }
    }

    /** 2차 서버처럼 품절·가격 변경이면 예약을 막고 새 견적을 돌려준다 ([QuoteChangedException]) */
    fun reserve(quickOrderId: String, idempotencyKey: String, expectedTotal: Money? = null): Order {
        val existingId = orderIdByKey[idempotencyKey]
        state.value?.let { if (it.id == existingId) return it }
        check(state.value?.stage?.isActive != true) { "진행 중인 주문이 있습니다" }
        val current = quote(quickOrderId)
        if (current.issue == QuoteIssue.SOLD_OUT || current.issue == QuoteIssue.STORE_CLOSED) throw QuoteChangedException(current)
        if (expectedTotal != null && expectedTotal != current.total) {
            throw QuoteChangedException(current.copy(issue = QuoteIssue.PRICE_CHANGED, previousTotal = expectedTotal))
        }

        val quick = quickOrder(quickOrderId)
        val lines = currentLines(quick)
        val no = nextDisplayNo++
        val order = Order(
            id = "fake-order-$no",
            displayNo = String.format(Locale.ROOT, "P-%03d", no),
            stage = AWAITING_STORE,
            store = quick.store,
            lines = lines,
            plateNumber = plateNumber,
            timing = null,
            cancelReason = null,
            requestedAt = clock.now(),
        )
        orderIdByKey[idempotencyKey] = order.id
        stageBeforeCancel = null
        pickupAtPrepStart = null
        state.value = order
        return order
    }

    fun reportEta(orderId: String, etaSec: Int) {
        val order = current(orderId)
        state.value = order.copy(timing = timingFor(order, etaSec))
    }

    /** 2차 서버처럼 수락 전이면 바로 취소, 수락 후면 취소 확인 중 */
    fun requestCancel(orderId: String) {
        val order = current(orderId)
        check(order.stage.isActive && order.stage != CANCEL_REQUESTED) { "취소를 요청할 수 없는 상태입니다: ${order.stage}" }
        if (order.stage == AWAITING_STORE) {
            state.value = order.copy(stage = CANCELLED, cancelReason = CancelReason.OTHER, cancelNote = "주문이 취소됐어요.", cancelledAt = clock.now())
            return
        }
        stageBeforeCancel = order.stage
        state.value = order.copy(stage = CANCEL_REQUESTED, cancelRejected = false)
    }

    // ── 매장 쪽 (시연 컨트롤에서 호출) ─────────────

    fun accept() = move(from = AWAITING_STORE, to = WAITING_TO_PREPARE)

    /** 예약 거절. note는 매장이 남기는 사유 (예: "주문량 증가") */
    fun reject(note: String? = null) {
        val order = expect(AWAITING_STORE)
        state.value = order.copy(
            stage = CANCELLED,
            cancelReason = CancelReason.STORE_REJECTED,
            cancelNote = note,
            cancelledAt = clock.now(),
        )
    }

    fun startPreparing() {
        val order = expect(WAITING_TO_PREPARE)
        pickupAtPrepStart = order.timing?.expectedPickupAt
        state.value = order.copy(stage = PREPARING)
    }

    fun markReady() = move(from = PREPARING, to = READY)

    fun pickUp() {
        val order = expect(READY)
        state.value = order.copy(stage = PICKED_UP, pickedUpAt = clock.now())
    }

    fun approveCancel() {
        val order = expect(CANCEL_REQUESTED)
        state.value = order.copy(stage = CANCELLED, cancelReason = CancelReason.CANCEL_APPROVED, cancelledAt = clock.now())
    }

    fun rejectCancel() {
        val order = expect(CANCEL_REQUESTED)
        state.value = order.copy(stage = checkNotNull(stageBeforeCancel), cancelRejected = true)
        stageBeforeCancel = null
    }

    /** 시연용: 예약 요청 시각을 앞당겨 매장 응답 지연(V03-T)을 바로 보여 준다 */
    fun backdateRequest(by: Duration) {
        val order = state.value ?: return
        state.value = order.copy(requestedAt = order.requestedAt?.minus(by))
    }

    /** 시연용: ETA 수신 시각을 앞당겨 연결·ETA 오류(V04-E)를 바로 보여 준다 */
    fun backdateEtaReceipt(by: Duration) {
        val order = state.value ?: return
        val timing = order.timing ?: return
        state.value = order.copy(timing = timing.copy(etaReceivedAt = timing.etaReceivedAt.minus(by)))
    }

    // ── 예약 전 확인 조건 (시연 컨트롤에서 호출) ─────

    /** V02-SO. 기본은 지금 등록 메뉴 */
    fun markSoldOut(menuItemId: String = firstQuickLine().menuItemId) {
        soldOutMenuIds += menuItemId
    }

    /** V02-PC. 기본은 지금 등록 메뉴 */
    fun changePrice(unitPrice: Money, menuItemId: String = firstQuickLine().menuItemId) {
        priceByMenuId[menuItemId] = unitPrice
    }

    /** V02-CL. 기본은 지금 등록 매장 */
    fun closeStore(storeId: String = quickOrders.first().store.id) {
        closedStoreIds += storeId
    }

    fun clearQuoteIssues() {
        soldOutMenuIds.clear()
        closedStoreIds.clear()
        priceByMenuId.clear()
    }

    /** V01-E. 등록 메뉴를 모두 지운다 */
    fun clearQuickOrders() {
        quickOrders = emptyList()
    }

    /** V00-E. 다음 조회 한 번을 실패시킨다 */
    fun failNextRefresh() {
        failNextRefresh = true
    }

    /** 현재 주문을 지운다. 끝난 주문을 닫을 때도 쓴다 */
    fun reset() {
        state.value = null
        stageBeforeCancel = null
        pickupAtPrepStart = null
    }

    /** 시연 처음 상태로. 주문과 함께 등록 메뉴·확인 조건도 되돌린다 */
    fun resetScenario() {
        reset()
        quickOrders = listOf(SampleData.usualIcedAmericano)
        clearQuoteIssues()
        failNextRefresh = false
    }

    // ── 내부 ────────────────────────────────

    private fun timingFor(order: Order, etaSec: Int): PickupTiming =
        pickupTiming(clock.now(), etaSec, afterEntrance, prepTime, order.timing, pickupAtPrepStart)

    private fun move(from: OrderStage, to: OrderStage) {
        val order = expect(from)
        state.value = order.copy(stage = to)
    }

    private fun expect(stage: OrderStage): Order {
        val order = checkNotNull(state.value) { "주문이 없습니다" }
        check(order.stage == stage) { "${order.stage} 상태에서는 할 수 없습니다 (필요: $stage)" }
        return order
    }

    private fun current(orderId: String): Order {
        val order = checkNotNull(state.value) { "주문이 없습니다" }
        check(order.id == orderId) { "다른 주문입니다: $orderId" }
        return order
    }

    /** 가격 변경을 반영한 주문 줄 */
    private fun currentLines(quick: QuickOrder) =
        quick.lines.map { line -> priceByMenuId[line.menuItemId]?.let { line.copy(unitPrice = it) } ?: line }

    private fun firstQuickLine() = checkNotNull(quickOrders.firstOrNull()?.lines?.firstOrNull()) { "등록 메뉴가 없습니다" }

    private fun quickOrder(id: String): QuickOrder =
        quickOrders.firstOrNull { it.id == id } ?: throw IllegalArgumentException("등록 메뉴가 없습니다: $id")
}
