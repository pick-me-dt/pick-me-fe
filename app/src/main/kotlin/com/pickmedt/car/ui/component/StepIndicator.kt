package com.pickmedt.car.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pickmedt.car.ui.theme.PickmeColors
import com.pickmedt.car.ui.theme.PickmeType
import com.pickmedt.car.ui.theme.designDp

/** 주문 준비 단계 라벨 (Figma 노드 2:36120) */
val OrderSteps = listOf("예약 완료", "제조 대기", "제조 중", "준비 완료")

/**
 * 읽기 전용 단계 표시. 현재 단계만 배경·초록 점·굵은 흰 글자로 강조한다.
 * Figma V04-A·V05-A 규칙을 따른다 (V04-B·V04-C·V05-B는 굵기 표기가 달라 디자이너 확인 필요).
 */
@Composable
fun StepIndicator(current: Int, modifier: Modifier = Modifier, labels: List<String> = OrderSteps) {
    Row(
        modifier.fillMaxWidth().height(130.designDp),
        horizontalArrangement = Arrangement.spacedBy(16.designDp),
    ) {
        labels.forEachIndexed { index, label ->
            val active = index == current
            val background = if (active) {
                Modifier.background(PickmeColors.BackgroundSecondary, RoundedCornerShape(20.designDp))
            } else {
                Modifier
            }
            Column(
                Modifier
                    .width(174.designDp)
                    .height(116.designDp)
                    .then(background)
                    .padding(16.designDp),
                verticalArrangement = Arrangement.spacedBy(12.designDp),
            ) {
                Box(
                    Modifier
                        .size(12.designDp)
                        .background(if (active) PickmeColors.InformativeActive else PickmeColors.TextTertiary, CircleShape),
                )
                Text(
                    text = label,
                    style = if (active) PickmeType.bold24x140 else PickmeType.regular24x140,
                    color = if (active) PickmeColors.TextPrimary else PickmeColors.TextTertiary,
                )
            }
        }
    }
}
