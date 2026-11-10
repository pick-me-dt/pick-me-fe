package com.pickmedt.car.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pickmedt.car.ui.theme.LocalScreenLayout
import com.pickmedt.car.ui.theme.PickmeColors
import com.pickmedt.car.ui.theme.ScreenLayout
import com.pickmedt.car.ui.theme.designDp

/**
 * Figma 앱 영역(840×1184) 배치.
 * 제목 줄은 위 32에 고정, 본문은 위 136·좌우 40에서 세로 간격 20으로 쌓고, 하단 버튼은 아래 40에 고정한다.
 *
 * 가로 배치(Pleos 가로 앱 영역 1700×1184)는 세로 화면 두 칸(840 + 간격 20 + 840)을 나란히 놓는다.
 * 왼쪽 칸은 세로 화면과 같이 제목 줄·본문·버튼, 오른쪽 칸에는 [aside](그림·주문 카드)를 둔다.
 * 버튼을 왼쪽 칸에 두는 이유: Pleos 가로 배치는 화면 왼쪽에 운전자 보조 영역이 있고 그 옆에 앱이 놓인다.
 * 왼쪽 칸이 운전석에 가까워, 넓은 화면 반대편까지 손을 뻗지 않고 누를 수 있다.
 * [aside]는 가로 배치에서만 그린다. 세로 배치에서는 화면이 본문 안에 직접 그린다.
 */
@Composable
fun ScreenFrame(
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit = {},
    aside: (@Composable ColumnScope.() -> Unit)? = null,
    body: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize().background(PickmeColors.BackgroundPrimary)) {
        PickmeTitleBar(Modifier.padding(top = 32.designDp))
        if (LocalScreenLayout.current == ScreenLayout.Landscape) {
            Row(Modifier.fillMaxSize().padding(top = 136.designDp, start = 40.designDp, end = 40.designDp, bottom = 40.designDp)) {
                Column(Modifier.width(760.designDp).fillMaxHeight()) {
                    Column(
                        Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(20.designDp),
                        content = body,
                    )
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.designDp), content = actions)
                }
                // 왼쪽 칸 오른쪽 여백 40 + 칸 사이 20 + 오른쪽 칸 왼쪽 여백 40
                Box(Modifier.width(100.designDp))
                Column(
                    Modifier.width(760.designDp).fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = aside ?: {},
                )
            }
            return@Box
        }
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
