package com.pickmedt.car.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.pickmedt.car.R
import com.pickmedt.car.ui.theme.PickmeColors
import com.pickmedt.car.ui.theme.PickmeType
import com.pickmedt.car.ui.theme.designDp

/**
 * 픽미 전용 주문 요약 카드 (Figma 컴포넌트 「Pickme / 주문 요약」 2:35960).
 * 공식 에셋에 대응 카드가 없어 공식 텍스트·색상 규칙으로 구성한 카드다.
 */
@Composable
fun OrderSummaryCard(
    title: String,
    menu: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.designDp)
    Row(
        modifier
            .fillMaxWidth()
            .height(200.designDp)
            .background(PickmeColors.BackgroundSecondary, shape)
            .padding(24.designDp),
        horizontalArrangement = Arrangement.spacedBy(24.designDp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CroppedImage(
            drawable = R.drawable.img_coffee_visual_small,
            boxWidth = 130, boxHeight = 140,
            imageWidth = 316.2f, imageHeight = 148.75f,
            offsetX = -92.5f, offsetY = -2,
        )
        Column(
            Modifier.weight(1f).height(140.designDp),
            verticalArrangement = Arrangement.spacedBy(6.designDp),
        ) {
            Text(title, style = PickmeType.bold32x145, color = PickmeColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(menu, style = PickmeType.regular28x145, color = PickmeColors.TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = PickmeType.regular24x145, color = PickmeColors.TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
