package com.pickmedt.car.core.time

import java.time.Instant

/** 현재 시각. 테스트·시연에서 시간을 고정하거나 앞당길 수 있게 주입한다. */
fun interface Clock {
    fun now(): Instant

    companion object {
        val System: Clock = Clock { Instant.now() }
    }
}
