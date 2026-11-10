@file:OptIn(ExperimentalTextApi::class)

package com.pickmedt.car.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import com.pickmedt.car.R

/** Asta Sans 가변 글꼴 (42dot, OFL 1.1). Figma에서 쓰는 굵기만 등록한다 */
val AstaSans = FontFamily(
    Font(R.font.asta_sans, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.asta_sans, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.asta_sans, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)

/**
 * Figma 글자 스타일. 이름이 있는 Pleos 스타일은 그 이름을, 나머지는 「굵기 크기 행간」을 이름으로 쓴다.
 * 예: bold32x140 = Bold 32, 행간 1.4
 */
object PickmeType {
    /** Headline/Large/Extra_Strong — ExtraBold 60/80 */
    val hero @Composable @ReadOnlyComposable get() = style(FontWeight.ExtraBold, 60, 80)

    /** Headline/Small/Extra_Strong — ExtraBold 48/64 */
    val headline @Composable @ReadOnlyComposable get() = style(FontWeight.ExtraBold, 48, 64)

    /** Title/Large/Strong — Bold 40/52 */
    val appTitle @Composable @ReadOnlyComposable get() = style(FontWeight.Bold, 40, 52)

    /** Title/Small/Strong — Bold 32/40 (버튼) */
    val button @Composable @ReadOnlyComposable get() = style(FontWeight.Bold, 32, 40)

    val bold166x190 @Composable @ReadOnlyComposable get() = style(FontWeight.Bold, 166, 190)
    val bold100x130 @Composable @ReadOnlyComposable get() = style(FontWeight.Bold, 100, 130)
    val bold48x140 @Composable @ReadOnlyComposable get() = style(FontWeight.Bold, 48, 48 * 1.4f)
    val bold36x140 @Composable @ReadOnlyComposable get() = style(FontWeight.Bold, 36, 36 * 1.4f)
    val bold32x140 @Composable @ReadOnlyComposable get() = style(FontWeight.Bold, 32, 32 * 1.4f)
    val bold32x145 @Composable @ReadOnlyComposable get() = style(FontWeight.Bold, 32, 32 * 1.45f)
    val bold26x145 @Composable @ReadOnlyComposable get() = style(FontWeight.Bold, 26, 26 * 1.45f)
    val bold24x140 @Composable @ReadOnlyComposable get() = style(FontWeight.Bold, 24, 24 * 1.4f)
    val regular28x140 @Composable @ReadOnlyComposable get() = style(FontWeight.Normal, 28, 28 * 1.4f)
    val regular28x145 @Composable @ReadOnlyComposable get() = style(FontWeight.Normal, 28, 28 * 1.45f)
    val regular26x140 @Composable @ReadOnlyComposable get() = style(FontWeight.Normal, 26, 26 * 1.4f)
    val regular24x140 @Composable @ReadOnlyComposable get() = style(FontWeight.Normal, 24, 24 * 1.4f)
    val regular24x145 @Composable @ReadOnlyComposable get() = style(FontWeight.Normal, 24, 24 * 1.45f)

    @Composable @ReadOnlyComposable
    private fun style(weight: FontWeight, size: Number, lineHeight: Number) = TextStyle(
        fontFamily = AstaSans,
        fontWeight = weight,
        fontSize = size.designSp,
        lineHeight = lineHeight.designSp,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
}
