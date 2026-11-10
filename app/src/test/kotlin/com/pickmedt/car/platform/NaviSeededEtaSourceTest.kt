package com.pickmedt.car.platform

import com.pickmedt.car.core.model.EtaOrigin
import com.pickmedt.car.core.model.EtaReading
import com.pickmedt.car.platform.navi.EtaSource
import com.pickmedt.car.platform.navi.scripted.NaviSeededEtaSource
import com.pickmedt.car.platform.navi.scripted.ScriptedEtaSource
import com.pickmedt.car.testing.MutableClock
import com.pickmedt.car.testing.kst
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration

class NaviSeededEtaSourceTest {
    private val clock = MutableClock(kst("08:02"))
    private val scripted = ScriptedEtaSource(clock)

    /** 에뮬레이터처럼 값이 줄지 않는 내비 (게이트 G3: 1680초 고정) */
    private class FixedNavi(var remainSec: Int?, private val clock: MutableClock) : EtaSource {
        override suspend fun read() = remainSec?.let { EtaReading(it, clock.now(), EtaOrigin.NAVI) }
    }

    private val navi = FixedNavi(1_680, clock)
    private val source = NaviSeededEtaSource(navi, scripted)

    @Test fun `첫 값은 실측, 이후는 그 값에서 줄어드는 모의 값`() = runTest {
        val first = source.read()!!
        assertEquals(1_680, first.etaSec)
        assertEquals(EtaOrigin.NAVI, first.origin)
        clock.advance(Duration.ofSeconds(30))
        val next = source.read()!!
        assertEquals(1_650, next.etaSec)
        assertEquals(EtaOrigin.SCRIPTED, next.origin)
    }

    @Test fun `시연 컨트롤로 정한 모의 ETA가 우선한다`() = runTest {
        scripted.set(360)
        assertEquals(360, source.read()!!.etaSec)
    }

    @Test fun `새 주문은 다시 실측 값으로 시작한다`() = runTest {
        source.read()
        clock.advance(Duration.ofMinutes(5))
        source.startNewTrip()
        val reading = source.read()!!
        assertEquals(1_680, reading.etaSec)
        assertEquals(EtaOrigin.NAVI, reading.origin)
    }

    @Test fun `내비 안내가 없으면 보낼 값이 없다`() = runTest {
        navi.remainSec = null
        assertNull(source.read())
    }
}
