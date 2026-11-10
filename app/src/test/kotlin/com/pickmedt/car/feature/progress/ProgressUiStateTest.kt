package com.pickmedt.car.feature.progress

import com.pickmedt.car.core.model.DriveContext
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.feature.progress.ArrivalHeadline.Eta
import com.pickmedt.car.feature.progress.ArrivalHeadline.PickupNumber
import com.pickmedt.car.testing.kst
import com.pickmedt.car.testing.testOrder
import com.pickmedt.car.testing.testTiming
import com.pickmedt.car.ui.component.PleosButtonStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressUiStateTest {
    private val now = kst("08:02")
    private val card = SummaryCardText("픽미 커피 · 양재 DT점", "아이스 아메리카노 1잔", "Regular · 4,500원")
    private val detailLocked = ProgressAction("주문 상세 · 주차 후", PleosButtonStyle.Disabled, ProgressAction.Kind.NONE)

    private fun arrival(state: ProgressUiState) = state as ProgressUiState.Arrival

    @Test fun `V03 매장 확인 중`() {
        val state = toProgressUiState(testOrder(OrderStage.AWAITING_STORE), DriveContext.PARKED, now, baseline = null)
        assertEquals(
            ProgressUiState.Card(
                variant = ProgressVariant.AWAITING_STORE,
                eyebrow = "매장 확인 중",
                title = "매장에서\n예약을 확인 중이에요",
                card = card,
                lead = "수락 결과를 받으면 알려드릴게요.",
                note = "예약 요청을 전달했어요.\n확인 중에는 다시 주문하지 않아도 돼요.",
                action = ProgressAction("매장 응답 대기 중", PleosButtonStyle.Disabled, ProgressAction.Kind.NONE),
            ),
            state,
        )
    }

    @Test fun `V04-A 제조 대기 - 주행 중에는 상세 잠금`() {
        val timing = testTiming("08:08")
        val order = testOrder(OrderStage.WAITING_TO_PREPARE).copy(timing = timing)
        assertEquals(
            ProgressUiState.Arrival(
                variant = ProgressVariant.WAITING,
                movingTo = "양재 DT점으로 이동 중",
                title = "도착에 맞춰\n준비할 예정이에요",
                headline = Eta(minutes = "6", line = "08:08 도착 예정", largeLine = true),
                step = 1,
                menuLine = "아이스 아메리카노 1잔",
                orderLine = "Regular  ·  주문 P-021",
                action = detailLocked,
            ),
            toProgressUiState(order, DriveContext.DRIVING, now, baseline = timing),
        )
    }

    @Test fun `주차 확인이면 주문 상세를 열 수 있다`() {
        val order = testOrder(OrderStage.WAITING_TO_PREPARE).copy(timing = testTiming("08:08"))
        assertEquals(
            ProgressAction("주문 상세", PleosButtonStyle.Basic, ProgressAction.Kind.DETAIL),
            toProgressUiState(order, DriveContext.PARKED, now, baseline = null).action,
        )
    }

    @Test fun `일시 정차와 상태 불명에서는 상세 잠금`() {
        val order = testOrder(OrderStage.WAITING_TO_PREPARE).copy(timing = testTiming("08:08"))
        assertEquals(detailLocked, toProgressUiState(order, DriveContext.STOPPED, now, null).action)
        assertEquals(detailLocked, toProgressUiState(order, DriveContext.UNKNOWN, now, null).action)
    }

    @Test fun `V05-A 제조 전 지연 - 추천이 60초 이상 늦어지면 기존 도착 시각을 함께 보여 준다`() {
        val order = testOrder(OrderStage.WAITING_TO_PREPARE).copy(timing = testTiming("08:12"))
        val state = arrival(toProgressUiState(order, DriveContext.DRIVING, now, baseline = testTiming("08:08")))
        assertEquals(ProgressVariant.WAITING_DELAYED, state.variant)
        assertEquals("늦어진 도착에 맞춰\n준비할 예정이에요", state.title)
        assertEquals(Eta(minutes = "10", line = "08:12 도착 예정 · 기존 08:08", largeLine = false), state.headline)
        assertEquals(1, state.step)
    }

    @Test fun `도착이 59초 늦어진 것은 지연으로 보지 않는다`() {
        val baseline = testTiming("08:08")
        val later = baseline.copy(
            entranceEtaAt = baseline.entranceEtaAt.plusSeconds(59),
            expectedPickupAt = baseline.expectedPickupAt.plusSeconds(59),
            recommendedStartAt = baseline.recommendedStartAt.plusSeconds(59),
        )
        val order = testOrder(OrderStage.WAITING_TO_PREPARE).copy(timing = later)
        assertEquals(ProgressVariant.WAITING, toProgressUiState(order, DriveContext.DRIVING, now, baseline).variant)
    }

    @Test fun `V04-B 제조 중`() {
        val order = testOrder(OrderStage.PREPARING).copy(timing = testTiming("08:05"))
        val state = arrival(toProgressUiState(order, DriveContext.DRIVING, now, baseline = null))
        assertEquals(ProgressVariant.PREPARING, state.variant)
        assertEquals("매장에서\n준비하고 있어요", state.title)
        assertEquals(Eta(minutes = "3", line = "08:05 도착 예정", largeLine = false), state.headline)
        assertEquals(2, state.step)
    }

    @Test fun `V05-B 제조 후 지연 - 늦어진 시간이 60초 이상이면 제조 진행 중을 알린다`() {
        val order = testOrder(OrderStage.PREPARING).copy(timing = testTiming("08:12", delaySec = 240))
        val state = arrival(toProgressUiState(order, DriveContext.DRIVING, now, baseline = null))
        assertEquals(ProgressVariant.PREPARING_DELAYED, state.variant)
        assertEquals("이미 준비 중이에요\n도착하면 수령해 주세요", state.title)
        assertEquals(Eta(minutes = "10", line = "08:12 도착 예정 · 제조 진행 중", largeLine = false), state.headline)
        assertEquals(2, state.step)
    }

    @Test fun `V04-C 준비 완료 - 픽업 주문번호`() {
        val order = testOrder(OrderStage.READY).copy(timing = testTiming("08:08"))
        val state = arrival(toProgressUiState(order, DriveContext.DRIVING, now, baseline = null))
        assertEquals(ProgressVariant.READY, state.variant)
        assertEquals("주문이\n준비됐어요", state.title)
        assertEquals(PickupNumber(number = "P-021", guide = "DT 차로에서 수령해 주세요"), state.headline)
        assertEquals(3, state.step)
    }

    @Test fun `V07 수령 완료`() {
        val order = testOrder(OrderStage.PICKED_UP).copy(pickedUpAt = kst("08:10"))
        assertEquals(
            ProgressUiState.Card(
                variant = ProgressVariant.PICKED_UP,
                eyebrow = "수령 완료",
                title = "주문을\n수령했어요",
                card = card,
                lead = "안전하게 이동하세요.",
                note = "수령 완료 08:10 · 주문 P-021",
                action = ProgressAction("주문 완료", PleosButtonStyle.Filled, ProgressAction.Kind.FINISH),
            ),
            toProgressUiState(order, DriveContext.DRIVING, now, baseline = null),
        )
    }

    @Test fun `도착 정보가 없으면 시간 대신 확인 중을 보여 준다`() {
        val state = arrival(toProgressUiState(testOrder(OrderStage.WAITING_TO_PREPARE), DriveContext.DRIVING, now, null))
        assertEquals(Eta(minutes = "–", line = "도착 시간 확인 중", largeLine = true), state.headline)
    }

    @Test fun `처음 받은 타이밍을 기준으로 이후 지연을 판단한다`() {
        val presenter = ProgressPresenter()
        val first = testOrder(OrderStage.WAITING_TO_PREPARE).copy(timing = testTiming("08:08"))
        assertEquals(ProgressVariant.WAITING, presenter.present(first, DriveContext.DRIVING, now).variant)
        val delayed = first.copy(timing = testTiming("08:12"))
        assertEquals(ProgressVariant.WAITING_DELAYED, presenter.present(delayed, DriveContext.DRIVING, now).variant)
    }

    @Test fun `주문이 바뀌면 기준을 새로 잡는다`() {
        val presenter = ProgressPresenter()
        presenter.present(testOrder(OrderStage.WAITING_TO_PREPARE, id = "a").copy(timing = testTiming("08:08")), DriveContext.DRIVING, now)
        val next = testOrder(OrderStage.WAITING_TO_PREPARE, id = "b").copy(timing = testTiming("08:12"))
        assertEquals(ProgressVariant.WAITING, presenter.present(next, DriveContext.DRIVING, now).variant)
    }
}
