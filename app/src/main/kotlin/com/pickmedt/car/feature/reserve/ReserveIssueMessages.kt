package com.pickmedt.car.feature.reserve

import com.pickmedt.car.core.model.DriveContext
import com.pickmedt.car.data.order.Quote
import com.pickmedt.car.data.order.QuoteIssue
import com.pickmedt.car.ui.component.MessageAction
import com.pickmedt.car.ui.component.MessageUiState
import com.pickmedt.car.ui.component.PleosButtonStyle.Basic
import com.pickmedt.car.ui.component.PleosButtonStyle.Filled
import com.pickmedt.car.ui.component.parkedOnly
import com.pickmedt.car.ui.text.Formats
import com.pickmedt.car.ui.text.Josa

/** V02 예외 화면의 버튼 */
enum class ReserveIssueAction { BACK_TO_START, CHOOSE_OTHER_MENU, CHOOSE_OTHER_STORE, SUBMIT }

/** 예약 전 재확인에서 문제가 나온 견적의 화면. 문제가 없으면(issue == null) V02 예약 확인을 쓴다 */
fun toReserveIssueMessage(quote: Quote, drive: DriveContext, submitting: Boolean): MessageUiState<ReserveIssueAction> {
    val issue = requireNotNull(quote.issue) { "문제가 없는 견적은 예약 확인 화면을 쓴다" }
    return when (issue) {
        QuoteIssue.SOLD_OUT -> soldOutMessage(quote)
        QuoteIssue.PRICE_CHANGED -> priceChangedMessage(quote, drive, submitting)
        QuoteIssue.STORE_CLOSED -> storeClosedMessage(quote)
    }
}

/** V02-SO 선택 메뉴 품절 (Figma 2:36791). 예: "아이스 아메리카노를 지금 예약할 수 없어요." */
private fun soldOutMessage(quote: Quote) = MessageUiState(
    eyebrow = "선택 메뉴 품절",
    title = "선택한 메뉴가\n품절됐어요",
    lead = "${Josa.eulReul(quote.lines.firstOrNull()?.name.orEmpty())} 지금 예약할 수 없어요.\n아직 예약은 확정되지 않았어요.",
    actions = listOf(
        MessageAction("시작으로", Basic, ReserveIssueAction.BACK_TO_START),
        MessageAction("다른 메뉴 선택", Filled, ReserveIssueAction.CHOOSE_OTHER_MENU),
    ),
)

/** V02-PC 가격 변경 재확인 (Figma 2:36806). 예: "기존 4,500원 → 현재 4,800원", "4,800원으로 예약 요청" */
private fun priceChangedMessage(quote: Quote, drive: DriveContext, submitting: Boolean): MessageUiState<ReserveIssueAction> {
    val total = quote.total.format()
    val change = listOfNotNull(quote.previousTotal?.let { "기존 ${it.format()}" }, "현재 $total").joinToString(" → ")
    return MessageUiState(
        eyebrow = "가격 변경 재확인",
        title = "메뉴 가격이\n변경됐어요",
        lead = "${Formats.menuLine(quote.lines)}\n$change",
        note = "바뀐 금액을 확인한 뒤 예약해 주세요.\n아직 예약 요청을 보내지 않았어요.",
        actions = listOf(
            MessageAction("메뉴 다시 선택", Basic, ReserveIssueAction.CHOOSE_OTHER_MENU),
            MessageAction("${Josa.euro(total)} 예약 요청", parkedOnly(Filled, drive.allowsDetail && !submitting), ReserveIssueAction.SUBMIT),
        ),
    )
}

/** V02-CL 매장 접수 종료 (Figma 2:36822). 예: "양재 DT점에서 현재 주문을 받지 않아요." */
private fun storeClosedMessage(quote: Quote) = MessageUiState(
    eyebrow = "매장 접수 종료",
    title = "매장 예약 접수가\n종료됐어요",
    lead = "${quote.store.name}에서 현재 주문을 받지 않아요.\n아직 예약은 확정되지 않았어요.",
    actions = listOf(
        MessageAction("시작으로", Basic, ReserveIssueAction.BACK_TO_START),
        MessageAction("다른 매장 선택", Filled, ReserveIssueAction.CHOOSE_OTHER_STORE),
    ),
)
