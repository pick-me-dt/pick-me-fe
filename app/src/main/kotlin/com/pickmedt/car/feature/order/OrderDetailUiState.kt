package com.pickmedt.car.feature.order

import com.pickmedt.car.core.model.DriveContext
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.ui.component.PleosButtonStyle
import com.pickmedt.car.ui.component.SummaryCardText
import com.pickmedt.car.ui.text.Formats

/** 사용자가 연 상세 화면의 단계 */
enum class DetailPage {
    /** V06-A 주문 상세 */
    OVERVIEW,
    /** V06-B 취소 요청 확인 */
    CONFIRM_CANCEL,
}

enum class DetailVariant {
    OVERVIEW,
    CONFIRM_CANCEL,
    /** V06-L 주행 중 상세 제한 */
    DRIVING_LOCKED,
}

data class DetailAction(val label: String, val style: PleosButtonStyle, val kind: Kind) {
    enum class Kind {
        /** 상세를 닫고 진행 화면으로 */
        KEEP_ORDER,
        /** 취소 요청 확인(V06-B)으로 */
        ASK_CANCEL,
        /** 매장에 취소 요청을 보낸다 */
        SEND_CANCEL,
    }
}

data class OrderDetailUiState(
    val variant: DetailVariant,
    val eyebrow: String,
    val title: String,
    val card: SummaryCardText,
    val lead: String,
    val note: String,
    /** 위에서 아래 순서 */
    val actions: List<DetailAction>,
)

/**
 * 주문 상세·취소 요청 확인 (Figma V06-A·B).
 * 주차 확인이 아니면 상세 대신 제한 안내(V06-L)를 보여 준다 (2차 기획 §4, §7).
 */
fun toOrderDetailUiState(order: Order, page: DetailPage, drive: DriveContext): OrderDetailUiState {
    val card = SummaryCardText(
        title = Formats.storeLine(order.store),
        menu = Formats.menuLine(order.lines),
        detail = Formats.optionPriceLine(order.lines, order.total),
    )
    if (!drive.allowsDetail) {
        return OrderDetailUiState(
            variant = DetailVariant.DRIVING_LOCKED,
            eyebrow = "주행 중 상세 제한",
            title = "주차 후\n상세를 확인해 주세요",
            card = card,
            lead = "이동 중에는 주문 상태만 안내해요.",
            note = "${Formats.stageLabel(order.stage)} · 예약은 유지 중이에요.\n주차 후 상세 내용과 취소 요청을 확인할 수 있어요.",
            actions = listOf(DetailAction("주문 상태로", PleosButtonStyle.Filled, DetailAction.Kind.KEEP_ORDER)),
        )
    }
    return when (page) {
        DetailPage.OVERVIEW -> OrderDetailUiState(
            variant = DetailVariant.OVERVIEW,
            eyebrow = "주문 상세",
            title = "주문 상세",
            card = card,
            lead = overviewLead(order.stage),
            note = listOfNotNull(
                order.displayNo?.let { "주문 $it" },
                order.timing?.let { "매장 도착 예정 ${Formats.clock(it.entranceEtaAt)}" },
                "취소는 매장 확인 후 확정돼요.",
            ).joinToString("\n"),
            actions = listOf(
                DetailAction("주문 유지", PleosButtonStyle.Basic, DetailAction.Kind.KEEP_ORDER),
                DetailAction("취소 요청", PleosButtonStyle.Filled, DetailAction.Kind.ASK_CANCEL),
            ),
        )

        DetailPage.CONFIRM_CANCEL -> OrderDetailUiState(
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
        )
    }
}

/** Figma에는 제조 대기 문구("예약 완료 · 제조 시작 전")만 있다. 나머지 단계는 같은 형식으로 맞춘다 */
private fun overviewLead(stage: OrderStage): String = when (stage) {
    OrderStage.WAITING_TO_PREPARE -> "예약 완료 · 제조 시작 전"
    OrderStage.AWAITING_STORE -> "예약 요청 · 매장 확인 중"
    else -> "예약 완료 · ${Formats.stageLabel(stage)}"
}
