package com.pickmedt.car.feature.reserve

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pickmedt.car.core.model.DriveContext
import com.pickmedt.car.data.order.Quote
import com.pickmedt.car.ui.component.OrderSummaryCard
import com.pickmedt.car.ui.component.PleosButton
import com.pickmedt.car.ui.component.PleosButtonStyle
import com.pickmedt.car.ui.component.ScreenFrame
import com.pickmedt.car.ui.text.Formats
import com.pickmedt.car.ui.theme.PickmeColors
import com.pickmedt.car.ui.theme.LocalScreenLayout
import com.pickmedt.car.ui.theme.PickmeType
import com.pickmedt.car.ui.theme.ScreenLayout

data class ReserveUiState(
    val storeLine: String,
    val menuLine: String,
    val detailLine: String,
    val total: String,
    /** 주차 확인이고 요청 중이 아닐 때만 (2차 기획 §7) */
    val canSubmit: Boolean,
)

fun toReserveUiState(quote: Quote, drive: DriveContext, submitting: Boolean) = ReserveUiState(
    storeLine = Formats.storeLine(quote.store),
    menuLine = Formats.menuLine(quote.lines),
    detailLine = Formats.optionPriceLine(quote.lines, quote.total),
    total = quote.total.format(),
    canSubmit = drive.allowsDetail && !submitting,
)

/** V02 예약 확인 (Figma 노드 2:36036) */
@Composable
fun ReserveScreen(
    state: ReserveUiState,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenFrame(
        modifier = modifier,
        actions = {
            PleosButton("이전으로", PleosButtonStyle.Basic, onBack)
            PleosButton(
                "${state.total} 예약 요청",
                if (state.canSubmit) PleosButtonStyle.Filled else PleosButtonStyle.Disabled,
                onSubmit,
            )
        },
        aside = { OrderSummaryCard(state.storeLine, state.menuLine, state.detailLine) },
    ) {
        Text("예약 확인", style = PickmeType.bold26x145, color = PickmeColors.TextSecondary)
        Text("주문 내용을\n확인해 주세요", style = PickmeType.headline, color = PickmeColors.TextPrimary)
        if (LocalScreenLayout.current == ScreenLayout.Portrait) OrderSummaryCard(state.storeLine, state.menuLine, state.detailLine)
        Text("기존 DT 차로에서 수령해요. 앞차에 따라 대기할 수 있어요.", style = PickmeType.regular28x145, color = PickmeColors.TextSecondary)
        Text("결제 예정 ${state.total}\n매장이 수락하면 예약이 완료돼요.", style = PickmeType.regular24x145, color = PickmeColors.TextSecondary)
    }
}
