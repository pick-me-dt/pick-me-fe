package com.pickmedt.car.core.model

import java.util.Locale

/** 원 단위 금액 */
@JvmInline
value class Money(val won: Int) {
    operator fun plus(other: Money) = Money(won + other.won)
    operator fun times(qty: Int) = Money(won * qty)

    /** 예: "4,500원" */
    fun format(): String = String.format(Locale.KOREA, "%,d원", won)
}
