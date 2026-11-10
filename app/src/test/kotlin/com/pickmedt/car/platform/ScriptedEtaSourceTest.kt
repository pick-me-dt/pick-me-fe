package com.pickmedt.car.platform

import com.pickmedt.car.core.model.EtaOrigin
import com.pickmedt.car.platform.navi.scripted.ScriptedEtaSource
import com.pickmedt.car.testing.MutableClock
import com.pickmedt.car.testing.kst
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration

class ScriptedEtaSourceTest {
    private val clock = MutableClock(kst("08:02"))
    private val source = ScriptedEtaSource(clock)

    @Test fun `설정 전에는 읽을 값이 없다`() = runTest {
        assertNull(source.read())
    }

    @Test fun `설정한 값이 실제 시간만큼 줄어든다`() = runTest {
        source.set(360)
        assertEquals(360, source.read()!!.etaSec)
        clock.advance(Duration.ofSeconds(60))
        val reading = source.read()!!
        assertEquals(300, reading.etaSec)
        assertEquals(kst("08:03"), reading.measuredAt)
        assertEquals(EtaOrigin.SCRIPTED, reading.origin)
    }

    @Test fun `지연을 더하면 남은 시간에 더해진다`() = runTest {
        source.set(360)
        clock.advance(Duration.ofSeconds(60))
        source.add(240)
        assertEquals(540, source.read()!!.etaSec)
    }

    @Test fun `0 아래로 내려가지 않는다`() = runTest {
        source.set(30)
        clock.advance(Duration.ofSeconds(60))
        assertEquals(0, source.read()!!.etaSec)
    }

    @Test fun `지우면 다시 읽을 값이 없다`() = runTest {
        source.set(360)
        source.clear()
        assertNull(source.read())
    }
}
