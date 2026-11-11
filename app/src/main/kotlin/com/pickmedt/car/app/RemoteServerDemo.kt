package com.pickmedt.car.app

import android.util.Log
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.data.order.OrderRepository
import com.pickmedt.car.data.remote.RemoteDevTools
import com.pickmedt.car.demo.ServerDemoActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 실서버 시연 조작. 매장 쪽 조작은 시드 직원 토큰으로 매장 API를 부른다 (매장 대시보드 /dashboard/에서 누르는 것과 같다).
 * 서버 호출은 백그라운드로 보내고 바로 돌아온다. 실패는 logcat(태그 PickmeRemote)
 */
class RemoteServerDemo(
    private val dev: RemoteDevTools,
    private val orders: OrderRepository,
    private val scope: CoroutineScope,
) : ServerDemoActions {
    override fun next(): Result<Unit> = withOrder("다음 단계") { dev.advance(it.id) }

    override fun refresh(): Result<Unit> = launch("새로고침") { orders.refresh() }

    override fun reset(): Result<Unit> = launch("서버 초기화") {
        dev.reset()
        orders.refresh()
        orders.clearFinished()
    }

    override fun accept() = storeOrder("매장 수락", "accept")

    /** 사유 기본값은 BUSY (Figma V03-R 예시 「주문량 증가」) */
    override fun reject(reason: String?): Result<Unit> {
        val code = (reason ?: "BUSY").uppercase()
        if (code !in REJECT_REASONS) return Result.failure(IllegalArgumentException("사유는 ${REJECT_REASONS.joinToString("·")} 중 하나"))
        return storeOrder("매장 거절", "reject", mapOf("reason" to code))
    }

    override fun prepare() = storeOrder("제조 시작", "start")
    override fun ready() = storeOrder("준비 완료", "ready")
    override fun pickup() = storeOrder("수령 완료", "pickup")
    override fun approveCancel() = storeOrder("취소 승인", "cancel-request/approve")
    override fun rejectCancel() = storeOrder("취소 거절", "cancel-request/reject")

    /** 홈 첫 줄 등록 메뉴의 항목을 품절로 */
    override fun soldOut(): Result<Unit> = launch("메뉴 품절") {
        val quick = checkNotNull(orders.quickOrders().firstOrNull()) { "등록 메뉴가 없어요" }
        quick.lines.forEach { dev.setSoldOut(quick.store.id, it.menuItemId, soldOut = true) }
    }

    /** 홈 첫 줄 등록 매장의 영업을 닫는다 */
    override fun closeStore(): Result<Unit> = launch("접수 종료") {
        val quick = checkNotNull(orders.quickOrders().firstOrNull()) { "등록 메뉴가 없어요" }
        dev.setOpen(quick.store.id, open = false)
    }

    /** 홈 첫 줄 등록 매장의 품절을 모두 풀고 영업을 연다 */
    override fun clearStore(): Result<Unit> = launch("조건 해제") {
        val quick = checkNotNull(orders.quickOrders().firstOrNull()) { "등록 메뉴가 없어요" }
        dev.setOpen(quick.store.id, open = true)
        dev.storeMenuIds(quick.store.id).forEach { dev.setSoldOut(quick.store.id, it, soldOut = false) }
    }

    private fun storeOrder(label: String, action: String, body: Map<String, String> = emptyMap()) =
        withOrder(label) { dev.storeOrder(it.store.id, it.id, action, body) }

    private fun withOrder(label: String, block: suspend (Order) -> Unit): Result<Unit> {
        val order = orders.currentOrder.value ?: return Result.failure(IllegalStateException("진행 중 주문이 없어요"))
        return launch(label) {
            block(order)
            orders.refresh()
        }
    }

    private fun launch(label: String, block: suspend () -> Unit): Result<Unit> {
        scope.launch {
            runCatching { block() }.onFailure { Log.w("PickmeRemote", "$label 실패: ${it.message}") }
        }
        return Result.success(Unit)
    }

    private companion object {
        /** pick-me-be orders.dto.ts RejectOrderDto */
        val REJECT_REASONS = listOf("SOLD_OUT", "BUSY", "CLOSED", "OTHER")
    }
}
