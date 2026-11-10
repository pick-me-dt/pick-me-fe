package com.pickmedt.car.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.pickmedt.car.ui.theme.PickmeColors
import com.pickmedt.car.ui.theme.PickmeType
import com.pickmedt.car.ui.theme.designDp

/** Pleos 공식 ivi_basic_button large 스타일 */
enum class PleosButtonStyle {
    /** 주요 행동. 밝은 배경 (Figma 노드 2:36028) */
    Filled,

    /** 보조 행동. 흰색 5% 배경 (Figma 노드 2:36027) */
    Basic,

    /** 누를 수 없음. 흰색 5% 배경 + 테두리 (Figma 노드 2:36137) */
    Disabled,
}

@Composable
fun PleosButton(
    text: String,
    style: PleosButtonStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(32.designDp)
    val background = if (style == PleosButtonStyle.Filled) PickmeColors.ButtonFilled else PickmeColors.ButtonBasic
    val textColor = when (style) {
        PleosButtonStyle.Filled -> PickmeColors.TextOnFilled
        PleosButtonStyle.Basic -> PickmeColors.TextPrimary
        PleosButtonStyle.Disabled -> PickmeColors.TextQuaternary
    }
    val border = if (style == PleosButtonStyle.Disabled) {
        Modifier.border(2.designDp, PickmeColors.DividerSecondary, shape)
    } else {
        Modifier
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(100.designDp)
            .clip(shape)
            .background(background, shape)
            .then(border)
            .clickable(enabled = style != PleosButtonStyle.Disabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 40.designDp, vertical = 16.designDp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = PickmeType.button,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
