package com.pickmedt.car.data.fake

import com.pickmedt.car.core.model.Money
import com.pickmedt.car.core.model.OrderLine
import com.pickmedt.car.core.model.QuickOrder
import com.pickmedt.car.core.model.StoreSummary

/** 2차 기획 §9 시연 주문: 양재 DT점·아이스 아메리카노. ID는 서버 시드와 같다 (pick-me-be src/seed/seed.ts:8,18) */
object SampleData {
    const val PLATE_NUMBER = "12가3456"

    val yangjae = StoreSummary(id = "00000000-0000-4000-8000-000000000101", name = "양재 DT점", brand = "픽미 커피")

    val usualIcedAmericano = QuickOrder(
        id = "fake-quick-usual",
        store = yangjae,
        lines = listOf(
            OrderLine(
                menuItemId = "00000000-0000-4000-8000-000000000206",
                name = "아이스 아메리카노",
                option = "Regular",
                unitPrice = Money(4_500),
                qty = 1,
            ),
        ),
    )
}
