package com.pickmedt.car.testing

import com.pickmedt.car.core.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class MutableClock(var current: Instant) : Clock {
    override fun now(): Instant = current

    fun advance(duration: Duration) {
        current = current.plus(duration)
    }
}

/** 시연 기준일 한국 시각. 예: kst("08:02") */
fun kst(time: String): Instant =
    LocalDate.of(2026, 11, 14).atTime(LocalTime.parse(time)).atZone(ZoneId.of("Asia/Seoul")).toInstant()
