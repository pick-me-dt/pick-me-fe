package com.pickmedt.car.testing

import com.pickmedt.car.core.model.Money
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.model.OrderLine
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.core.model.StoreSummary

fun testOrder(stage: OrderStage, id: String = "order-1") = Order(
    id = id,
    displayNo = "P-021",
    stage = stage,
    store = StoreSummary("store-1", "양재 DT점"),
    lines = listOf(OrderLine("menu-1", "아이스 아메리카노", "Regular", Money(4_500), 1)),
    plateNumber = "12가3456",
    timing = null,
    cancelReason = null,
)
