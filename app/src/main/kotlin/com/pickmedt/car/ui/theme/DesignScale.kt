package com.pickmedt.car.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/** Figma 세로 앱 영역 폭 */
const val DESIGN_WIDTH = 840f

/** Figma 세로 앱 영역 높이 */
const val DESIGN_HEIGHT = 1184f

/** 실제 크기 ÷ Figma 크기. Figma 수치를 이 비율로 늘이거나 줄인다 */
val LocalDesignScale = staticCompositionLocalOf { 1f }

/** 폭·높이 모두 들어가는 가장 큰 비율 */
fun designScaleFor(width: Dp, height: Dp): Float = minOf(width.value / DESIGN_WIDTH, height.value / DESIGN_HEIGHT)

/**
 * 주어진 영역에 Figma 앱 영역(840×1184)을 비율 그대로 가장 크게 넣고 가운데 둔다.
 * Pleos Connect 오른쪽 작업 영역(가로 1682×1210)처럼 Figma와 비율이 다른 창에서도 화면이 잘리지 않는다.
 */
@Composable
fun ProvideDesignScale(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val scale = designScaleFor(maxWidth, maxHeight)
        CompositionLocalProvider(LocalDesignScale provides scale) {
            Box(Modifier.size((DESIGN_WIDTH * scale).dp, (DESIGN_HEIGHT * scale).dp)) { content() }
        }
    }
}

/** Figma 수치(px) → Dp */
val Number.designDp: Dp
    @Composable @ReadOnlyComposable
    get() = (toFloat() * LocalDesignScale.current).dp

/** Figma 글자 크기(px) → TextUnit. 기기 글꼴 배율과 상관없이 Figma 비율을 지킨다 */
val Number.designSp: TextUnit
    @Composable @ReadOnlyComposable
    get() = with(LocalDensity.current) { (toFloat() * LocalDesignScale.current).dp.toSp() }
