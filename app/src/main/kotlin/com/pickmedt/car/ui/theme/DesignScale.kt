package com.pickmedt.car.ui.theme

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/** Figma 세로 앱 영역 폭 */
const val DESIGN_WIDTH = 840f

/** 실제 폭 ÷ Figma 폭. Figma 수치를 이 비율로 늘이거나 줄인다 */
val LocalDesignScale = staticCompositionLocalOf { 1f }

/**
 * 주어진 영역 폭으로 비율을 계산해 하위 화면에 내려준다.
 * Pleos 에뮬레이터의 화면 밀도를 확인하기 전까지는 창 폭에 맞춰 Figma와 같은 비율로 그린다.
 */
@Composable
fun ProvideDesignScale(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier) {
        CompositionLocalProvider(LocalDesignScale provides maxWidth.value / DESIGN_WIDTH) { content() }
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
