package com.pickmedt.car.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PickmeColorScheme = darkColorScheme(
    primary = PickmeColors.ButtonFilled,
    onPrimary = PickmeColors.TextOnFilled,
    background = PickmeColors.BackgroundPrimary,
    onBackground = PickmeColors.TextPrimary,
    surface = PickmeColors.BackgroundSecondary,
    onSurface = PickmeColors.TextPrimary,
)

@Composable
fun PickmeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PickmeColorScheme, content = content)
}
