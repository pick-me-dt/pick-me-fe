package com.pickmedt.car.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import com.pickmedt.car.ui.theme.PickmeColors
import com.pickmedt.car.ui.theme.PickmeType
import com.pickmedt.car.ui.theme.designDp

/**
 * Pleos 공식 ivi_text_field (Figma V00-I 2:36732, 오류 V00-IE 2:36750).
 * 높이 100, 배경 #131417, 모서리 24, 좌우 32. 기본 테두리 2 흰색 10%, 오류는 테두리 3 빨강 + 도움말.
 * [onValueChange]가 null이면 입력할 수 없는 표시용이다.
 */
@Composable
fun PleosTextField(
    value: String,
    onValueChange: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    onDone: () -> Unit = {},
) {
    val shape = RoundedCornerShape(24.designDp)
    val border = if (errorText != null) {
        Modifier.border(3.designDp, PickmeColors.RegulationRed, shape)
    } else {
        Modifier.border(2.designDp, PickmeColors.DividerSecondary, shape)
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.designDp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(100.designDp)
                .background(PickmeColors.FieldBackground, shape)
                .then(border)
                .padding(horizontal = 32.designDp),
            contentAlignment = Alignment.CenterStart,
        ) {
            val textStyle = PickmeType.regular36x44.copy(color = PickmeColors.TextPrimary)
            if (onValueChange == null) {
                Text(value, style = textStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = textStyle,
                    singleLine = true,
                    cursorBrush = SolidColor(PickmeColors.TextPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onDone() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (errorText != null) {
            Text(
                errorText,
                style = PickmeType.regular26x34,
                color = PickmeColors.RegulationRed,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.designDp),
            )
        }
    }
}
