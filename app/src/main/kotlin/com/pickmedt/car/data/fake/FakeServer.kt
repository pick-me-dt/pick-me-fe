package com.pickmedt.car.data.fake

import com.pickmedt.car.core.model.CancelReason
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
import com.pickmedt.car.core.time.Clock
import com.pickmedt.car.data.order.Quote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Duration
import java.time.Instant
import java.util.Locale

/**
 * 서버 없이 2차 기획 흐름을 재현하는 가짜 서버.
 * 차량 쪽 호출(예약·ETA·취소 요청)과 매장 쪽 조작(수락·제조·준비·수령·취소 판단)을 함께 제공한다.
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

    val quickOrders: List<QuickOrder> = listOf(SampleData.usualIcedAmericano)

    private val orderIdByKey = mutableMapOf<String, String>()
    private var nextDisplayNo = firstDisplayNo
    private var stageBeforeCancel: OrderStage? = null

    /** 제조를 시작한 시점의 수령 예상. 이후 지연 계산의 기준 */
    private var pickupAtPrepStart: Instant? = null

    // ── 차량 쪽 ─────────────────────────────

    fun quote(quickOrderId: String): Quote {
        val quick = quickOrder(quickOrderId)
        return Quote(quick.id, quick.store, quick.lines, quick.total, issue = null)
    }

    fun reserve(quickOrderId: String, idempotencyKey: String): Order {
        val existingId = orderIdByKey[idempotencyKey]
        state.value?.let { if (it.id == existingId) return it }
        check(state.value?.stage?.isActive != true) { "진행 중인 주문이 있습니다" }

        val quick = quickOrder(quickOrderId)
        val no = nextDisplayNo++
        val order = Order(
            id = "fake-order-$no",
            displayNo = String.format(Locale.ROOT, "P-%03d", no),
            stage = AWAITING_STORE,
            store = quick.store,
            lines = quick.lines,
            plateNumber = plateNumber,
            timing = null,
            cancelReason = null,
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

    fun requestCancel(orderId: String) {
        val order = current(orderId)
        check(order.stage.isActive && order.stage != CANCEL_REQUESTED) { "취소를 요청할 수 없는 상태입니다: ${order.stage}" }
        stageBeforeCancel = order.stage
        state.value = order.copy(stage = CANCEL_REQUESTED, cancelRejected = false)
    }

    // ── 매장 쪽 (시연 컨트롤에서 호출) ─────────────

    fun accept() = move(from = AWAITING_STORE, to = WAITING_TO_PREPARE)

    fun reject() {
        val order = expect(AWAITING_STORE)
        state.value = order.copy(stage = CANCELLED, cancelReason = CancelReason.STORE_REJECTED)
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
        state.value = order.copy(stage = CANCELLED, cancelReason = CancelReason.CANCEL_APPROVED)
    }

    fun rejectCancel() {
        val order = expect(CANCEL_REQUESTED)
        state.value = order.copy(stage = checkNotNull(stageBeforeCancel), cancelRejected = true)
        stageBeforeCancel = null
    }

    /** 시연 처음 상태로 */
    fun reset() {
        state.value = null
        stageBeforeCancel = null
        pickupAtPrepStart = null
    }

    // ── 내부 ────────────────────────────────

    private fun timingFor(order: Order, etaSec: Int): PickupTiming {
        val now = clock.now()
        val entrance = now.plusSeconds(etaSec.toLong())
        val pickup = entrance.plus(afterEntrance)
        val startedPickup = pickupAtPrepStart
        val previous = order.timing
        // 제조를 시작했으면 추천을 고정하고 늦어진 시간만 계산한다 (2차 기획 §3, §5)
        if (startedPickup != null && previous != null) {
            val delaySec = maxOf(0L, Duration.between(startedPickup, pickup).seconds).toInt()
            return PickupTiming(entrance, pickup, previous.recommendedStartAt, delaySec, now)
        }
        return PickupTiming(entrance, pickup, pickup.minus(prepTime), delaySec = 0, etaReceivedAt = now)
    }

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

    private fun quickOrder(id: String): QuickOrder =
        quickOrders.firstOrNull { it.id == id } ?: throw IllegalArgumentException("등록 메뉴가 없습니다: $id")
}
