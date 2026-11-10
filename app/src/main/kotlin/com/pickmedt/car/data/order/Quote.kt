package com.pickmedt.car.data.order

import com.pickmedt.car.core.model.Money
import com.pickmedt.car.core.model.OrderLine
import com.pickmedt.car.core.model.StoreSummary

data class Quote(
    val quickOrderId: String,
    val store: StoreSummary,
    val lines: List<OrderLine>,
    val total: Money,
    /** 예약 전에 다시 확인해야 하는 조건. 없으면 null */
    val issue: QuoteIssue?,
    /** 가격이 바뀐 경우(PRICE_CHANGED) 등록할 때의 금액 (Figma V02-PC 「기존 4,500원」) */
    val previousTotal: Money? = null,
)

/** Figma V02-SO·V02-PC·V02-CL */
enum class QuoteIssue { SOLD_OUT, PRICE_CHANGED, STORE_CLOSED }
