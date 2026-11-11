package com.pickmedt.car.core.rule

import java.time.Duration
import java.time.Instant

/** 매장 응답 지연 기준 (Figma V03-T) */
object StoreResponsePolicy {
    /**
     * 예약 요청 후 이 시간이 지나도 매장이 수락·거절하지 않으면 응답 지연으로 본다.
     * 2차 기획에 기준 시간이 없어 백엔드 기본값(2분, pick-me-be pickup-timing.ts ACCEPT_OVERDUE_SEC)에 맞췄다. 기획 확인 필요.
     * 실서버는 이 값 대신 주문의 acceptOverdue를 쓴다.
     */
    val SlowAfter: Duration = Duration.ofSeconds(120)
}

fun isStoreResponseSlow(requestedAt: Instant?, now: Instant): Boolean =
    requestedAt != null && Duration.between(requestedAt, now) >= StoreResponsePolicy.SlowAfter
