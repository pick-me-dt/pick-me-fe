package com.pickmedt.car.feature.progress

import com.pickmedt.car.core.model.CancelReason
import com.pickmedt.car.core.model.DriveContext
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.core.model.PickupTiming
import com.pickmedt.car.core.rule.EtaPolicy
import com.pickmedt.car.core.rule.isRecommendationShiftEmphasized
import com.pickmedt.car.feature.progress.ArrivalHeadline.Eta
import com.pickmedt.car.feature.progress.ArrivalHeadline.PickupNumber
import com.pickmedt.car.ui.component.PleosButtonStyle
import com.pickmedt.car.ui.text.Formats
import com.pickmedt.car.ui.text.Josa
import java.time.Duration
import java.time.Instant

/**
 * 주문마다 처음 받은 제조 타이밍을 기준으로 기억해 제조 전 지연(V05-A)을 판단한다.
 * 앱을 다시 시작하면 기준이 지금 값으로 바뀐다 (서버가 기준 시각을 주면 그 값으로 교체).
 */
class ProgressPresenter {
    private var orderId: String? = null
    private var baseline: PickupTiming? = null

    fun present(order: Order, drive: DriveContext, now: Instant): ProgressUiState {
        if (order.id != orderId) {
            orderId = order.id
            baseline = null
        }
        if (baseline == null) baseline = order.timing
        return toProgressUiState(order, drive, now, baseline)
    }
}

/** 주문·주행 맥락·현재 시각을 진행 화면 상태로 바꾼다. 문구는 Figma V03~V07 기준 */
fun toProgressUiState(order: Order, drive: DriveContext, now: Instant, baseline: PickupTiming?): ProgressUiState {
    val card = SummaryCardText(
        title = Formats.storeLine(order.store),
        menu = Formats.menuLine(order.lines),
        detail = Formats.optionPriceLine(order.lines, order.total),
    )
    return when (order.stage) {
        OrderStage.AWAITING_STORE -> ProgressUiState.Card(
            variant = ProgressVariant.AWAITING_STORE,
            eyebrow = "매장 확인 중",
            title = "매장에서\n예약을 확인 중이에요",
            card = card,
            lead = "수락 결과를 받으면 알려드릴게요.",
            note = "예약 요청을 전달했어요.\n확인 중에는 다시 주문하지 않아도 돼요.",
            action = ProgressAction("매장 응답 대기 중", PleosButtonStyle.Disabled, ProgressAction.Kind.NONE),
        )

        OrderStage.WAITING_TO_PREPARE -> {
            val delayed = isDelayedBeforePrep(baseline, order.timing)
            arrival(
                order = order,
                drive = drive,
                variant = if (delayed) ProgressVariant.WAITING_DELAYED else ProgressVariant.WAITING,
                title = if (delayed) "늦어진 도착에 맞춰\n준비할 예정이에요" else "도착에 맞춰\n준비할 예정이에요",
                step = 1,
                headline = eta(
                    timing = order.timing,
                    now = now,
                    suffix = if (delayed) " · 기존 ${Formats.clock(baseline!!.entranceEtaAt)}" else null,
                    largeLine = !delayed,
                ),
            )
        }

        OrderStage.PREPARING -> {
            val delayed = (order.timing?.delaySec ?: 0) >= EtaPolicy.EmphasizeShift.seconds
            arrival(
                order = order,
                drive = drive,
                variant = if (delayed) ProgressVariant.PREPARING_DELAYED else ProgressVariant.PREPARING,
                title = if (delayed) "이미 준비 중이에요\n도착하면 수령해 주세요" else "매장에서\n준비하고 있어요",
                step = 2,
                headline = eta(order.timing, now, suffix = if (delayed) " · 제조 진행 중" else null, largeLine = false),
            )
        }

        OrderStage.READY -> arrival(
            order = order,
            drive = drive,
            variant = ProgressVariant.READY,
            title = "주문이\n준비됐어요",
            step = 3,
            headline = PickupNumber(number = order.displayNo.orEmpty(), guide = "DT 차로에서 수령해 주세요"),
        )

        OrderStage.PICKED_UP -> ProgressUiState.Card(
            variant = ProgressVariant.PICKED_UP,
            eyebrow = "수령 완료",
            title = "주문을\n수령했어요",
            card = card,
            lead = "안전하게 이동하세요.",
            note = listOfNotNull(
                order.pickedUpAt?.let { "수령 완료 ${Formats.clock(it)}" },
                order.displayNo?.let { "주문 $it" },
            ).joinToString(" · "),
            action = ProgressAction("주문 완료", PleosButtonStyle.Filled, ProgressAction.Kind.FINISH),
        )

        OrderStage.CANCEL_REQUESTED -> ProgressUiState.Other(
            stage = order.stage,
            message = "매장에서 취소 요청을 확인하고 있어요.",
            action = ProgressAction("취소 확인 중", PleosButtonStyle.Disabled, ProgressAction.Kind.NONE),
        )

        OrderStage.CANCELLED -> ProgressUiState.Other(
            stage = order.stage,
            message = if (order.cancelReason == CancelReason.STORE_REJECTED) "매장에서 예약을 받지 못했어요." else "주문이 취소됐어요.",
            action = ProgressAction("확인", PleosButtonStyle.Filled, ProgressAction.Kind.FINISH),
        )
    }
}

/** 제조 시작 추천이 기준보다 60초 이상 늦어졌는지 (2차 기획 §5 강조 기준) */
private fun isDelayedBeforePrep(baseline: PickupTiming?, current: PickupTiming?): Boolean {
    if (baseline == null || current == null) return false
    return current.recommendedStartAt.isAfter(baseline.recommendedStartAt) &&
        isRecommendationShiftEmphasized(baseline.recommendedStartAt, current.recommendedStartAt)
}

private fun eta(timing: PickupTiming?, now: Instant, suffix: String?, largeLine: Boolean): Eta {
    if (timing == null) return Eta(minutes = "–", line = "도착 시간 확인 중", largeLine = largeLine)
    val minutes = Formats.etaMinutes(Duration.between(now, timing.entranceEtaAt).seconds)
    return Eta(
        minutes = minutes.toString(),
        line = "${Formats.clock(timing.entranceEtaAt)} 도착 예정${suffix.orEmpty()}",
        largeLine = largeLine,
    )
}

private fun arrival(
    order: Order,
    drive: DriveContext,
    variant: ProgressVariant,
    title: String,
    step: Int,
    headline: ArrivalHeadline,
) = ProgressUiState.Arrival(
    variant = variant,
    movingTo = "${Josa.euro(order.store.name)} 이동 중",
    title = title,
    headline = headline,
    step = step,
    menuLine = Formats.menuLine(order.lines),
    orderLine = Formats.orderLine(order.lines, order.displayNo),
    action = detailAction(drive),
)

/** 주문 상세는 주차 확인일 때만 연다 (2차 기획 §4) */
private fun detailAction(drive: DriveContext) =
    if (drive.allowsDetail) {
        ProgressAction("주문 상세", PleosButtonStyle.Basic, ProgressAction.Kind.DETAIL)
    } else {
        ProgressAction("주문 상세 · 주차 후", PleosButtonStyle.Disabled, ProgressAction.Kind.NONE)
    }
