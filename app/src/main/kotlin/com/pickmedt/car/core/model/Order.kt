package com.pickmedt.car.core.model

import java.time.Instant

/** 2차 기획 §3의 주문 단계. 서버 상태 이름과의 변환은 data 계층에서 한다. */
enum class OrderStage {
    /** 매장 확인 중 */
    AWAITING_STORE,
    /** 제조 대기 (직원 수락) */
    WAITING_TO_PREPARE,
    /** 제조 중 */
    PREPARING,
    /** 준비 완료 */
    READY,
    /** 수령 완료 */
    PICKED_UP,
    /** 취소 확인 중 */
    CANCEL_REQUESTED,
    /** 취소 완료 (매장 거절 포함) */
    CANCELLED;

    val isActive: Boolean get() = this != PICKED_UP && this != CANCELLED
}

data class StoreSummary(val id: String, val name: String)

data class OrderLine(
    val menuItemId: String,
    val name: String,
    /** 예: "Regular" */
    val option: String?,
    val unitPrice: Money,
    val qty: Int,
) {
    val price: Money get() = unitPrice * qty
}

/** 제조 타이밍 (2차 기획 §5). 서버가 계산한 값을 담는다. */
data class PickupTiming(
    val entranceEtaAt: Instant,
    val expectedPickupAt: Instant,
    /** 제조 시작 후에는 고정된다 */
    val recommendedStartAt: Instant,
    /** 제조 시작 시점보다 수령 예상이 늦어진 시간(초). 제조 전에는 0 */
    val delaySec: Int,
    val etaReceivedAt: Instant,
)

enum class CancelReason { STORE_REJECTED, CANCEL_APPROVED, OTHER }

data class Order(
    val id: String,
    /** 예: "P-021" */
    val displayNo: String?,
    val stage: OrderStage,
    val store: StoreSummary,
    val lines: List<OrderLine>,
    val plateNumber: String,
    val timing: PickupTiming?,
    val cancelReason: CancelReason?,
    /** 직전 취소 요청을 매장이 거절했는지 (Figma V06-E) */
    val cancelRejected: Boolean = false,
) {
    val total: Money get() = lines.fold(Money(0)) { sum, line -> sum + line.price }
}
