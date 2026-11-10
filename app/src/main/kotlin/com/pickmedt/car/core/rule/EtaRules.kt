package com.pickmedt.car.core.rule

import java.time.Duration
import java.time.Instant

/** 2차 기획 §5 ETA 정책 */
object EtaPolicy {
    /** 차량 앱이 ETA를 보내는 주기 */
    val ReportInterval: Duration = Duration.ofSeconds(30)

    /** 이 시간을 넘기면 오래됨 */
    val StaleAfter: Duration = Duration.ofSeconds(90)

    /** 추천 시각이 이만큼 이상 바뀌면 강조 */
    val EmphasizeShift: Duration = Duration.ofSeconds(60)
}

enum class EtaFreshness { NONE, FRESH, STALE }

fun etaFreshness(receivedAt: Instant?, now: Instant): EtaFreshness = when {
    receivedAt == null -> EtaFreshness.NONE
    Duration.between(receivedAt, now) > EtaPolicy.StaleAfter -> EtaFreshness.STALE
    else -> EtaFreshness.FRESH
}

fun isRecommendationShiftEmphasized(previous: Instant?, next: Instant?): Boolean {
    if (previous == null || next == null) return false
    return Duration.between(previous, next).abs() >= EtaPolicy.EmphasizeShift
}
