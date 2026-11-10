package com.pickmedt.car.app

import com.pickmedt.car.core.rule.EtaPolicy
import com.pickmedt.car.data.order.OrderRepository
import com.pickmedt.car.platform.navi.EtaSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Duration

/**
 * 진행 중 주문이 있는 동안 ETA를 주기적으로 서버에 보낸다 (2차 기획 §5: 30초 갱신).
 * 주문이 없거나 끝나면 멈춰 백그라운드 폴링을 줄인다.
 */
class EtaReporter(
    private val orders: OrderRepository,
    private val eta: EtaSource,
    private val scope: CoroutineScope,
    private val interval: Duration = EtaPolicy.ReportInterval,
) {
    private var job: Job? = null

    /** 마지막으로 ETA 측정을 시작한 주문. 연결 끊김 후 다시 시작해도 같은 주문이면 측정을 이어 간다 */
    private var tripOrderId: String? = null

    fun start() {
        if (job != null) return
        job = scope.launch {
            orders.currentOrder
                .map { order -> order?.takeIf { it.stage.isActive }?.id }
                .distinctUntilChanged()
                .collectLatest { orderId ->
                    if (orderId == null) return@collectLatest
                    if (orderId != tripOrderId) {
                        tripOrderId = orderId
                        eta.startNewTrip()
                    }
                    while (true) {
                        report(orderId)
                        delay(interval.toMillis())
                    }
                }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    /** 시연 조작 직후처럼 주기를 기다리지 않고 바로 보낼 때 */
    suspend fun reportNow() {
        val orderId = orders.currentOrder.value?.takeIf { it.stage.isActive }?.id ?: return
        report(orderId)
    }

    private suspend fun report(orderId: String) {
        val reading = eta.read() ?: return
        try {
            orders.reportEta(orderId, reading)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 다음 주기에 다시 보낸다. 끊김 표시는 서버의 마지막 수신 시각으로 판단한다 (2차 기획 §5)
        }
    }
}
