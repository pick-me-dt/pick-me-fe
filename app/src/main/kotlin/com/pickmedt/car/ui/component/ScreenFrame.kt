package com.pickmedt.car.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pickmedt.car.ui.theme.PickmeColors
import com.pickmedt.car.ui.theme.designDp

/**
 * Figma 앱 영역(840×1184) 배치.
 * 제목 줄은 위 32에 고정, 본문은 위 136·좌우 40에서 세로 간격 20으로 쌓고, 하단 버튼은 아래 40에 고정한다.
 */
@Composable
fun ScreenFrame(
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit = {},
    body: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize().background(PickmeColors.BackgroundPrimary)) {
        PickmeTitleBar(Modifier.padding(top = 32.designDp))
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = 136.designDp, start = 40.designDp, end = 40.designDp, bottom = 40.designDp),
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.designDp),
                content = body,
            )
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.designDp),
                content = actions,
            )
        }
    }
}
