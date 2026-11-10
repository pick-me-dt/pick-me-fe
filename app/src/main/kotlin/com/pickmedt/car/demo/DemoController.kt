package com.pickmedt.car.demo

import com.pickmedt.car.core.model.Gear
import com.pickmedt.car.data.fake.FakeServer
import com.pickmedt.car.platform.drive.fake.FakeDriveSignalSource
import com.pickmedt.car.platform.navi.scripted.ScriptedEtaSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Duration

/**
 * 시연 컨트롤 (Figma 프로토타입 하단 '시연 컨트롤' 영역).
 * 가짜 서버의 매장 조작, 모의 ETA, 모의 주행 신호를 바꾼다. 실제 주문·차량과 연동되지 않는다.
 */
class DemoController(
    private val server: FakeServer,
    private val drive: FakeDriveSignalSource,
    private val eta: ScriptedEtaSource,
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

    /** V04-E. ETA 전송을 멈추고 마지막 수신을 90초 넘게 지난 것으로 만든다 */
    fun disconnect() = runCatching {
        pauseEtaReports()
        server.backdateEtaReceipt(Duration.ofSeconds(91))
    }

    /** ETA 전송을 다시 시작한다. 시작하면 바로 한 번 보낸다 */
    fun reconnect() = runCatching { resumeEtaReports() }

    fun reset() = runCatching {
        server.reset()
        eta.clear()
        drive.shift(Gear.PARK)
        resumeEtaReports()
    }
}
