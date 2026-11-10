package com.pickmedt.car.data.fake

import com.pickmedt.car.core.model.MenuOption
import com.pickmedt.car.core.model.Money
import com.pickmedt.car.core.model.QuickOrder
import com.pickmedt.car.core.model.StoreOnRoute
import com.pickmedt.car.core.model.StoreSummary
import com.pickmedt.car.core.model.toOrderLine

/**
 * 2차 기획 §9 시연 주문: 양재 DT점·아이스 아메리카노. ID는 서버 시드와 같다 (pick-me-be src/seed/seed.ts:8,18).
 * 서초 DT점·따뜻한 아메리카노는 Figma V01-M·V01-N 예시 문구로, 서버 시드에 없어 가짜 ID를 쓴다.
 */
object SampleData {
    const val PLATE_NUMBER = "12가3456"

    val yangjae = StoreSummary(id = "00000000-0000-4000-8000-000000000101", name = "양재 DT점", brand = "픽미 커피")
    val seocho = StoreSummary(id = "fake-store-seocho", name = "서초 DT점", brand = "픽미 커피")

    val yangjaeOnRoute = StoreOnRoute(yangjae, etaMinutes = 6)
    val seochoOnRoute = StoreOnRoute(seocho, etaMinutes = 9)

    val icedAmericano = MenuOption(
        id = "00000000-0000-4000-8000-000000000206",
        name = "아이스 아메리카노",
        option = "Regular",
        price = Money(4_500),
    )
    val hotAmericano = MenuOption(
        id = "fake-menu-hot-americano",
        name = "따뜻한 아메리카노",
        option = "Regular",
        price = Money(4_500),
    )

    val usualIcedAmericano = QuickOrder(
        id = "fake-quick-usual",
        store = yangjae,
        lines = listOf(icedAmericano.toOrderLine()),
    )
}
