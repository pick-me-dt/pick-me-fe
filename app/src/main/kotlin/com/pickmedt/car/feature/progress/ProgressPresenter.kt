package com.pickmedt.car.feature.progress

import com.pickmedt.car.core.model.CancelReason
import com.pickmedt.car.core.model.DriveContext
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.core.model.PickupTiming
import com.pickmedt.car.core.rule.EtaFreshness
import com.pickmedt.car.core.rule.EtaPolicy
import com.pickmedt.car.core.rule.etaFreshness
import com.pickmedt.car.core.rule.isRecommendationShiftEmphasized
import com.pickmedt.car.core.rule.isStoreResponseSlow
import com.pickmedt.car.feature.progress.ArrivalHeadline.Eta
import com.pickmedt.car.feature.progress.ArrivalHeadline.PickupNumber
import com.pickmedt.car.feature.progress.ProgressAction.Kind
import com.pickmedt.car.ui.component.InfoBoxText
import com.pickmedt.car.ui.component.PleosButtonStyle
import com.pickmedt.car.ui.component.SummaryCardText
import com.pickmedt.car.ui.text.Formats
import com.pickmedt.car.ui.text.Josa
import java.time.Duration
import java.time.Instant

/**
 * 진행 화면 상태를 만들면서 주문마다 두 가지를 기억한다.
 * - 제조 전 지연(V05-A) 판단 기준 타이밍: 처음 받은 값. 도착이 빨라지면 빨라진 계획으로 바꾼다
 *   (빨라진 것은 지연이 아니므로, 이후 늦어지는 만큼을 「기존」 시각과 비교해 알린다). 앱을 다시 시작하면 지금 값으로 바뀐다
 * - 취소 거절(V06-E)을 사용자가 확인했는지
 */
class ProgressPresenter {
    private var orderId: String? = null
    private var baseline: PickupTiming? = null
    private var cancelRejectionSeen = false

    fun present(order: Order, drive: DriveContext, now: Instant): ProgressUiState {
        if (order.id != orderId) {
            orderId = order.id
            baseline = null
            cancelRejectionSeen = false
        }
        val timing = order.timing
        val current = baseline
        if (current == null || (timing != null && timing.recommendedStartAt.isBefore(current.recommendedStartAt))) {
            baseline = timing
        }
        // 새 취소 요청이 들어가면 거절 확인 여부를 초기화한다
        if (order.stage == OrderStage.CANCEL_REQUESTED) cancelRejectionSeen = false
        return toProgressUiState(order, drive, now, baseline, cancelRejectionSeen)
    }

    fun acknowledgeCancelRejection(orderId: String) {
        if (orderId == this.orderId) cancelRejectionSeen = true
    }
}

/** 주문·주행 맥락·현재 시각을 진행 화면 상태로 바꾼다. 문구는 Figma V03~V07, V06-C·D·E 기준 */
fun toProgressUiState(
    order: Order,
    drive: DriveContext,
    now: Instant,
    baseline: PickupTiming?,
    cancelRejectionSeen: Boolean = false,
): ProgressUiState {
    val card = SummaryCardText(
        title = Formats.storeLine(order.store),
        menu = Formats.menuLine(order.lines),
        detail = Formats.optionPriceLine(order.lines, order.total),
    )
    if (order.cancelRejected && !cancelRejectionSeen && order.stage.isActive) return cancelRejected(order, card)

    return when (order.stage) {
        OrderStage.AWAITING_STORE ->
            if (order.acceptOverdue ?: isStoreResponseSlow(order.requestedAt, now)) storeSlow(card) else awaitingStore(card)

        OrderStage.WAITING_TO_PREPARE ->
            if (isEtaLost(order.timing, now)) etaLost(order.timing!!, card) else waiting(order, drive, now, baseline)

        OrderStage.PREPARING ->
            if (isEtaLost(order.timing, now)) etaLost(order.timing!!, card) else preparing(order, drive, now)

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
            note = joinDot(order.pickedUpAt?.let { "수령 완료 ${Formats.clock(it)}" }, order.displayNo?.let { "주문 $it" }),
            actions = listOf(ProgressAction("주문 완료", PleosButtonStyle.Filled, Kind.FINISH)),
        )

        OrderStage.CANCEL_REQUESTED -> ProgressUiState.Card(
            variant = ProgressVariant.CANCEL_PENDING,
            eyebrow = "취소 확인 중",
            title = "매장에\n취소를 요청했어요",
            card = card,
            lead = "아직 취소가 완료되지 않았어요.",
            note = "매장 응답을 기다리고 있어요.\n응답이 늦으면 예약 상태를 확인해 주세요.",
            actions = listOf(ProgressAction("취소 상태 확인", PleosButtonStyle.Filled, Kind.REFRESH)),
        )

        OrderStage.CANCELLED ->
            if (order.cancelReason == CancelReason.STORE_REJECTED) rejected(order, card) else cancelled(order, card)
    }
}

