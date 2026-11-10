package com.pickmedt.car.feature.progress

import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextOverflow
import com.pickmedt.car.R
import com.pickmedt.car.feature.progress.ArrivalHeadline.Eta
import com.pickmedt.car.feature.progress.ArrivalHeadline.PickupNumber
import com.pickmedt.car.ui.component.CroppedImage
import com.pickmedt.car.ui.component.PleosButton
import com.pickmedt.car.ui.component.ScreenFrame
import com.pickmedt.car.ui.component.StatusCardAside
import com.pickmedt.car.ui.component.StatusCardBody
import com.pickmedt.car.ui.component.StepIndicator
import com.pickmedt.car.ui.theme.PickmeColors
import com.pickmedt.car.ui.theme.LocalScreenLayout
import com.pickmedt.car.ui.theme.PickmeType
import com.pickmedt.car.ui.theme.ScreenLayout
import com.pickmedt.car.ui.theme.designDp
import com.pickmedt.car.ui.theme.designSp

/** 주문 단계에 따라 바뀌는 진행 화면 (V03·V04·V05·V07, V06-C·D·E) */
@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onAction: (ProgressAction.Kind) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenFrame(
        modifier = modifier,
        actions = {
            state.actions.forEach { action ->
                PleosButton(action.label, action.style, onClick = { onAction(action.kind) })
            }
        },
        // 가로 배치: 도착 화면은 커피 그림을, 카드 화면은 주문 카드를 오른쪽 칸에
        aside = if (state is ProgressUiState.Arrival) {
            {
                Image(
                    painter = painterResource(R.drawable.img_coffee_visual),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.size(632.designDp, 297.designDp),
                )
            }
        } else {
            { StatusCardAside((state as ProgressUiState.Card).card) }
        },
    ) {
        when (state) {
            is ProgressUiState.Card -> StatusCardBody(
                eyebrow = state.eyebrow,
                title = state.title,
                card = state.card,
                lead = state.lead,
                note = state.note,
                info = state.info,
            )

            is ProgressUiState.Arrival -> ArrivalBody(state)
        }
    }
}

/** V04·V05 (Figma 노드 2:36089) */
@Composable
private fun ArrivalBody(state: ProgressUiState.Arrival) {
    Text(state.movingTo, style = PickmeType.regular28x140, color = PickmeColors.TextSecondary)
    Text(state.title, style = PickmeType.bold48x140, color = PickmeColors.TextPrimary)
    Row(
        Modifier.fillMaxWidth().height(280.designDp),
        horizontalArrangement = Arrangement.spacedBy(22.designDp),
    ) {
        Column(Modifier.width(410.designDp).height(280.designDp).clipToBounds()) {
            when (val headline = state.headline) {
                is Eta -> {
                    Text("매장 도착까지", style = PickmeType.regular26x140, color = PickmeColors.TextSecondary)
                    Row(Modifier.height(200.designDp), horizontalArrangement = Arrangement.spacedBy(12.designDp)) {
                        Text(headline.minutes, style = PickmeType.bold166x190, color = PickmeColors.TextPrimary, modifier = Modifier.alignByBaseline())
                        Text("분", style = PickmeType.bold48x140, color = PickmeColors.TextSecondary, modifier = Modifier.alignByBaseline())
                    }
                    Text(
                        headline.line,
                        style = if (headline.largeLine) PickmeType.regular28x140 else PickmeType.regular24x140,
                        color = PickmeColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                is PickupNumber -> {
                    Text("픽업 주문번호", style = PickmeType.regular26x140, color = PickmeColors.TextSecondary)
                    Row(Modifier.height(200.designDp)) {
                        // 서버 주문번호가 길면(예: "MOCK-CDB5BE39") 한 줄 안에서 글자를 줄인다. Figma 예시 "P-021"은 그대로 100
                        BasicText(
                            text = headline.number,
                            style = PickmeType.bold100x130.copy(color = PickmeColors.TextPrimary),
                            maxLines = 1,
                            autoSize = TextAutoSize.StepBased(minFontSize = 40.designSp, maxFontSize = 100.designSp),
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                    Text(headline.guide, style = PickmeType.regular24x140, color = PickmeColors.TextSecondary)
                }
            }
        }
        if (LocalScreenLayout.current == ScreenLayout.Portrait) {
            CroppedImage(
                drawable = R.drawable.img_coffee_visual,
                boxWidth = 260, boxHeight = 280,
                imageWidth = 632.4f, imageHeight = 297.5f,
                offsetX = -185, offsetY = -4,
            )
        }
    }
    StepIndicator(current = state.step)
    Column(verticalArrangement = Arrangement.spacedBy(10.designDp)) {
        Text(state.menuLine, style = PickmeType.bold32x140, color = PickmeColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(state.orderLine, style = PickmeType.regular26x140, color = PickmeColors.TextSecondary, maxLines = 1)
    }
}
