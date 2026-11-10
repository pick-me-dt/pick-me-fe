package com.pickmedt.car.testing

import com.pickmedt.car.core.model.Money
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.model.OrderLine
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.core.model.PickupTiming
import com.pickmedt.car.core.model.StoreSummary

val testStore = StoreSummary("store-1", "양재 DT점", "픽미 커피")

val testLine = OrderLine("menu-1", "아이스 아메리카노", "Regular", Money(4_500), 1)

fun testOrder(stage: OrderStage, id: String = "order-1") = Order(
    id = id,
    displayNo = "P-021",
    stage = stage,
    store = testStore,
    lines = listOf(testLine),
    plateNumber = "12가3456",
    timing = null,
    cancelReason = null,
)

/** 2차 기획 §5 시연값(입구 이후 2분, 준비 3분)으로 만든 제조 타이밍. 시각은 kst("08:08") 형식 */
fun testTiming(entrance: String, receivedAt: String = "08:02", delaySec: Int = 0, recommended: String? = null): PickupTiming {
    val entranceAt = kst(entrance)
    val pickup = entranceAt.plusSeconds(120)
    return PickupTiming(
        entranceEtaAt = entranceAt,
        expectedPickupAt = pickup,
        recommendedStartAt = recommended?.let(::kst) ?: pickup.minusSeconds(180),
        delaySec = delaySec,
        etaReceivedAt = kst(receivedAt),
    )
}
