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
)

/** Figma V02-SO·V02-PC·V02-CL */
enum class QuoteIssue { SOLD_OUT, PRICE_CHANGED, STORE_CLOSED }
