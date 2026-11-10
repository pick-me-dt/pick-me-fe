package com.pickmedt.car.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Figma V01·V04-A 캡처 기준 색. 디자인 토큰 정리는 화면 구현 계획에서 한다 */
private val PickmeColors = darkColorScheme(
    primary = Color(0xFFFF7A3D),
    background = Color(0xFF1C1D21),
    surface = Color(0xFF26272B),
    onBackground = Color(0xFFF5F5F7),
    onSurface = Color(0xFFF5F5F7),
)

@Composable
fun PickmeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PickmeColors, content = content)
}
