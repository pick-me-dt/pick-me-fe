package com.pickmedt.car.core.rule

import com.pickmedt.car.testing.kst
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EtaRulesTest {
    private val now = kst("08:05")

    @Test fun `수신 기록이 없으면 NONE`() =
        assertEquals(EtaFreshness.NONE, etaFreshness(null, now))

    @Test fun `90초까지는 최신`() =
        assertEquals(EtaFreshness.FRESH, etaFreshness(now.minusSeconds(90), now))

    @Test fun `90초를 넘으면 오래됨`() =
        assertEquals(EtaFreshness.STALE, etaFreshness(now.minusSeconds(91), now))

    @Test fun `추천이 59초 바뀌면 강조하지 않는다`() =
        assertFalse(isRecommendationShiftEmphasized(kst("08:07"), kst("08:07").plusSeconds(59)))

    @Test fun `추천이 60초 이상 늦어지면 강조`() =
        assertTrue(isRecommendationShiftEmphasized(kst("08:07"), kst("08:08")))

    @Test fun `추천이 60초 이상 당겨져도 강조`() =
        assertTrue(isRecommendationShiftEmphasized(kst("08:07"), kst("08:06")))

    @Test fun `이전 추천이 없으면 강조하지 않는다`() =
        assertFalse(isRecommendationShiftEmphasized(null, kst("08:07")))
}
