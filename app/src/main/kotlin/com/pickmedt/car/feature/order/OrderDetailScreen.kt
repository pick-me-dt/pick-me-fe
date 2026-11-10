package com.pickmedt.car.feature.order

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pickmedt.car.ui.component.PleosButton
import com.pickmedt.car.ui.component.ScreenFrame
import com.pickmedt.car.ui.component.StatusCardAside
import com.pickmedt.car.ui.component.StatusCardBody

/** 주문 상세 V06-A, 취소 요청 확인 V06-B, 주행 중 상세 제한 V06-L */
@Composable
fun OrderDetailScreen(
    state: OrderDetailUiState,
    onAction: (DetailAction.Kind) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenFrame(
        modifier = modifier,
        actions = {
            state.actions.forEach { action ->
                PleosButton(action.label, action.style, onClick = { onAction(action.kind) })
            }
        },
        aside = { StatusCardAside(state.card) },
    ) {
        StatusCardBody(
            eyebrow = state.eyebrow,
            title = state.title,
            card = state.card,
            lead = state.lead,
            note = state.note,
        )
    }
}
