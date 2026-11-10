package com.pickmedt.car.ui.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pickmedt.car.ui.theme.PickmeColors
import com.pickmedt.car.ui.theme.PickmeType

/** 문구형 화면의 글자 배치 */
enum class MessageTone {
    /** eyebrow → 제목(ExtraBold 48/64) → 안내(Regular 28/1.45). V00·V01-E·R·NO·V02 예외·V08 */
    Notice,

    /** 제목(Bold 48/57) → 안내(Bold 28/33). 선택 화면 V01-M·N */
    Choice,
}

/** eyebrow 크기. V01-E만 26이고 나머지는 24다 */
enum class EyebrowSize { Small, Large }

data class MessageAction<K>(val label: String, val style: PleosButtonStyle, val kind: K)

data class MessageUiState<K>(
    val eyebrow: String?,
    val title: String,
    val lead: String,
    val note: String? = null,
    val actions: List<MessageAction<K>>,
    val tone: MessageTone = MessageTone.Notice,
    val eyebrowSize: EyebrowSize = EyebrowSize.Small,
)

/**
 * 문구형 화면 (3순위 화면 대부분).
 * eyebrow → 제목 → 안내 → [extra: 체크박스·입력란] → [작은 안내], 하단에 버튼 목록
 */
@Composable
fun <K> MessageScreen(
    state: MessageUiState<K>,
    onAction: (K) -> Unit,
    modifier: Modifier = Modifier,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    ScreenFrame(
        modifier = modifier,
        actions = {
            state.actions.forEach { action -> PleosButton(action.label, action.style, { onAction(action.kind) }) }
        },
    ) {
        state.eyebrow?.let {
            val style = if (state.eyebrowSize == EyebrowSize.Large) PickmeType.bold26x145 else PickmeType.bold24x145
            Text(it, style = style, color = PickmeColors.TextSecondary)
        }
        val choice = state.tone == MessageTone.Choice
        Text(state.title, style = if (choice) PickmeType.bold48x57 else PickmeType.headline, color = PickmeColors.TextPrimary)
        Text(state.lead, style = if (choice) PickmeType.bold28x33 else PickmeType.regular28x145, color = PickmeColors.TextSecondary)
        extra()
        state.note?.let { Text(it, style = PickmeType.regular24x145, color = PickmeColors.TextSecondary) }
    }
}

/** 주차 상태일 때만 누를 수 있는 버튼 스타일 (2차 기획 §7) */
fun parkedOnly(style: PleosButtonStyle, allowed: Boolean) = if (allowed) style else PleosButtonStyle.Disabled
