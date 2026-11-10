package com.pickmedt.car.feature.launch

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.pickmedt.car.R
import com.pickmedt.car.core.model.DriveContext
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.ui.component.ConsentRow
import com.pickmedt.car.ui.component.MessageAction
import com.pickmedt.car.ui.component.MessageScreen
import com.pickmedt.car.ui.component.MessageUiState
import com.pickmedt.car.ui.component.PleosButtonStyle.Basic
import com.pickmedt.car.ui.component.PleosButtonStyle.Disabled
import com.pickmedt.car.ui.component.PleosButtonStyle.Filled
import com.pickmedt.car.ui.component.parkedOnly
import com.pickmedt.car.ui.text.Formats
import com.pickmedt.car.ui.theme.PickmeColors
import com.pickmedt.car.ui.theme.PickmeType
import com.pickmedt.car.ui.theme.designDp

/** V00 화면의 버튼 */
enum class LaunchAction { NONE, RETRY, FINISH, START, AGREE, DECLINE, RECHECK_ACCESS, LATER }

/** V00-L 초기 상태 확인 (Figma 2:36636) */
fun loadingMessage() = MessageUiState(
    eyebrow = "초기 상태 확인",
    title = "주문 정보를\n불러오고 있어요",
    lead = "진행 중인 주문과 등록 정보를 확인해요.",
    actions = listOf(MessageAction("불러오는 중", Disabled, LaunchAction.NONE)),
)

/** V00-E 앱 실행 실패 (Figma 2:36657) */
fun failedMessage() = MessageUiState(
    eyebrow = "앱 실행 실패",
    title = "정보를\n불러오지 못했어요",
    lead = "연결 상태를 확인한 뒤 다시 시도해 주세요.\n이미 요청한 주문이 있다면 새로 예약하지 마세요.",
    actions = listOf(
        MessageAction("나중에 확인", Basic, LaunchAction.FINISH),
        MessageAction("다시 시도", Filled, LaunchAction.RETRY),
    ),
)

/** V00-W 첫 이용 안내 (Figma 2:36672). 「첫 이용 설정은 주차 상태에서」라 주행 중에는 시작할 수 없다 */
fun welcomeMessage(drive: DriveContext) = MessageUiState(
    eyebrow = "첫 이용 안내",
    title = "도착에 맞춰\n주문을 준비해요",
    lead = "출발 전에 매장과 메뉴를 예약해요.\n이동 중에는 주문 상태를 안내해요.",
    note = "첫 이용 설정은 주차 상태에서 진행해 주세요.",
    actions = listOf(MessageAction("시작하기", parkedOnly(Filled, drive.allowsDetail), LaunchAction.START)),
)

/**
 * V00-C 정보 이용 안내·동의 (Figma 2:36687).
 * Figma는 미선택 상태만 있다. 두 항목을 모두 고르면 주요 버튼을 「동의하고 계속」으로 옮긴다.
 */
fun consentMessage(step: LaunchStep.Consent, drive: DriveContext) = MessageUiState(
    eyebrow = "정보 이용 안내·동의",
    title = "예약에 필요한\n정보를 확인해 주세요",
    lead = "도착 예상 시간과 위치: 준비 시점 계산\n차량번호: 매장에서 주문 조회",
    note = "상세 내용과 이용 기간을 확인할 수 있어요.",
    actions = listOf(
        MessageAction("동의하지 않음", if (step.canAgree) Basic else Filled, LaunchAction.DECLINE),
        MessageAction(
            "동의하고 계속",
            if (step.canAgree) parkedOnly(Filled, drive.allowsDetail) else Disabled,
            LaunchAction.AGREE,
        ),
    ),
)

/** V00-P 정보 접근 불가 (Figma 2:36709) */
fun accessBlockedMessage() = MessageUiState(
    eyebrow = "정보 접근 불가",
    title = "도착 정보를\n확인할 수 없어요",
    lead = "도착에 맞춘 준비를 위해\n위치·내비 정보 접근 상태를 확인해 주세요.",
    note = "권한을 허용한 뒤 다시 시도할 수 있어요.\n진행 중인 주문은 유지돼요.",
    actions = listOf(
        MessageAction("나중에 설정", Basic, LaunchAction.LATER),
        MessageAction("접근 상태 확인", Filled, LaunchAction.RECHECK_ACCESS),
    ),
)

/** V00-RE 진행 주문 복귀 (Figma 2:36837). 예: "양재 DT점 · 아이스 아메리카노 1잔⏎현재 상태: 제조 대기" */
fun resumeOrderMessage(order: Order) = MessageUiState(
    eyebrow = "진행 주문 복귀",
    title = "진행 중인 주문이\n있어요",
    lead = "${order.store.name} · ${Formats.menuLine(order.lines)}\n현재 상태: ${Formats.stageLabel(order.stage)}",
    note = "최근 상태를 다시 확인했어요.\n기존 주문을 이어서 확인해 주세요.",
    actions = listOf(MessageAction("진행 중 주문 보기", Filled, LaunchAction.FINISH)),
)

/** V00-S 스플래시 (Figma 2:36627). 제목 바 없이 가운데에 144 아이콘 + "pickme" */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .background(PickmeColors.BackgroundPrimary)
            .padding(48.designDp),
        verticalArrangement = Arrangement.spacedBy(24.designDp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(R.drawable.ic_pickme_splash), contentDescription = null, modifier = Modifier.size(144.designDp))
        Text("pickme", style = PickmeType.bold40x145, color = PickmeColors.TextPrimary, textAlign = TextAlign.Center)
    }
}

/** V00-C. 문구형 화면 + 동의 항목 두 줄 */
@Composable
fun ConsentScreen(
    step: LaunchStep.Consent,
    drive: DriveContext,
    onToggle: (ConsentItem) -> Unit,
    onAction: (LaunchAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    MessageScreen(consentMessage(step, drive), onAction, modifier) {
        ConsentRow("개인정보 수집·이용 동의", step.personalInfo, { onToggle(ConsentItem.PERSONAL_INFO) })
        ConsentRow("위치정보 이용 동의", step.location, { onToggle(ConsentItem.LOCATION) })
    }
}