// ── 단계별 화면 ─────────────────────────────

private fun awaitingStore(card: SummaryCardText) = ProgressUiState.Card(
    variant = ProgressVariant.AWAITING_STORE,
    eyebrow = "매장 확인 중",
    title = "매장에서\n예약을 확인 중이에요",
    card = card,
    lead = "수락 결과를 받으면 알려드릴게요.",
    note = "예약 요청을 전달했어요.\n확인 중에는 다시 주문하지 않아도 돼요.",
    actions = listOf(ProgressAction("매장 응답 대기 중", PleosButtonStyle.Disabled, Kind.NONE)),
)

/** V03-T. 응답이 늦어도 실패로 처리하지 않는다 */
private fun storeSlow(card: SummaryCardText) = ProgressUiState.Card(
    variant = ProgressVariant.STORE_SLOW,
    eyebrow = "응답 지연",
    title = "매장 확인이\n조금 늦어지고 있어요",
    card = card,
    lead = "예약 결과를 확인하고 있어요.",
    note = "응답이 늦어도 실패로 처리하지 않아요.\n현재 예약 상태를 다시 확인해 주세요.",
    actions = listOf(ProgressAction("예약 상태 확인", PleosButtonStyle.Filled, Kind.REFRESH)),
)

/** V03-R */
private fun rejected(order: Order, card: SummaryCardText) = ProgressUiState.Card(
    variant = ProgressVariant.REJECTED,
    eyebrow = "예약 거절",
    title = "지금은 예약을\n받기 어려워요",
    card = card,
    lead = order.cancelNote?.let { "매장이 ${Josa.euro(it)} 예약을 받지 못했어요." } ?: "매장이 예약을 받지 못했어요.",
    note = "예약은 확정되지 않았어요.\n주차 상태에서 다른 매장을 선택할 수 있어요.",
    actions = listOf(
        ProgressAction("시작으로", PleosButtonStyle.Basic, Kind.FINISH),
        ProgressAction("다른 매장 선택", PleosButtonStyle.Filled, Kind.CHOOSE_OTHER_STORE),
    ),
)

/** V06-D */
private fun cancelled(order: Order, card: SummaryCardText) = ProgressUiState.Card(
    variant = ProgressVariant.CANCELLED,
    eyebrow = "취소 완료",
    title = "예약이\n취소됐어요",
    card = card,
    // 매장이 취소 요청을 승인한 경우만 Figma 문구. 그 밖(수락 전 취소, 매장의 주문 종료 등)은 사유 문구를 보여 준다
    lead = if (order.cancelReason == CancelReason.CANCEL_APPROVED) "매장에서 취소를 승인했어요." else order.cancelNote ?: "주문이 취소됐어요.",
    note = joinDot(order.displayNo?.let { "주문 $it" }, order.cancelledAt?.let { "취소 완료 ${Formats.clock(it)}" }),
    actions = listOf(ProgressAction("시작으로", PleosButtonStyle.Filled, Kind.FINISH)),
)

