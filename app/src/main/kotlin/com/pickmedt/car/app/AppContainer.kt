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
import com.pickmedt.car.platform.navi.pleos.PleosNavi
import com.pickmedt.car.platform.navi.scripted.NaviSeededEtaSource
import com.pickmedt.car.platform.navi.scripted.ScriptedEtaSource
import com.pickmedt.car.platform.navi.scripted.ScriptedRouteSource
import kotlinx.coroutines.CoroutineScope

/**
 * 수동 의존성 주입. 가짜/실서버, 모의/실차 구현은 여기서만 고른다.
 * 서버는 연동(11/13) 전이라 가짜 구현이다. 사용자 설정은 앱(PickmeApplication)이 SharedPreferences 구현을 넘긴다.
 * 차량에서 실행하면 앱이 Car API 주행 신호([carDrive])와 Pleos 내비([carNavi])를 넘긴다. 없으면 모의 구현을 쓴다.
 * 차량의 ETA는 실제 내비의 첫 값에서 시작하는 모의 값이다 (에뮬레이터는 위치가 움직이지 않음, 게이트 G3).
 */
class AppContainer(
    appScope: CoroutineScope,
    val userSettings: UserSettingsRepository,
    carDrive: DriveSignalSource? = null,
    carNavi: PleosNavi? = null,
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
    val routeSource: RouteSource = carNavi ?: scriptedRoute
    private val etaSource: EtaSource = carNavi?.let { NaviSeededEtaSource(it, scriptedEta) } ?: scriptedEta

    val etaReporter = EtaReporter(orderRepository, etaSource, appScope)

    val demo = DemoController(
        server = fakeServer,
        drive = fakeDrive.takeIf { carDrive == null },
        eta = scriptedEta,
        catalog = fakeCatalog,
        routes = scriptedRoute.takeIf { carNavi == null },
        settings = userSettings,
        navi = carNavi,
        reportEtaNow = etaReporter::reportNow,
        pauseEtaReports = etaReporter::stop,
        resumeEtaReports = etaReporter::start,
        scope = appScope,
    )
}
