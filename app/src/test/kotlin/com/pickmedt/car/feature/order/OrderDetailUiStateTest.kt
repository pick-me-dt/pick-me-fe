package com.pickmedt.car.feature.order

import com.pickmedt.car.core.model.DriveContext
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.testing.testOrder
import com.pickmedt.car.testing.testTiming
import com.pickmedt.car.ui.component.PleosButtonStyle
import com.pickmedt.car.ui.component.SummaryCardText
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderDetailUiStateTest {
    private val card = SummaryCardText("픽미 커피 · 양재 DT점", "아이스 아메리카노 1잔", "Regular · 4,500원")
    private val waiting = testOrder(OrderStage.WAITING_TO_PREPARE).copy(timing = testTiming("08:08"))

    @Test fun `V06-A 주문 상세`() = assertEquals(
        OrderDetailUiState(
            variant = DetailVariant.OVERVIEW,
            eyebrow = "주문 상세",
            title = "주문 상세",
            card = card,
            lead = "예약 완료 · 제조 시작 전",
            note = "주문 P-021\n매장 도착 예정 08:08\n취소는 매장 확인 후 확정돼요.",
            actions = listOf(
                DetailAction("주문 유지", PleosButtonStyle.Basic, DetailAction.Kind.KEEP_ORDER),
                DetailAction("취소 요청", PleosButtonStyle.Filled, DetailAction.Kind.ASK_CANCEL),
            ),
        ),
        toOrderDetailUiState(waiting, DetailPage.OVERVIEW, DriveContext.PARKED),
    )

    @Test fun `도착 정보가 없으면 도착 예정 줄을 빼고 보여 준다`() = assertEquals(
        "주문 P-021\n취소는 매장 확인 후 확정돼요.",
        toOrderDetailUiState(testOrder(OrderStage.WAITING_TO_PREPARE), DetailPage.OVERVIEW, DriveContext.PARKED).note,
    )

    @Test fun `V06-B 취소 요청 확인`() = assertEquals(
        OrderDetailUiState(
            variant = DetailVariant.CONFIRM_CANCEL,
            eyebrow = "취소 요청 확인",
            title = "취소를 요청할까요?",
            card = card,
            lead = "매장이 승인해야 취소가 완료돼요.",
            note = "확인하는 동안 제조가 시작될 수 있어요.\n제조 후 요청은 매장 판단이 필요해요.",
            actions = listOf(
                DetailAction("주문 유지", PleosButtonStyle.Basic, DetailAction.Kind.KEEP_ORDER),
                DetailAction("매장에 취소 요청", PleosButtonStyle.Filled, DetailAction.Kind.SEND_CANCEL),
            ),
        ),
        toOrderDetailUiState(waiting, DetailPage.CONFIRM_CANCEL, DriveContext.PARKED),
    )

    @Test fun `V06-L 주행 중에는 상세 대신 제한 안내`() = assertEquals(
        OrderDetailUiState(
            variant = DetailVariant.DRIVING_LOCKED,
            eyebrow = "주행 중 상세 제한",
            title = "주차 후\n상세를 확인해 주세요",
            card = card,
            lead = "이동 중에는 주문 상태만 안내해요.",
            note = "제조 대기 · 예약은 유지 중이에요.\n주차 후 상세 내용과 취소 요청을 확인할 수 있어요.",
            actions = listOf(DetailAction("주문 상태로", PleosButtonStyle.Filled, DetailAction.Kind.KEEP_ORDER)),
        ),
        toOrderDetailUiState(waiting, DetailPage.OVERVIEW, DriveContext.DRIVING),
    )

    @Test fun `취소 확인 단계에서도 주차가 풀리면 제한 안내`() {
        assertEquals(DetailVariant.DRIVING_LOCKED, toOrderDetailUiState(waiting, DetailPage.CONFIRM_CANCEL, DriveContext.STOPPED).variant)
        assertEquals(DetailVariant.DRIVING_LOCKED, toOrderDetailUiState(waiting, DetailPage.CONFIRM_CANCEL, DriveContext.UNKNOWN).variant)
    }
}
