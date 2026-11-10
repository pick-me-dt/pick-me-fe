package com.pickmedt.car.feature.progress

import com.pickmedt.car.core.model.CancelReason
import com.pickmedt.car.core.model.DriveContext
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.feature.progress.ArrivalHeadline.Eta
import com.pickmedt.car.feature.progress.ArrivalHeadline.PickupNumber
import com.pickmedt.car.testing.kst
import com.pickmedt.car.testing.testOrder
import com.pickmedt.car.testing.testTiming
import com.pickmedt.car.ui.component.InfoBoxText
import com.pickmedt.car.ui.component.PleosButtonStyle
import com.pickmedt.car.ui.component.SummaryCardText
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressUiStateTest {
    private val now = kst("08:02")
    private val card = SummaryCardText("픽미 커피 · 양재 DT점", "아이스 아메리카노 1잔", "Regular · 4,500원")
    private val detailLocked = listOf(ProgressAction("주문 상세 · 주차 후", PleosButtonStyle.Disabled, ProgressAction.Kind.NONE))

    private fun arrival(state: ProgressUiState) = state as ProgressUiState.Arrival

    @Test fun `V03 매장 확인 중`() {
        val order = testOrder(OrderStage.AWAITING_STORE).copy(requestedAt = kst("08:02"))
        assertEquals(
            ProgressUiState.Card(
                variant = ProgressVariant.AWAITING_STORE,
                eyebrow = "매장 확인 중",
                title = "매장에서\n예약을 확인 중이에요",
                card = card,
                lead = "수락 결과를 받으면 알려드릴게요.",
                note = "예약 요청을 전달했어요.\n확인 중에는 다시 주문하지 않아도 돼요.",
                actions = listOf(ProgressAction("매장 응답 대기 중", PleosButtonStyle.Disabled, ProgressAction.Kind.NONE)),
            ),
            toProgressUiState(order, DriveContext.PARKED, now, baseline = null),
        )
    }

    @Test fun `V03-T 응답 지연 - 요청 후 60초가 지나면 예약 상태 확인을 안내한다`() {
        val order = testOrder(OrderStage.AWAITING_STORE).copy(requestedAt = kst("08:01"))
        assertEquals(
            ProgressUiState.Card(
                variant = ProgressVariant.STORE_SLOW,
                eyebrow = "응답 지연",
                title = "매장 확인이\n조금 늦어지고 있어요",
                card = card,
                lead = "예약 결과를 확인하고 있어요.",
                note = "응답이 늦어도 실패로 처리하지 않아요.\n현재 예약 상태를 다시 확인해 주세요.",
                actions = listOf(ProgressAction("예약 상태 확인", PleosButtonStyle.Filled, ProgressAction.Kind.REFRESH)),
            ),
            toProgressUiState(order, DriveContext.PARKED, now, baseline = null),
        )
    }

    @Test fun `요청 후 59초까지는 매장 확인 중`() {
        val order = testOrder(OrderStage.AWAITING_STORE).copy(requestedAt = now.minusSeconds(59))
        assertEquals(ProgressVariant.AWAITING_STORE, toProgressUiState(order, DriveContext.PARKED, now, null).variant)
    }

    @Test fun `V03-R 예약 거절 - 매장이 남긴 사유를 보여 준다`() {
        val order = testOrder(OrderStage.CANCELLED).copy(cancelReason = CancelReason.STORE_REJECTED, cancelNote = "주문량 증가")
        assertEquals(
            ProgressUiState.Card(
                variant = ProgressVariant.REJECTED,
                eyebrow = "예약 거절",
                title = "지금은 예약을\n받기 어려워요",
                card = card,
                lead = "매장이 주문량 증가로 예약을 받지 못했어요.",
                note = "예약은 확정되지 않았어요.\n주차 상태에서 다른 매장을 선택할 수 있어요.",
                actions = listOf(
                    ProgressAction("시작으로", PleosButtonStyle.Basic, ProgressAction.Kind.FINISH),
                    ProgressAction("다른 매장 선택", PleosButtonStyle.Filled, ProgressAction.Kind.CHOOSE_OTHER_STORE),
                ),
            ),
            toProgressUiState(order, DriveContext.PARKED, now, baseline = null),
        )
    }

    @Test fun `거절 사유가 없으면 사유 없이 안내한다`() {
        val order = testOrder(OrderStage.CANCELLED).copy(cancelReason = CancelReason.STORE_REJECTED)
        val state = toProgressUiState(order, DriveContext.PARKED, now, null) as ProgressUiState.Card
        assertEquals("매장이 예약을 받지 못했어요.", state.lead)
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
                actions = detailLocked,
            ),
            toProgressUiState(order, DriveContext.DRIVING, now, baseline = timing),
        )
    }

    @Test fun `주차 확인이면 주문 상세를 열 수 있다`() {
        val order = testOrder(OrderStage.WAITING_TO_PREPARE).copy(timing = testTiming("08:08"))
        assertEquals(
            listOf(ProgressAction("주문 상세", PleosButtonStyle.Basic, ProgressAction.Kind.DETAIL)),
            toProgressUiState(order, DriveContext.PARKED, now, baseline = null).actions,
        )
    }

    @Test fun `일시 정차와 상태 불명에서는 상세 잠금`() {
        val order = testOrder(OrderStage.WAITING_TO_PREPARE).copy(timing = testTiming("08:08"))
        assertEquals(detailLocked, toProgressUiState(order, DriveContext.STOPPED, now, null).actions)
        assertEquals(detailLocked, toProgressUiState(order, DriveContext.UNKNOWN, now, null).actions)
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

    @Test fun `V04-E 연결·ETA 오류 - 마지막 ETA 수신이 90초를 넘으면 마지막 확인 시각을 보여 준다`() {
        val order = testOrder(OrderStage.WAITING_TO_PREPARE).copy(timing = testTiming("08:08", receivedAt = "08:02"))
        assertEquals(
            ProgressUiState.Card(
                variant = ProgressVariant.ETA_LOST,
                eyebrow = "연결·ETA 오류",
                title = "최신 도착 정보를\n확인하고 있어요",
                info = InfoBoxText("마지막 확인 08:02", "현재 도착 시간은 확인되지 않아요"),
                card = card,
                lead = "예약은 유지돼요.\n연결되면 최신 주문 상태를 불러와요.",
                note = null,
                actions = listOf(ProgressAction("연결 확인 중", PleosButtonStyle.Disabled, ProgressAction.Kind.NONE)),
            ),
            toProgressUiState(order, DriveContext.DRIVING, kst("08:02").plusSeconds(91), baseline = null),
        )
    }

    @Test fun `마지막 ETA 수신 후 90초까지는 도착 화면`() {
        val order = testOrder(OrderStage.PREPARING).copy(timing = testTiming("08:08", receivedAt = "08:02"))
        assertEquals(ProgressVariant.PREPARING, toProgressUiState(order, DriveContext.DRIVING, kst("08:02").plusSeconds(90), null).variant)
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
                actions = listOf(ProgressAction("주문 완료", PleosButtonStyle.Filled, ProgressAction.Kind.FINISH)),
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

    @Test fun `도착이 빨라지면 빨라진 계획을 새 기준으로 삼는다`() {
        val presenter = ProgressPresenter()
        val order = testOrder(OrderStage.WAITING_TO_PREPARE)
        // 내비 첫 값 20분 → 시연 ETA 6분(빨라짐) → 정체 +4분
        presenter.present(order.copy(timing = testTiming("08:22")), DriveContext.DRIVING, now)
        assertEquals(ProgressVariant.WAITING, presenter.present(order.copy(timing = testTiming("08:08")), DriveContext.DRIVING, now).variant)
        val delayed = presenter.present(order.copy(timing = testTiming("08:12")), DriveContext.DRIVING, now)
        assertEquals(ProgressVariant.WAITING_DELAYED, delayed.variant)
        assertEquals("08:12 도착 예정 · 기존 08:08", ((delayed as ProgressUiState.Arrival).headline as ArrivalHeadline.Eta).line)
    }

    @Test fun `조금씩 늦어지면 처음 기준과 비교해 쌓인 지연을 알린다`() {
        val presenter = ProgressPresenter()
        val order = testOrder(OrderStage.WAITING_TO_PREPARE)
        presenter.present(order.copy(timing = testTiming("08:08")), DriveContext.DRIVING, now)
        val step = testTiming("08:08").let { t ->
            t.copy(entranceEtaAt = t.entranceEtaAt.plusSeconds(40), expectedPickupAt = t.expectedPickupAt.plusSeconds(40), recommendedStartAt = t.recommendedStartAt.plusSeconds(40))
        }
        assertEquals(ProgressVariant.WAITING, presenter.present(order.copy(timing = step), DriveContext.DRIVING, now).variant)
        assertEquals(ProgressVariant.WAITING_DELAYED, presenter.present(order.copy(timing = testTiming("08:09")), DriveContext.DRIVING, now).variant)
    }

    @Test fun `주문이 바뀌면 기준을 새로 잡는다`() {
        val presenter = ProgressPresenter()
        presenter.present(testOrder(OrderStage.WAITING_TO_PREPARE, id = "a").copy(timing = testTiming("08:08")), DriveContext.DRIVING, now)
        val next = testOrder(OrderStage.WAITING_TO_PREPARE, id = "b").copy(timing = testTiming("08:12"))
        assertEquals(ProgressVariant.WAITING, presenter.present(next, DriveContext.DRIVING, now).variant)
    }

    @Test fun `V06-C 취소 확인 중`() {
        assertEquals(
            ProgressUiState.Card(
                variant = ProgressVariant.CANCEL_PENDING,
                eyebrow = "취소 확인 중",
                title = "매장에\n취소를 요청했어요",
                card = card,
                lead = "아직 취소가 완료되지 않았어요.",
                note = "매장 응답을 기다리고 있어요.\n응답이 늦으면 예약 상태를 확인해 주세요.",
                actions = listOf(ProgressAction("취소 상태 확인", PleosButtonStyle.Filled, ProgressAction.Kind.REFRESH)),
            ),
            toProgressUiState(testOrder(OrderStage.CANCEL_REQUESTED), DriveContext.PARKED, now, baseline = null),
        )
    }

    @Test fun `V06-D 취소 완료`() {
        val order = testOrder(OrderStage.CANCELLED).copy(cancelReason = CancelReason.CANCEL_APPROVED, cancelledAt = kst("08:03"))
        assertEquals(
            ProgressUiState.Card(
                variant = ProgressVariant.CANCELLED,
                eyebrow = "취소 완료",
                title = "예약이\n취소됐어요",
                card = card,
                lead = "매장에서 취소를 승인했어요.",
                note = "주문 P-021 · 취소 완료 08:03",
                actions = listOf(ProgressAction("시작으로", PleosButtonStyle.Filled, ProgressAction.Kind.FINISH)),
            ),
            toProgressUiState(order, DriveContext.PARKED, now, baseline = null),
        )
    }

    @Test fun `V06-E 취소 거절 - 제조가 시작된 주문`() {
        val order = testOrder(OrderStage.PREPARING).copy(cancelRejected = true)
        assertEquals(
            ProgressUiState.Card(
                variant = ProgressVariant.CANCEL_REJECTED,
                eyebrow = "취소 거절",
                title = "취소 요청이\n승인되지 않았어요",
                card = card,
                lead = "매장이 이미 제조를 시작했어요.",
                note = "현재 주문 상태: 제조 중\n주문을 유지하고 수령을 진행해 주세요.",
                actions = listOf(ProgressAction("현재 주문 보기", PleosButtonStyle.Filled, ProgressAction.Kind.ACK_CANCEL_REJECTION)),
            ),
            toProgressUiState(order, DriveContext.PARKED, now, baseline = null),
        )
    }

    @Test fun `취소 거절을 확인하면 원래 진행 화면으로 돌아간다`() {
        val order = testOrder(OrderStage.PREPARING).copy(cancelRejected = true, timing = testTiming("08:05"))
        assertEquals(
            ProgressVariant.PREPARING,
            toProgressUiState(order, DriveContext.DRIVING, now, baseline = null, cancelRejectionSeen = true).variant,
        )
        val presenter = ProgressPresenter()
        assertEquals(ProgressVariant.CANCEL_REJECTED, presenter.present(order, DriveContext.DRIVING, now).variant)
        presenter.acknowledgeCancelRejection(order.id)
        assertEquals(ProgressVariant.PREPARING, presenter.present(order, DriveContext.DRIVING, now).variant)
    }
}
