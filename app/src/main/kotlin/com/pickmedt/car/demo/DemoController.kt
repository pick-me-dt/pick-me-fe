package com.pickmedt.car.demo

import com.pickmedt.car.core.model.Gear
import com.pickmedt.car.core.model.Money
import com.pickmedt.car.data.fake.FakeCatalog
import com.pickmedt.car.data.fake.FakeServer
import com.pickmedt.car.data.settings.UserSettingsRepository
import com.pickmedt.car.platform.drive.fake.FakeDriveSignalSource
import com.pickmedt.car.platform.navi.pleos.PleosNaviProbe
import com.pickmedt.car.platform.navi.scripted.ScriptedEtaSource
import com.pickmedt.car.platform.navi.scripted.ScriptedRouteSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Duration

/**
 * 시연 컨트롤 (Figma 프로토타입 하단 '시연 컨트롤' 영역).
 * 가짜 서버의 매장 조작·예약 전 확인 조건, 모의 ETA·경로·주행 신호, 기기 설정을 바꾼다.
 * 실제 주문·차량과 연동되지 않는다.
 */
class DemoController(
    private val server: FakeServer,
    /** 실차 신호를 쓰면 null. 이때 기어는 Virtual Console에서 바꾼다 */
    private val drive: FakeDriveSignalSource?,
    private val eta: ScriptedEtaSource,
    private val catalog: FakeCatalog,
    private val routes: ScriptedRouteSource,
    private val settings: UserSettingsRepository,
    /** 차량에서만 있다. 게이트 G3 확인용 */
    private val naviProbe: PleosNaviProbe?,
    private val reportEtaNow: suspend () -> Unit,
    private val pauseEtaReports: () -> Unit,
    private val resumeEtaReports: () -> Unit,
    private val scope: CoroutineScope,
) {
    fun accept() = runCatching { server.accept() }

    /** V03-R. Figma 예시 사유 "주문량 증가" */
    fun reject() = runCatching { server.reject(note = "주문량 증가") }

    /** V03-T. 예약 요청을 2분 전으로 돌려 응답 지연을 바로 보여 준다 */
    fun slowStoreResponse() = runCatching { server.backdateRequest(Duration.ofMinutes(2)) }

    fun startPreparing() = runCatching { server.startPreparing() }
    fun markReady() = runCatching { server.markReady() }
    fun pickUp() = runCatching { server.pickUp() }
    fun approveCancel() = runCatching { server.approveCancel() }
    fun rejectCancel() = runCatching { server.rejectCancel() }

    fun shift(gear: Gear) = runCatching {
        checkNotNull(drive) { "실차 기어 사용 중 · Virtual Console에서 바꾸세요" }.shift(gear)
    }

    fun setEta(etaSec: Int) = runCatching {
        eta.set(etaSec)
        scope.launch { reportEtaNow() }
        Unit
    }

    fun delayEta(deltaSec: Int) = runCatching {
        eta.add(deltaSec)
        scope.launch { reportEtaNow() }
        Unit
    }

    /** V04-E. ETA 전송을 멈추고 마지막 수신을 90초 넘게 지난 것으로 만든다 */
    fun disconnect() = runCatching {
        pauseEtaReports()
        server.backdateEtaReceipt(Duration.ofSeconds(91))
    }

    /** ETA 전송을 다시 시작한다. 시작하면 바로 한 번 보낸다 */
    fun reconnect() = runCatching { resumeEtaReports() }

    // ── 내비 확인 (게이트 G3) ─────────────────

    /** 양재점으로 실제 내비 경로를 요청한다. 결과는 logcat 태그 PickmeNavi */
    fun naviRoute() = runCatching { checkNotNull(naviProbe) { "차량에서만 쓸 수 있어요" }.requestRouteToYangjae() }

    /** 실제 내비의 경로 상태와 남은 시간을 조회한다. 결과는 logcat 태그 PickmeNavi */
    fun naviQuery() = runCatching { checkNotNull(naviProbe) { "차량에서만 쓸 수 있어요" }.query() }

    // ── 시작 (V00) ──────────────────────────

    /** 동의·차량번호를 지우고 주문을 비운다. 화면을 스플래시로 되돌리는 일은 AppRoot가 한다 */
    fun firstRun() = runCatching {
        settings.reset()
        server.reset()
        eta.clear()
        drive?.shift(Gear.PARK)
        resumeEtaReports()
    }

    /** V00-E. 다음 서버 조회 한 번을 실패시킨다. 「첫 실행」·「앱 다시 시작」 전에 누른다 */
    fun failNextLoad() = runCatching { server.failNextRefresh() }

    // ── 홈·예약 (V01·V02) ────────────────────

    /** V01-E */
    fun clearQuickOrders() = runCatching { server.clearQuickOrders() }

    /** V01-R */
    fun removeRoute() = runCatching { routes.setRoute(null) }

    fun restoreRoute() = runCatching { routes.setRoute(ScriptedRouteSource.DEFAULT) }

    /** V01-NO */
    fun noStores() = runCatching { catalog.noStores = true }

    /** V02-SO. 지금 등록 메뉴를 품절로 */
    fun soldOut() = runCatching { server.markSoldOut() }

    /** V02-PC. Figma 예시 금액 4,800원 */
    fun priceUp() = runCatching { server.changePrice(Money(4_800)) }

    /** V02-CL. 지금 등록 매장의 접수를 닫는다 */
    fun closeStore() = runCatching { server.closeStore() }

    /** 경로·매장·품절·가격·접수 조건을 정상으로 */
    fun clearIssues() = runCatching {
        server.clearQuoteIssues()
        catalog.noStores = false
        routes.setRoute(ScriptedRouteSource.DEFAULT)
    }

    fun reset() = runCatching {
        server.resetScenario()
        catalog.noStores = false
        routes.setRoute(ScriptedRouteSource.DEFAULT)
        eta.clear()
        drive?.shift(Gear.PARK)
        resumeEtaReports()
    }
}
