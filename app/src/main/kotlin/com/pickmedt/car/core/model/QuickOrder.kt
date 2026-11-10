package com.pickmedt.car.core.model

/** 홈에 보여 주는 등록 매장·메뉴 (2차 기획 §3 자주 주문 메뉴) */
data class QuickOrder(
    val id: String,
    val store: StoreSummary,
    val lines: List<OrderLine>,
) {
    val total: Money get() = lines.fold(Money(0)) { sum, line -> sum + line.price }
}
