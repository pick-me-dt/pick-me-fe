package com.pickmedt.car.core.rule

import com.pickmedt.car.core.model.PickupTiming
import java.time.Duration
import java.time.Instant

/**
 * 2차 기획 §5 제조 타이밍 식.
 * 예상 수령 = 입구 도착 예정 + 입구 이후 시간, 시작 추천 = 예상 수령 − 준비 시간.
 * 제조를 시작했으면 추천을 고정하고, 제조 시작 때의 예상 수령보다 늦어진 시간만 계산한다 (§3).
 *
 * 실서버는 서버가 계산한 값(timing)을 그대로 쓰고, 이 식은 앱 안의 가짜 서버만 쓴다.
 * 서버(pick-me-be src/timing/pickup-timing.ts)와 식은 같지만, 서버는 제조 시작 후에도 시작 추천을 다시 계산한다.
 */
fun pickupTiming(
    now: Instant,
    etaSec: Int,
    afterEntrance: Duration,
    prepTime: Duration,
    previous: PickupTiming?,
    pickupAtPrepStart: Instant?,
): PickupTiming {
    val entrance = now.plusSeconds(etaSec.toLong())
    val pickup = entrance.plus(afterEntrance)
    if (pickupAtPrepStart != null && previous != null) {
        val delaySec = maxOf(0L, Duration.between(pickupAtPrepStart, pickup).seconds).toInt()
        return PickupTiming(entrance, pickup, previous.recommendedStartAt, delaySec, now)
    }
    return PickupTiming(entrance, pickup, pickup.minus(prepTime), delaySec = 0, etaReceivedAt = now)
}
