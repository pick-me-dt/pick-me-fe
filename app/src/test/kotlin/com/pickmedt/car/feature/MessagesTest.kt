package com.pickmedt.car.feature

import com.pickmedt.car.core.model.DriveContext
import com.pickmedt.car.core.model.Money
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.data.fake.SampleData
import com.pickmedt.car.data.order.Quote
import com.pickmedt.car.data.order.QuoteIssue
import com.pickmedt.car.feature.home.emptyMessage
import com.pickmedt.car.feature.home.pickMenuMessage
import com.pickmedt.car.feature.home.pickStoreMessage
import com.pickmedt.car.feature.launch.LaunchStep
import com.pickmedt.car.feature.launch.consentMessage
import com.pickmedt.car.feature.launch.resumeOrderMessage
import com.pickmedt.car.feature.reserve.toReserveIssueMessage
import com.pickmedt.car.feature.settings.settingsMessage
import com.pickmedt.car.testing.testOrder
import com.pickmedt.car.ui.component.PleosButtonStyle.Basic
import com.pickmedt.car.ui.component.PleosButtonStyle.Disabled
import com.pickmedt.car.ui.component.PleosButtonStyle.Filled
import org.junit.Assert.assertEquals
import org.junit.Test

/** 3순위 화면에서 데이터로 만드는 문구와 버튼 상태 (Figma 예시 문구 기준) */
class MessagesTest {
    private val quick = SampleData.usualIcedAmericano

    private fun quote(issue: QuoteIssue?, total: Int = 4_500, previous: Int? = null) = Quote(
        quickOrderId = quick.id,
        store = quick.store,
        lines = quick.lines.map { it.copy(unitPrice = Money(total)) },
        total = Money(total),
        issue = issue,
        previousTotal = previous?.let(::Money),
    )

    @Test fun `V00-RE 진행 주문 안내는 매장 메뉴 현재 상태`() = assertEquals(
        "양재 DT점 · 아이스 아메리카노 1잔\n현재 상태: 제조 대기",
        resumeOrderMessage(testOrder(OrderStage.WAITING_TO_PREPARE)).lead,
    )

    @Test fun `V00-C 두 항목을 고르기 전에는 동의하지 않음이 주요 버튼`() {
        val actions = consentMessage(LaunchStep.Consent(), DriveContext.PARKED).actions
        assertEquals(listOf(Filled, Disabled), actions.map { it.style })
    }

    @Test fun `V00-C 두 항목을 고르면 동의하고 계속이 주요 버튼`() {
        val actions = consentMessage(LaunchStep.Consent(personalInfo = true, location = true), DriveContext.PARKED).actions
        assertEquals(listOf(Basic, Filled), actions.map { it.style })
    }

    @Test fun `V01-E 주행 중에는 등록할 수 없다`() =
        assertEquals(Disabled, emptyMessage(DriveContext.DRIVING).actions.single().style)

    @Test fun `V01-M 매장 버튼은 이름과 도착 시간, 첫 매장만 채움`() {
        val actions = pickStoreMessage(listOf(SampleData.yangjaeOnRoute, SampleData.seochoOnRoute), DriveContext.PARKED).actions
        assertEquals(listOf("양재 DT점 · 도착 6분", "서초 DT점 · 도착 9분", "이전으로"), actions.map { it.label })
        assertEquals(listOf(Filled, Basic, Basic), actions.map { it.style })
    }

    @Test fun `V01-M 매장은 3곳까지`() {
        val stores = List(5) { SampleData.yangjaeOnRoute }
        assertEquals(4, pickStoreMessage(stores, DriveContext.PARKED).actions.size)
    }

    @Test fun `V01-N 메뉴 버튼은 이름과 금액, 안내는 브랜드와 매장`() {
        val state = pickMenuMessage(SampleData.yangjae, listOf(SampleData.icedAmericano, SampleData.hotAmericano), DriveContext.PARKED)
        assertEquals("픽미 커피 · 양재 DT점\n주차 중 등록 메뉴를 선택해요.", state.lead)
        assertEquals(listOf("아이스 아메리카노 · 4,500원", "따뜻한 아메리카노 · 4,500원", "이전으로"), state.actions.map { it.label })
    }

    @Test fun `V01-N 주행 중에는 메뉴를 고를 수 없고 이전으로만 누를 수 있다`() {
        val actions = pickMenuMessage(SampleData.yangjae, listOf(SampleData.icedAmericano), DriveContext.DRIVING).actions
        assertEquals(listOf(Disabled, Basic), actions.map { it.style })
    }

    @Test fun `V02-SO 품절 안내는 메뉴 이름에 을를`() = assertEquals(
        "아이스 아메리카노를 지금 예약할 수 없어요.\n아직 예약은 확정되지 않았어요.",
        toReserveIssueMessage(quote(QuoteIssue.SOLD_OUT), DriveContext.PARKED, submitting = false).lead,
    )

    @Test fun `V02-PC 가격 변경은 기존과 현재 금액, 버튼은 현재 금액으로 예약 요청`() {
        val state = toReserveIssueMessage(quote(QuoteIssue.PRICE_CHANGED, total = 4_800, previous = 4_500), DriveContext.PARKED, submitting = false)
        assertEquals("아이스 아메리카노 1잔\n기존 4,500원 → 현재 4,800원", state.lead)
        assertEquals("4,800원으로 예약 요청", state.actions.last().label)
        assertEquals(Filled, state.actions.last().style)
    }

    @Test fun `V02-PC 요청 중이거나 주행 중이면 예약 요청을 누를 수 없다`() {
        val changed = quote(QuoteIssue.PRICE_CHANGED, total = 4_800, previous = 4_500)
        assertEquals(Disabled, toReserveIssueMessage(changed, DriveContext.PARKED, submitting = true).actions.last().style)
        assertEquals(Disabled, toReserveIssueMessage(changed, DriveContext.DRIVING, submitting = false).actions.last().style)
    }

    @Test fun `V02-CL 접수 종료 안내는 매장 이름`() = assertEquals(
        "양재 DT점에서 현재 주문을 받지 않아요.\n아직 예약은 확정되지 않았어요.",
        toReserveIssueMessage(quote(QuoteIssue.STORE_CLOSED), DriveContext.PARKED, submitting = false).lead,
    )

    @Test fun `V08 차량번호 버튼은 표시 형식`() =
        assertEquals("차량번호 · 12가 3456", settingsMessage("12가3456").actions.first().label)

    @Test fun `V08 차량번호가 없으면 등록 버튼`() =
        assertEquals("차량번호 등록", settingsMessage(null).actions.first().label)
}
