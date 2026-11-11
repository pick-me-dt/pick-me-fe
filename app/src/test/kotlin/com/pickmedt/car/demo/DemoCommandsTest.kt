package com.pickmedt.car.demo

import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.data.fake.FakeCatalog
import com.pickmedt.car.data.fake.FakeServer
import com.pickmedt.car.data.fake.InMemoryUserSettings
import com.pickmedt.car.data.fake.SampleData
import com.pickmedt.car.platform.drive.fake.FakeDriveSignalSource
import com.pickmedt.car.platform.navi.scripted.ScriptedEtaSource
import com.pickmedt.car.platform.navi.scripted.ScriptedRouteSource
import com.pickmedt.car.testing.MutableClock
import com.pickmedt.car.testing.kst
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoCommandsTest {
    private val clock = MutableClock(kst("08:02"))
    private val server = FakeServer(clock)
    private val eta = ScriptedEtaSource(clock)
    private val scope = TestScope()
    private var savedPanel: Boolean? = null
    private val controller = DemoController(
        server = server,
        drive = FakeDriveSignalSource(),
        eta = eta,
        catalog = FakeCatalog(),
        routes = ScriptedRouteSource(),
        settings = InMemoryUserSettings(),
        navi = null,
        reportEtaNow = {},
        pauseEtaReports = {},
        resumeEtaReports = {},
        scope = scope,
        onPanelVisibilityChanged = { savedPanel = it },
    )
    private val commands = DemoCommands(controller)

    @Test fun `매장 명령은 버튼과 같은 동작`() {
        server.reserve(SampleData.usualIcedAmericano.id, "key")
        assertEquals("매장 수락 완료", commands.run("accept", null))
        assertEquals(OrderStage.WAITING_TO_PREPARE, server.order.value?.stage)
    }

    @Test fun `순서가 맞지 않으면 실패 사유를 돌려준다`() {
        val result = commands.run("ready", null)
        assertTrue(result, result.startsWith("준비 완료 실패:"))
    }

    @Test fun `ETA는 분으로 받는다`() = runTest {
        assertEquals("ETA 설정 완료", commands.run("eta", "6"))
        assertEquals(360, eta.read()!!.etaSec)
        assertEquals("ETA 늘리기 완료", commands.run("delay", "4"))
        assertEquals(600, eta.read()!!.etaSec)
    }

    @Test fun `분이 숫자가 아니면 실패`() {
        assertTrue(commands.run("eta", "여섯").startsWith("ETA 설정 실패:"))
    }

    @Test fun `시연 컨트롤 숨기기는 상태를 바꾸고 저장한다`() {
        commands.run("panel", "off")
        assertFalse(controller.panelVisible.value)
        assertEquals(false, savedPanel)
        commands.run("panel", "on")
        assertTrue(controller.panelVisible.value)
    }

    @Test fun `화면 흐름 명령은 화면 이벤트로 보낸다`() = scope.runTest {
        var received: DemoUiEvent? = null
        val job = launch { received = controller.uiEvents.first() }
        runCurrent()
        commands.run("settings", null)
        runCurrent()
        assertEquals(DemoUiEvent.OPEN_SETTINGS, received)
        job.cancel()
    }

    @Test fun `모르는 명령은 안내`() {
        assertEquals("모르는 명령: fly (help로 목록 확인)", commands.run("fly", null))
    }

    @Test fun `help는 명령 목록`() {
        val help = commands.run("help", null)
        assertTrue(help.contains("accept"))
        assertTrue(help.contains("eta <분>"))
        assertTrue(help.contains("panel on|off"))
    }

    @Test fun `내비 경로는 좌표를 위도,경도로 받는다`() {
        val bad = commands.run("navi-route", "37.42")
        assertTrue(bad, bad.startsWith("내비 경로(양재) 실패: 좌표는 위도,경도로"))
        // 좌표는 맞고, 이 테스트에는 실제 내비가 없어 경로 요청 단계에서 실패한다
        assertEquals("내비 경로(양재) 실패: 차량에서만 쓸 수 있어요", commands.run("navi-route", "37.4199, 127.0958"))
        assertEquals("내비 경로(양재) 실패: 차량에서만 쓸 수 있어요", commands.run("navi-route", null))
    }

    @Test fun `실서버 모드에서는 같은 이름의 명령을 서버로 보낸다`() {
        val called = mutableListOf<String>()
        val ok = Result.success(Unit)
        val serverCommands = DemoCommands(
            controller,
            object : ServerDemoActions {
                override fun next() = ok.also { called += "next" }
                override fun refresh() = ok
                override fun reset() = ok
                override fun accept() = ok.also { called += "accept" }
                override fun reject(reason: String?) = ok.also { called += "reject:$reason" }
                override fun prepare() = ok
                override fun ready() = ok
                override fun pickup() = ok
                override fun approveCancel() = ok
                override fun rejectCancel() = ok
                override fun soldOut() = ok
                override fun closeStore() = ok
                override fun clearStore() = ok
            },
        )
        assertEquals("매장 수락 완료", serverCommands.run("accept", null))
        assertEquals("매장 거절 완료", serverCommands.run("reject", "SOLD_OUT"))
        assertEquals("다음 단계 완료", serverCommands.run("next", null))
        assertEquals(listOf("accept", "reject:SOLD_OUT", "next"), called)
        // 가짜 서버(앱 안)에 직접 손대지 않는다
        assertEquals(null, server.order.value)
        // 서버에 API가 없는 조작은 이유를 알려 준다
        assertTrue(serverCommands.run("price-up", null).startsWith("가격 변경 실패: 실서버에는"))
        assertEquals("모르는 명령: no-route (help로 목록 확인)", serverCommands.run("no-route", null))
    }
}
