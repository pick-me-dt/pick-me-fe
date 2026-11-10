package com.pickmedt.car.feature.settings

import com.pickmedt.car.core.rule.PlateNumber
import com.pickmedt.car.ui.component.MessageAction
import com.pickmedt.car.ui.component.MessageUiState
import com.pickmedt.car.ui.component.PleosButtonStyle.Basic
import com.pickmedt.car.ui.component.PleosButtonStyle.Filled

/** V08 화면의 버튼 */
enum class SettingsAction {
    EDIT_PLATE,
    MANAGE_QUICK_ORDER,
    OPEN_LEGAL,
    CLOSE,

    /** V08-L 안내 문서 3개. Figma에 연결 대상이 없어 누르면 아무 동작 없음 */
    OPEN_DOCUMENT,
    BACK,
}

/** V08 설정·등록 정보 관리 (Figma 2:36852). 예: "차량번호 · 12가 3456". 등록 전이면 "차량번호 등록" */
fun settingsMessage(plateNumber: String?) = MessageUiState(
    eyebrow = "설정·등록 정보 관리",
    title = "설정",
    lead = "등록한 정보를 확인하고 변경해요.",
    actions = listOf(
        MessageAction(plateNumber?.let { "차량번호 · ${PlateNumber.display(it)}" } ?: "차량번호 등록", Filled, SettingsAction.EDIT_PLATE),
        MessageAction("등록 매장·메뉴 관리", Basic, SettingsAction.MANAGE_QUICK_ORDER),
        MessageAction("이용 안내·정보 처리", Basic, SettingsAction.OPEN_LEGAL),
        MessageAction("이전으로", Basic, SettingsAction.CLOSE),
    ),
)

/** V08-L 이용 안내·정보 처리 (Figma 2:36869) */
fun legalMessage() = MessageUiState(
    eyebrow = "이용 안내·정보 처리",
    title = "이용 안내",
    lead = "예약·취소 방법과\n정보 사용 내용을 확인할 수 있어요.",
    actions = listOf(
        MessageAction("서비스 이용 안내", Filled, SettingsAction.OPEN_DOCUMENT),
        MessageAction("개인정보 처리방침", Basic, SettingsAction.OPEN_DOCUMENT),
        MessageAction("위치정보 이용 안내", Basic, SettingsAction.OPEN_DOCUMENT),
        MessageAction("이전으로", Basic, SettingsAction.BACK),
    ),
)
