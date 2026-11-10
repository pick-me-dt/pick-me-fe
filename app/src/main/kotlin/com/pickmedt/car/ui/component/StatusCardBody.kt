package com.pickmedt.car.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pickmedt.car.ui.theme.PickmeColors
import com.pickmedt.car.ui.theme.PickmeType
import com.pickmedt.car.ui.theme.designDp

/** 주문 요약 카드 세 줄 */
data class SummaryCardText(val title: String, val menu: String, val detail: String)

/** 정보 상자 두 줄 (Figma V04-E 「도착·진행 정보」 2:36463) */
data class InfoBoxText(val title: String, val sub: String)

/**
 * 카드형 화면 본문 (Figma V03·V03-R·V03-T·V04-E·V06·V07).
 * eyebrow → 제목 → [정보 상자] → 주문 요약 카드 → 안내 → [작은 안내]
 */
@Composable
fun StatusCardBody(
    eyebrow: String,
    title: String,
    card: SummaryCardText,
    lead: String,
    note: String?,
    info: InfoBoxText? = null,
) {
    Text(eyebrow, style = PickmeType.bold26x145, color = PickmeColors.TextSecondary)
    Text(title, style = PickmeType.headline, color = PickmeColors.TextPrimary)
    if (info != null) InfoBox(info)
    OrderSummaryCard(card.title, card.menu, card.detail)
    Text(lead, style = PickmeType.regular28x145, color = PickmeColors.TextSecondary)
    if (note != null) Text(note, style = PickmeType.regular24x145, color = PickmeColors.TextSecondary)
}

/** 760×154, 배경 #131417, 모서리 20, 여백 24, 간격 10 */
@Composable
private fun InfoBox(info: InfoBoxText) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(154.designDp)
            .background(PickmeColors.BackgroundSecondary, RoundedCornerShape(20.designDp))
            .padding(24.designDp),
        verticalArrangement = Arrangement.spacedBy(10.designDp),
    ) {
        Text(info.title, style = PickmeType.bold40x145, color = PickmeColors.TextPrimary)
        Text(info.sub, style = PickmeType.regular24x145, color = PickmeColors.TextSecondary)
    }
}
