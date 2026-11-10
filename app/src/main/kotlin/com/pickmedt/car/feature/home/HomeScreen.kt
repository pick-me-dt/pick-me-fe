package com.pickmedt.car.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.pickmedt.car.R
import com.pickmedt.car.core.model.DriveContext
import com.pickmedt.car.core.model.QuickOrder
import com.pickmedt.car.ui.component.PleosButton
import com.pickmedt.car.ui.component.PleosButtonStyle
import com.pickmedt.car.ui.component.ScreenFrame
import com.pickmedt.car.ui.text.Formats
import com.pickmedt.car.ui.theme.PickmeColors
import com.pickmedt.car.ui.theme.PickmeType
import com.pickmedt.car.ui.theme.designDp

data class HomeUiState(
    val menuName: String,
    val info: String,
    /** 예약은 주차 확인일 때만 (2차 기획 §7) */
    val canReserve: Boolean,
)

fun toHomeUiState(quick: QuickOrder, drive: DriveContext) = HomeUiState(
    menuName = quick.lines.firstOrNull()?.name.orEmpty(),
    info = Formats.homeInfoLine(quick.store, quick.lines, quick.total),
    canReserve = drive.allowsDetail,
)

/** V01 시작 (Figma 노드 2:35998) */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onChangeMenu: () -> Unit,
    onReserve: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenFrame(
        modifier = modifier,
        actions = {
            PleosButton("매장·메뉴 변경", PleosButtonStyle.Basic, onChangeMenu)
            PleosButton(
                "이 메뉴로 예약",
                if (state.canReserve) PleosButtonStyle.Filled else PleosButtonStyle.Disabled,
                onReserve,
            )
        },
    ) {
        Text("가는 길에,\n평소 그 한 잔.", style = PickmeType.hero, color = PickmeColors.TextPrimary)
        Image(
            painter = painterResource(R.drawable.img_coffee_hero),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.size(760.designDp, 350.designDp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.designDp)) {
            Text(state.menuName, style = PickmeType.bold36x140, color = PickmeColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(state.info, style = PickmeType.regular26x140, color = PickmeColors.TextSecondary, maxLines = 1)
        }
    }
}
