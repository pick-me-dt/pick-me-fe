package com.pickmedt.car.feature.home

import com.pickmedt.car.core.model.Money
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.data.fake.FakeCatalog
import com.pickmedt.car.data.fake.FakeOrderRepository
import com.pickmedt.car.data.fake.FakeServer
import com.pickmedt.car.data.fake.SampleData
import com.pickmedt.car.data.order.QuoteIssue
import com.pickmedt.car.platform.navi.scripted.ScriptedRouteSource
import com.pickmedt.car.testing.MutableClock
import com.pickmedt.car.testing.kst
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeControllerTest {
    private val server = FakeServer(MutableClock(kst("08:02")))
    private val catalog = FakeCatalog()
    private val routes = ScriptedRouteSource()
    private val controller = HomeController(FakeOrderRepository(server), catalog, routes)

    @Test fun `등록 메뉴가 있으면 시작 화면`() = runTest {
        controller.load()
        assertEquals(HomeStep.Start(SampleData.usualIcedAmericano), controller.step.value)
    }

    @Test fun `등록 메뉴가 없으면 등록 내역 없음`() = runTest {
        server.clearQuickOrders()
        controller.load()
        assertEquals(HomeStep.Empty, controller.step.value)
    }

    @Test fun `경로가 없으면 경로 없음`() = runTest {
        controller.load()
        routes.setRoute(null)
        controller.changeStoreAndMenu()
        assertEquals(HomeStep.NoRoute, controller.step.value)
    }

    @Test fun `경로에 매장이 없으면 이용 매장 없음`() = runTest {
        controller.load()
        catalog.noStores = true
        controller.changeStoreAndMenu()
        assertEquals(HomeStep.NoStore, controller.step.value)
    }

    @Test fun `매장을 고르고 메뉴를 고르면 등록 메뉴가 바뀐 시작 화면`() = runTest {
        controller.load()
        controller.changeStoreAndMenu()
        assertEquals(HomeStep.PickStore(listOf(SampleData.yangjaeOnRoute, SampleData.seochoOnRoute)), controller.step.value)
        controller.pickStore(SampleData.seocho)
        assertEquals(HomeStep.PickMenu(SampleData.seocho, listOf(SampleData.icedAmericano, SampleData.hotAmericano)), controller.step.value)
        controller.pickMenu(SampleData.hotAmericano)
        val start = controller.step.value as HomeStep.Start
        assertEquals("서초 DT점", start.quick.store.name)
        assertEquals("따뜻한 아메리카노", start.quick.lines.single().name)
    }

    @Test fun `이전으로는 한 단계씩 돌아간다`() = runTest {
        controller.load()
        controller.changeStoreAndMenu()
        controller.pickStore(SampleData.seocho)
        controller.back()
        assertEquals(HomeStep.PickStore(listOf(SampleData.yangjaeOnRoute, SampleData.seochoOnRoute)), controller.step.value)
        controller.back()
        assertEquals(HomeStep.Start(SampleData.usualIcedAmericano), controller.step.value)
    }

    @Test fun `예약을 누르면 견적을 받아 예약 확인`() = runTest {
        controller.load()
        controller.reserve()
        val confirm = controller.step.value as HomeStep.Confirm
        assertEquals(null, confirm.quote.issue)
        assertEquals(Money(4_500), confirm.quote.total)
    }

    @Test fun `품절이면 예약 확인 대신 품절, 다른 메뉴 선택은 같은 매장 메뉴로`() = runTest {
        server.markSoldOut()
        controller.load()
        controller.reserve()
        assertEquals(QuoteIssue.SOLD_OUT, (controller.step.value as HomeStep.Confirm).quote.issue)
        controller.chooseOtherMenu()
        assertEquals(SampleData.yangjae, (controller.step.value as HomeStep.PickMenu).store)
    }

    @Test fun `예약 요청을 보내면 주문이 생긴다`() = runTest {
        controller.load()
        controller.reserve()
        controller.submit("key-1")
        assertEquals(OrderStage.AWAITING_STORE, server.order.value?.stage)
    }
}
