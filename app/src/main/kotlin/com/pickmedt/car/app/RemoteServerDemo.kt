package com.pickmedt.car.app

import android.util.Log
import com.pickmedt.car.data.order.OrderRepository
import com.pickmedt.car.data.remote.RemoteDevTools
import com.pickmedt.car.demo.ServerDemoActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 실서버 시연 조작. 1차 기획 서버에는 직원 수락이 없어 「다음 단계」로 RESERVED → SENT(매장 전송)를 넘긴다.
 * 제조 시작·준비 완료·수령은 매장 대시보드(서버 /dashboard/)에서 누른다.
 * 서버 호출은 백그라운드로 보내고 바로 돌아온다. 실패는 logcat(태그 PickmeRemote)
 */
class RemoteServerDemo(
    private val dev: RemoteDevTools,
    private val orders: OrderRepository,
    private val scope: CoroutineScope,
) : ServerDemoActions {
    override fun next(): Result<Unit> {
        val orderId = orders.currentOrder.value?.id ?: return Result.failure(IllegalStateException("진행 중 주문이 없어요"))
        return launch("다음 단계") {
            dev.advance(orderId)
            orders.refresh()
        }
    }

    override fun refresh(): Result<Unit> = launch("새로고침") { orders.refresh() }

    override fun reset(): Result<Unit> = launch("서버 초기화") {
        dev.reset()
        orders.refresh()
        orders.clearFinished()
    }

    private fun launch(label: String, block: suspend () -> Unit): Result<Unit> {
        scope.launch {
            runCatching { block() }.onFailure { Log.w("PickmeRemote", "$label 실패: ${it.message}") }
        }
        return Result.success(Unit)
    }
}