/** V06-E. 제조가 시작된 주문이면 그 사실을, 아니면 승인되지 않았다는 사실만 알린다 */
private fun cancelRejected(order: Order, card: SummaryCardText) = ProgressUiState.Card(
    variant = ProgressVariant.CANCEL_REJECTED,
    eyebrow = "취소 거절",
    title = "취소 요청이\n승인되지 않았어요",
    card = card,
    lead = if (order.stage == OrderStage.PREPARING || order.stage == OrderStage.READY) {
        "매장이 이미 제조를 시작했어요."
    } else {
        "매장이 취소 요청을 승인하지 않았어요."
    },
    note = "현재 주문 상태: ${Formats.stageLabel(order.stage)}\n주문을 유지하고 수령을 진행해 주세요.",
    actions = listOf(ProgressAction("현재 주문 보기", PleosButtonStyle.Filled, Kind.ACK_CANCEL_REJECTION)),
)

/** V04-E. 예약은 유지하고 마지막 확인 시각만 보여 준다 (2차 기획 §3 ETA 수신 중단) */
private fun etaLost(timing: PickupTiming, card: SummaryCardText) = ProgressUiState.Card(
    variant = ProgressVariant.ETA_LOST,
    eyebrow = "연결·ETA 오류",
    title = "최신 도착 정보를\n확인하고 있어요",
    info = InfoBoxText("마지막 확인 ${Formats.clock(timing.etaReceivedAt)}", "현재 도착 시간은 확인되지 않아요"),
    card = card,
    lead = "예약은 유지돼요.\n연결되면 최신 주문 상태를 불러와요.",
    note = null,
    actions = listOf(ProgressAction("연결 확인 중", PleosButtonStyle.Disabled, Kind.NONE)),
)

private fun waiting(order: Order, drive: DriveContext, now: Instant, baseline: PickupTiming?): ProgressUiState {
    val delayed = isDelayedBeforePrep(baseline, order.timing)
    return arrival(
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

private fun preparing(order: Order, drive: DriveContext, now: Instant): ProgressUiState {
    val delayed = (order.timing?.delaySec ?: 0) >= EtaPolicy.EmphasizeShift.seconds
    return arrival(
        order = order,
        drive = drive,
        variant = if (delayed) ProgressVariant.PREPARING_DELAYED else ProgressVariant.PREPARING,
        title = if (delayed) "이미 준비 중이에요\n도착하면 수령해 주세요" else "매장에서\n준비하고 있어요",
        step = 2,
        headline = eta(order.timing, now, suffix = if (delayed) " · 제조 진행 중" else null, largeLine = false),
    )
}

// ── 판단 ────────────────────────────────

/** 제조 시작 추천이 기준보다 60초 이상 늦어졌는지 (2차 기획 §5 강조 기준) */
private fun isDelayedBeforePrep(baseline: PickupTiming?, current: PickupTiming?): Boolean {
    if (baseline == null || current == null) return false
    return current.recommendedStartAt.isAfter(baseline.recommendedStartAt) &&
        isRecommendationShiftEmphasized(baseline.recommendedStartAt, current.recommendedStartAt)
}

/** 마지막 ETA 수신이 90초를 넘었는지 (2차 기획 §5) */
private fun isEtaLost(timing: PickupTiming?, now: Instant): Boolean =
    timing != null && etaFreshness(timing.etaReceivedAt, now) == EtaFreshness.STALE

// ── 공통 ────────────────────────────────

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
    actions = listOf(detailAction(drive)),
)

/** 주문 상세는 주차 확인일 때만 연다 (2차 기획 §4) */
private fun detailAction(drive: DriveContext) =
    if (drive.allowsDetail) {
        ProgressAction("주문 상세", PleosButtonStyle.Basic, Kind.DETAIL)
    } else {
        ProgressAction("주문 상세 · 주차 후", PleosButtonStyle.Disabled, Kind.NONE)
    }

private fun joinDot(vararg parts: String?): String = parts.filterNotNull().joinToString(" · ")
