package com.pickmedt.car.demo

import com.pickmedt.car.core.model.Gear
import com.pickmedt.car.data.fake.FakeServer
import com.pickmedt.car.platform.drive.fake.FakeDriveSignalSource
import com.pickmedt.car.platform.navi.scripted.ScriptedEtaSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 시연 컨트롤 (Figma 프로토타입 하단 '시연 컨트롤' 영역).
 * 가짜 서버의 매장 조작, 모의 ETA, 모의 주행 신호를 바꾼다. 실제 주문·차량과 연동되지 않는다.
 */
class DemoController(
    private val server: FakeServer,
    private val drive: FakeDriveSignalSource,
    private val eta: ScriptedEtaSource,
    private val reportEtaNow: suspend () -> Unit,
    private val scope: CoroutineScope,
) {
    fun accept() = runCatching { server.accept() }
    fun reject() = runCatching { server.reject() }
    fun startPreparing() = runCatching { server.startPreparing() }
    fun markReady() = runCatching { server.markReady() }
    fun pickUp() = runCatching { server.pickUp() }
    fun approveCancel() = runCatching { server.approveCancel() }
    fun rejectCancel() = runCatching { server.rejectCancel() }

    fun shift(gear: Gear) = runCatching { drive.shift(gear) }

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

    fun reset() = runCatching {
        server.reset()
        eta.clear()
        drive.shift(Gear.PARK)
    }
}
