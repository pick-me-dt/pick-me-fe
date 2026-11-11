package com.pickmedt.car.data.fake

import com.pickmedt.car.core.model.EtaReading
import com.pickmedt.car.core.model.MenuOption
import com.pickmedt.car.core.model.Money
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.model.QuickOrder
import com.pickmedt.car.core.model.StoreSummary
import com.pickmedt.car.data.order.OrderRepository
import com.pickmedt.car.data.order.Quote
import kotlinx.coroutines.flow.StateFlow

/** 가짜 서버를 쓰는 주문 저장소. 실서버 연동 전 화면 개발과 시연용 */
class FakeOrderRepository(private val server: FakeServer) : OrderRepository {
    override val currentOrder: StateFlow<Order?> = server.order

    override suspend fun quickOrders(): List<QuickOrder> = server.quickOrders

    override suspend fun saveQuickOrder(store: StoreSummary, menu: MenuOption): QuickOrder = server.saveQuickOrder(store, menu)

    override suspend fun quote(quickOrderId: String): Quote = server.quote(quickOrderId)

    override suspend fun reserve(quickOrderId: String, idempotencyKey: String, expectedTotal: Money?): Order =
        server.reserve(quickOrderId, idempotencyKey, expectedTotal)

    override suspend fun reportEta(orderId: String, reading: EtaReading) = server.reportEta(orderId, reading.etaSec)

    override suspend fun requestCancel(orderId: String) = server.requestCancel(orderId)

    override suspend fun refresh() = server.refresh()

    override fun clearFinished() {
        if (server.order.value?.stage?.isActive == false) server.reset()
    }
}
