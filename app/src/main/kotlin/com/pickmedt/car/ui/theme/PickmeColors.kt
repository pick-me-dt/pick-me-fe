package com.pickmedt.car.ui.theme

import androidx.compose.ui.graphics.Color

/** Figma 변수 기준 색 토큰 (Pleos 다크 테마) */
object PickmeColors {
    /** background/primary */
    val BackgroundPrimary = Color(0xFF1D1E21)

    /** background/secondary */
    val BackgroundSecondary = Color(0xFF131417)

    /** text/dark/primary */
    val TextPrimary = Color(0xFFFFFFFF)

    /** text/dark/secondary (흰색 84%) */
    val TextSecondary = Color(0xD6FFFFFF)

    /** text/dark/tertiary (흰색 64%) */
    val TextTertiary = Color(0xA3FFFFFF)

    /** text/dark/quaternary (흰색 32%) */
    val TextQuaternary = Color(0x52FFFFFF)

    /** text/light/primary — 밝은 버튼 위 글자 */
    val TextOnFilled = Color(0xFF131417)

    /** informative/active — 현재 단계 점 */
    val InformativeActive = Color(0xFF00E074)

    /** button/filled/enabled */
    val ButtonFilled = Color(0xFFE0E1E6)

    /** button/basic/enabled, button/filled/disabled (흰색 5%) */
    val ButtonBasic = Color(0x0DFFFFFF)

    /** divider/secondary (흰색 10%) — 비활성 버튼 테두리 */
    val DividerSecondary = Color(0x1AFFFFFF)
}
