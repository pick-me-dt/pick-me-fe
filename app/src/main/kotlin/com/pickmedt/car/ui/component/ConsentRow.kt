package com.pickmedt.car.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import com.pickmedt.car.R
import com.pickmedt.car.ui.theme.PickmeColors
import com.pickmedt.car.ui.theme.PickmeType
import com.pickmedt.car.ui.theme.designDp

/**
 * 동의 항목 행 (Figma V00-C 2:36694). 760×74, 체크박스 영역 64 + 간격 20 + 라벨.
 * 체크박스는 48 상자, 모서리 12. 미선택은 테두리 4 흰색 20%, 선택은 초록 바탕 + 흰 체크 44.
 */
@Composable
fun ConsentRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(74.designDp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() }),
        horizontalArrangement = Arrangement.spacedBy(20.designDp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(64.designDp), contentAlignment = Alignment.Center) {
            val shape = RoundedCornerShape(12.designDp)
            val box = Modifier.size(48.designDp)
            if (checked) {
                Box(box.background(PickmeColors.CheckboxOn, shape), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(44.designDp))
                }
            } else {
                Box(box.border(4.designDp, PickmeColors.DividerTertiary, shape))
            }
        }
        Text(label, style = PickmeType.regular26x145, color = PickmeColors.TextPrimary)
    }
}
