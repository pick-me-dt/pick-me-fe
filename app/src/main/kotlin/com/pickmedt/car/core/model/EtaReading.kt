package com.pickmedt.car.core.model

import java.time.Instant

/** 매장 입구까지 남은 시간 측정값 */
data class EtaReading(
    val etaSec: Int,
    val measuredAt: Instant,
    val origin: EtaOrigin,
)

enum class EtaOrigin {
    /** NaviHelper 실측 */
    NAVI,
    /** 시연용 모의 값 */
    SCRIPTED,
}
