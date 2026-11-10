package com.pickmedt.car.data.remote

import com.pickmedt.car.core.model.CancelReason
import com.pickmedt.car.core.model.Money
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.model.OrderLine
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.core.model.PickupTiming
import com.pickmedt.car.core.model.StoreSummary
import java.time.Instant

/**
 * 1차 기획 기준 서버 상태 → 2차 기획 주문 단계.
 * 서버에는 직원 수락이 없어 「매장에 전송됨(SENT)」을 제조 대기로 본다.
 * 서버가 2차 기획을 반영하면 이 표를 서버 상태 이름에 맞춰 바꾼다.
 */
fun stageOf(status: String): OrderStage = when (status) {
    "NEEDS_CONFIRMATION", "RESERVED" -> OrderStage.AWAITING_STORE
    "SENT" -> OrderStage.WAITING_TO_PREPARE
    "PREPARING" -> OrderStage.PREPARING
    "READY" -> OrderStage.READY
    "PICKED_UP" -> OrderStage.PICKED_UP
    "CANCELLED" -> OrderStage.CANCELLED
    else -> OrderStage.AWAITING_STORE
}

/** 서버 취소 사유: USER·EXPIRED·NO_SHOW·STORE_REJECTED·PAYMENT_DECLINED·STORE (pick-me-be src/orders/orders.service.ts:21) */
fun cancelReasonOf(reason: String?): CancelReason? = when (reason) {
    null -> null
    "STORE_REJECTED", "STORE" -> CancelReason.STORE_REJECTED
    "USER" -> CancelReason.CANCEL_APPROVED
    else -> CancelReason.OTHER
}

fun OrderDto.toOrder(timing: PickupTiming?): Order = Order(
    id = id,
    displayNo = externalOrderId,
    stage = stageOf(status),
    store = StoreSummary(store.id, store.name, RemoteCatalog.BRAND),
    lines = items.map { it.toLine() },
    plateNumber = plateNumber.orEmpty(),
    timing = timing,
    cancelReason = cancelReasonOf(cancelReason),
    pickedUpAt = pickedUpAt?.let(::instantOrNull),
    requestedAt = createdAt?.let(::instantOrNull),
    cancelledAt = cancelledAt?.let(::instantOrNull),
)

fun OrderItemDto.toLine() = OrderLine(menuItemId = menuItemId, name = name, option = null, unitPrice = Money(unitPrice), qty = qty)

private fun instantOrNull(text: String): Instant? = runCatching { Instant.parse(text) }.getOrNull()
