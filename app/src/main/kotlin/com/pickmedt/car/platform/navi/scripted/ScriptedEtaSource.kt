package com.pickmedt.car.platform.navi.scripted

import com.pickmedt.car.core.model.EtaOrigin
import com.pickmedt.car.core.model.EtaReading
import com.pickmedt.car.core.time.Clock
import com.pickmedt.car.platform.navi.EtaSource
import java.time.Duration
import java.time.Instant

/**
 * 시연용 모의 ETA (2차 기획 §5 '불가 시 모의 ETA').
 * 설정한 순간부터 실제 시간만큼 줄어든다.
 */
class ScriptedEtaSource(private val clock: Clock) : EtaSource {
    private var baseSec: Int? = null
    private var baseAt: Instant = Instant.EPOCH

    fun set(etaSec: Int) {
        baseSec = maxOf(0, etaSec)
        baseAt = clock.now()
    }

    /** 정체 등으로 남은 시간을 늘리거나 줄인다 */
    fun add(deltaSec: Int) = set(remainingSec() + deltaSec)

    fun clear() {
        baseSec = null
    }

    override suspend fun read(): EtaReading? {
        if (baseSec == null) return null
        return EtaReading(remainingSec(), clock.now(), EtaOrigin.SCRIPTED)
    }

    private fun remainingSec(): Int {
        val sec = baseSec ?: return 0
        val elapsed = Duration.between(baseAt, clock.now()).seconds.toInt()
        return maxOf(0, sec - elapsed)
    }
}
