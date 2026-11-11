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

/** 예약 순간에 품절·가격 변경으로 막혔다. 새 견적(issue 포함)으로 다시 확인받는다 (2차 기획 §7, 서버 409) */
class QuoteChangedException(val quote: Quote) : IllegalStateException("예약 조건이 바뀌었습니다: ${quote.issue}")
