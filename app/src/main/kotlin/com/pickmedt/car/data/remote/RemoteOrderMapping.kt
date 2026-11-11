package com.pickmedt.car.data.remote

import com.pickmedt.car.core.model.CancelReason
import com.pickmedt.car.core.model.Money
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.model.OrderLine
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.core.model.PickupTiming
import com.pickmedt.car.core.model.StoreSummary
import java.time.Instant

/** 2차 서버 상태 → 주문 단계 (pick-me-be prisma OrderStatus, src/orders/order-state.ts) */
fun stageOf(status: String): OrderStage = when (status) {
    "PENDING_ACCEPT" -> OrderStage.AWAITING_STORE
    "ACCEPTED" -> OrderStage.WAITING_TO_PREPARE
    "PREPARING" -> OrderStage.PREPARING
    "READY" -> OrderStage.READY
    "PICKED_UP" -> OrderStage.PICKED_UP
    "CANCEL_REQUESTED" -> OrderStage.CANCEL_REQUESTED
    "REJECTED", "CANCELLED" -> OrderStage.CANCELLED
    else -> OrderStage.AWAITING_STORE
}

/**
 * 거절 사유 코드 → V03-R 문구 「매장이 ○○로 예약을 받지 못했어요」의 ○○.
 * 서버 문구(pick-me-be orders.service.ts REJECT_TEXT)를 따르되, BUSY는 Figma V03-R 예시 「주문량 증가」로 쓴다.
 */
fun rejectReasonText(reason: String?): String? = when (reason) {
    "SOLD_OUT" -> "품절"
    "BUSY" -> "주문량 증가"
    "CLOSED" -> "영업 종료"
    "OTHER" -> "매장 사정"
    else -> null
}

/** 매장 승인이 아닌 취소의 안내 문구. 서버 TTS 문구(orders.service.ts CANCEL_TTS)와 같다 */
fun cancelReasonText(reason: String?): String = when (reason) {
    "NO_SHOW" -> "매장에서 미수령으로 주문을 종료했어요."
    "STORE" -> "매장 사정으로 주문이 취소됐어요."
    "PAYMENT_DECLINED" -> "결제 승인이 되지 않아 주문을 취소했어요."
    "STORE_UNAVAILABLE" -> "매장에 주문을 전달하지 못해 취소했어요."
    else -> "주문이 취소됐어요."
}

/**
 * 서버 주문 → 앱 주문.
 * [cancelRejected]는 서버 응답에 없다(거절하면 cancelRequest를 지운다). 저장소가 「취소 확인 중 → 진행」 변화를 보고 넘긴다.
 */
fun OrderDto.toOrder(cancelRejected: Boolean = false): Order {
    val rejected = status == "REJECTED"
    val cancelled = status == "CANCELLED"
    val reason = when {
        rejected -> CancelReason.STORE_REJECTED
        // 운전자의 취소 요청을 매장이 승인하면 cancelRequest가 남는다 (orders.service.ts approveCancel)
        cancelled && cancelReason == "USER" && cancelRequest != null -> CancelReason.CANCEL_APPROVED
        cancelled -> CancelReason.OTHER
        else -> null
    }
    return Order(
        id = id,
        displayNo = orderNo,
        stage = stageOf(status),
        store = StoreSummary(store.id, store.name, RemoteCatalog.BRAND),
        lines = items.map { it.toLine() },
        plateNumber = plateNumber.orEmpty(),
        timing = timing?.toPickupTiming(),
        cancelReason = reason,
        cancelRejected = cancelRejected,
        pickedUpAt = pickedUpAt?.let(::instantOrNull),
        requestedAt = createdAt?.let(::instantOrNull),
        cancelledAt = (cancelledAt ?: rejectedAt)?.let(::instantOrNull),
        cancelNote = when {
            rejected -> rejectReasonText(rejectReason)
            reason == CancelReason.OTHER -> cancelReasonText(cancelReason)
            else -> null
        },
        acceptOverdue = acceptOverdue,
    )
}

/**
 * 매장이 취소 요청을 거절했는지. 서버는 거절하면 cancelRequest를 지우고 이전 상태로 되돌리므로(orders.service.ts rejectCancel),
 * 「취소 확인 중 → 진행 상태」 변화로 판단한다. 한 번 거절되면 새 취소 요청 전까지 유지한다.
 */
fun cancelRejectedAfter(previous: OrderStage?, current: OrderStage, rejectedBefore: Boolean): Boolean = when {
    current == OrderStage.CANCEL_REQUESTED -> false
    previous == OrderStage.CANCEL_REQUESTED && current.isActive -> true
    else -> rejectedBefore
}

/** ETA를 아직 받지 못했으면 시각이 없어 null */
fun TimingDto.toPickupTiming(): PickupTiming? {
    val entrance = entranceArrivalAt?.let(::instantOrNull) ?: return null
    val pickup = expectedPickupAt?.let(::instantOrNull) ?: return null
    val recommended = recommendedStartAt?.let(::instantOrNull) ?: return null
    val received = etaUpdatedAt?.let(::instantOrNull) ?: entrance
    return PickupTiming(entrance, pickup, recommended, delaySec, received)
}

fun OrderItemDto.toLine() = OrderLine(menuItemId = menuItemId, name = name, option = null, unitPrice = Money(unitPrice), qty = qty)

private fun instantOrNull(text: String): Instant? = runCatching { Instant.parse(text) }.getOrNull()
