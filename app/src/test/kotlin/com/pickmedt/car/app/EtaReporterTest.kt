package com.pickmedt.car.app

import com.pickmedt.car.core.model.EtaReading
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.core.model.QuickOrder
import com.pickmedt.car.data.order.OrderRepository
import com.pickmedt.car.data.order.Quote
import com.pickmedt.car.platform.navi.scripted.ScriptedEtaSource
import com.pickmedt.car.testing.MutableClock
import com.pickmedt.car.testing.kst
import com.pickmedt.car.testing.testOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EtaReporterTest {
    private val eta = ScriptedEtaSource(MutableClock(kst("08:02"))).apply { set(360) }
    private val orders = RecordingOrders()

    @Test fun `진행 중 주문이 없으면 보내지 않는다`() = runTest {
        EtaReporter(orders, eta, backgroundScope).start()
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(0, orders.reports.size)
    }

    @Test fun `진행 중 주문이 생기면 바로 보내고 30초마다 다시 보낸다`() = runTest {
        EtaReporter(orders, eta, backgroundScope).start()
        orders.currentOrder.value = testOrder(OrderStage.WAITING_TO_PREPARE)
        runCurrent()
        assertEquals(listOf("order-1" to 360), orders.reports)
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(2, orders.reports.size)
    }

    @Test fun `주문이 끝나면 보내지 않는다`() = runTest {
        EtaReporter(orders, eta, backgroundScope).start()
        orders.currentOrder.value = testOrder(OrderStage.WAITING_TO_PREPARE)
        runCurrent()
        orders.currentOrder.value = testOrder(OrderStage.PICKED_UP)
        runCurrent()
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(1, orders.reports.size)
    }

    @Test fun `reportNow는 주기와 상관없이 바로 보낸다`() = runTest {
        val reporter = EtaReporter(orders, eta, backgroundScope)
        orders.currentOrder.value = testOrder(OrderStage.PREPARING)
        reporter.reportNow()
        assertEquals(1, orders.reports.size)
    }

    private class RecordingOrders : OrderRepository {
        override val currentOrder = MutableStateFlow<Order?>(null)
        val reports = mutableListOf<Pair<String, Int>>()

        override suspend fun reportEta(orderId: String, reading: EtaReading) {
            reports += orderId to reading.etaSec
        }

        override suspend fun quickOrders(): List<QuickOrder> = emptyList()
        override suspend fun quote(quickOrderId: String): Quote = error("사용하지 않음")
        override suspend fun reserve(quickOrderId: String, idempotencyKey: String): Order = error("사용하지 않음")
        override suspend fun requestCancel(orderId: String) = Unit
        override suspend fun refresh() = Unit
        override fun clearFinished() = Unit
    }
}
