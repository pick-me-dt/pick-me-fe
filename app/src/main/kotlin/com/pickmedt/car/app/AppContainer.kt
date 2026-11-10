package com.pickmedt.car.app

import com.pickmedt.car.core.time.Clock
import com.pickmedt.car.data.fake.FakeOrderRepository
import com.pickmedt.car.data.fake.FakeServer
import com.pickmedt.car.data.order.OrderRepository
import com.pickmedt.car.demo.DemoController
import com.pickmedt.car.platform.drive.DriveSignalSource
import com.pickmedt.car.platform.drive.fake.FakeDriveSignalSource
import com.pickmedt.car.platform.navi.EtaSource
import com.pickmedt.car.platform.navi.scripted.ScriptedEtaSource
import kotlinx.coroutines.CoroutineScope

/**
 * 수동 의존성 주입. 가짜/실서버, 모의/실차 구현은 여기서만 고른다.
 * 지금은 서버 연동(11/13)과 에뮬레이터 게이트 전이라 모두 가짜·모의 구현이다.
 */
class AppContainer(appScope: CoroutineScope, val clock: Clock = Clock.System) {
    private val fakeServer = FakeServer(clock)
    private val fakeDrive = FakeDriveSignalSource()
    private val scriptedEta = ScriptedEtaSource(clock)

    val orderRepository: OrderRepository = FakeOrderRepository(fakeServer)
    val driveSignalSource: DriveSignalSource = fakeDrive
    private val etaSource: EtaSource = scriptedEta

    val etaReporter = EtaReporter(orderRepository, etaSource, appScope)

    val demo = DemoController(
        server = fakeServer,
        drive = fakeDrive,
        eta = scriptedEta,
        reportEtaNow = etaReporter::reportNow,
        scope = appScope,
    )
}
