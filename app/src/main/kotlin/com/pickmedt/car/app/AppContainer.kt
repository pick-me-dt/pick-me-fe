package com.pickmedt.car.app

import com.pickmedt.car.core.time.Clock
import com.pickmedt.car.data.catalog.CatalogRepository
import com.pickmedt.car.data.fake.FakeCatalog
import com.pickmedt.car.data.fake.FakeOrderRepository
import com.pickmedt.car.data.fake.FakeServer
import com.pickmedt.car.data.order.OrderRepository
import com.pickmedt.car.data.settings.UserSettingsRepository
import com.pickmedt.car.demo.DemoController
import com.pickmedt.car.platform.drive.DriveSignalSource
import com.pickmedt.car.platform.drive.fake.FakeDriveSignalSource
import com.pickmedt.car.platform.navi.EtaSource
import com.pickmedt.car.platform.navi.RouteSource
import com.pickmedt.car.platform.navi.pleos.PleosNaviProbe
import com.pickmedt.car.platform.navi.scripted.ScriptedEtaSource
import com.pickmedt.car.platform.navi.scripted.ScriptedRouteSource
import kotlinx.coroutines.CoroutineScope

/**
 * 수동 의존성 주입. 가짜/실서버, 모의/실차 구현은 여기서만 고른다.
 * 지금은 서버 연동(11/13)과 에뮬레이터 게이트 전이라 사용자 설정을 빼고 모두 가짜·모의 구현이다.
 * 사용자 설정은 기기 저장이라 앱(PickmeApplication)이 SharedPreferences 구현을 넘긴다.
 * 차량에서 실행하면 앱이 Car API 주행 신호([carDrive])를 넘기고, 없으면 모의 신호를 쓴다.
 */
class AppContainer(
    appScope: CoroutineScope,
    val userSettings: UserSettingsRepository,
    carDrive: DriveSignalSource? = null,
    naviProbe: PleosNaviProbe? = null,
    val clock: Clock = Clock.System,
) {
    private val fakeServer = FakeServer(clock)
    private val fakeCatalog = FakeCatalog()
    private val fakeDrive = FakeDriveSignalSource()
    private val scriptedEta = ScriptedEtaSource(clock)
    private val scriptedRoute = ScriptedRouteSource()

    val orderRepository: OrderRepository = FakeOrderRepository(fakeServer)
    val catalog: CatalogRepository = fakeCatalog
    val driveSignalSource: DriveSignalSource = carDrive ?: fakeDrive
    val routeSource: RouteSource = scriptedRoute
    private val etaSource: EtaSource = scriptedEta

    val etaReporter = EtaReporter(orderRepository, etaSource, appScope)

    val demo = DemoController(
        server = fakeServer,
        drive = fakeDrive.takeIf { carDrive == null },
        eta = scriptedEta,
        catalog = fakeCatalog,
        routes = scriptedRoute,
        settings = userSettings,
        naviProbe = naviProbe,
        reportEtaNow = etaReporter::reportNow,
        pauseEtaReports = etaReporter::stop,
        resumeEtaReports = etaReporter::start,
        scope = appScope,
    )
}
