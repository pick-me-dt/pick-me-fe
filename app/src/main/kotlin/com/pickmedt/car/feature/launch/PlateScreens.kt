package com.pickmedt.car.feature.launch

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pickmedt.car.core.model.DriveContext
import com.pickmedt.car.ui.component.MessageAction
import com.pickmedt.car.ui.component.MessageScreen
import com.pickmedt.car.ui.component.MessageUiState
import com.pickmedt.car.ui.component.PleosButtonStyle.Basic
import com.pickmedt.car.ui.component.PleosButtonStyle.Filled
import com.pickmedt.car.ui.component.PleosTextField
import com.pickmedt.car.ui.component.parkedOnly

/** 차량번호 화면의 버튼. 첫 이용(V00)과 설정(V08 차량번호 변경)이 함께 쓴다 */
enum class PlateAction { BACK, SUBMIT, FIX }

/** V00-I 차량번호 등록 (Figma 2:36725) */
fun plateInputMessage(drive: DriveContext) = MessageUiState(
    eyebrow = "차량번호 등록",
    title = "수령할 차량을\n등록해 주세요",
    lead = "매장에서 주문을 찾는 데 사용해요.\n차량번호만으로 수령을 확정하지 않아요.",
    actions = listOf(
        MessageAction("이전으로", Basic, PlateAction.BACK),
        MessageAction("이 차량으로 등록", parkedOnly(Filled, drive.allowsDetail), PlateAction.SUBMIT),
    ),
)

/** V00-IE 차량번호 입력 오류 (Figma 2:36743) */
fun plateErrorMessage() = MessageUiState(
    eyebrow = "차량번호 입력 오류",
    title = "차량번호를\n다시 확인해 주세요",
    lead = "차량번호 형식이 맞지 않아요.",
    note = "예: 12가 3456\n등록증에 표시된 차량번호를 확인해 주세요.",
    actions = listOf(MessageAction("입력 수정", Filled, PlateAction.FIX)),
)

const val PLATE_ERROR_HELP = "차량번호 형식을 확인해 주세요"

@Composable
fun PlateInputScreen(
    text: String,
    drive: DriveContext,
    onTextChange: (String) -> Unit,
    onAction: (PlateAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    MessageScreen(plateInputMessage(drive), onAction, modifier) {
        PleosTextField(
            value = text,
            onValueChange = onTextChange,
            onDone = { if (drive.allowsDetail) onAction(PlateAction.SUBMIT) },
        )
    }
}

/** 오류 화면의 입력란은 표시용이다. 고치려면 「입력 수정」으로 돌아간다 */
@Composable
fun PlateErrorScreen(text: String, onAction: (PlateAction) -> Unit, modifier: Modifier = Modifier) {
    MessageScreen(plateErrorMessage(), onAction, modifier) {
        PleosTextField(value = text, onValueChange = null, errorText = PLATE_ERROR_HELP)
    }
}
