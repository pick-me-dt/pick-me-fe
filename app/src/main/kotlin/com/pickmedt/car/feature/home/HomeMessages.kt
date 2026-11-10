package com.pickmedt.car.feature.home

import com.pickmedt.car.core.model.DriveContext
import com.pickmedt.car.core.model.MenuOption
import com.pickmedt.car.core.model.StoreOnRoute
import com.pickmedt.car.core.model.StoreSummary
import com.pickmedt.car.ui.component.EyebrowSize
import com.pickmedt.car.ui.component.MessageAction
import com.pickmedt.car.ui.component.MessageTone
import com.pickmedt.car.ui.component.MessageUiState
import com.pickmedt.car.ui.component.PleosButtonStyle.Basic
import com.pickmedt.car.ui.component.PleosButtonStyle.Filled
import com.pickmedt.car.ui.component.parkedOnly
import com.pickmedt.car.ui.text.Formats

/** V01 예외·선택 화면의 버튼 */
sealed interface HomeAction {
    /** V01-E 「매장·메뉴 등록」, V01-R 「경로 다시 확인」, V01-NO 「매장 다시 확인」 */
    data object FindStores : HomeAction

    data object OpenNavigation : HomeAction

    /** 「시작으로」, 「이전으로」 */
    data object Back : HomeAction

    data class PickStore(val store: StoreSummary) : HomeAction

    data class PickMenu(val menu: MenuOption) : HomeAction

    /** 메뉴가 3개보다 많을 때 「다른 메뉴 보기」 */
    data object NextMenuPage : HomeAction
}

/** 선택 목록은 버튼으로 한 번에 3개까지 보여 준다 (Figma는 2개). 메뉴가 더 많으면 「다른 메뉴 보기」로 넘긴다 */
const val MAX_CHOICES = 3

/** V01-E 등록 내역 없음 (Figma 2:36477). 「등록과 변경은 주차 상태에서」라 주행 중에는 누를 수 없다 */
fun emptyMessage(drive: DriveContext) = MessageUiState<HomeAction>(
    eyebrow = "등록 내역 없음",
    eyebrowSize = EyebrowSize.Large,
    title = "자주 가는 매장과\n메뉴를 등록해 주세요",
    lead = "출발 전에 등록해 두면 다음 예약이 간편해져요.",
    note = "등록한 매장과 메뉴로 예약할 수 있어요.\n등록과 변경은 주차 상태에서 진행해요.",
    actions = listOf(MessageAction("매장·메뉴 등록", parkedOnly(Filled, drive.allowsDetail), HomeAction.FindStores)),
)

/** V01-R 경로 없음 (Figma 2:36759) */
fun noRouteMessage() = MessageUiState<HomeAction>(
    eyebrow = "경로 없음",
    title = "이동 경로를\n먼저 설정해 주세요",
    lead = "내비게이션 경로를 기준으로\n이용할 수 있는 매장을 확인해요.",
    actions = listOf(
        MessageAction("경로 다시 확인", Basic, HomeAction.FindStores),
        MessageAction("내비게이션 열기", Filled, HomeAction.OpenNavigation),
    ),
)

/** V01-NO 이용 매장 없음 (Figma 2:36776) */
fun noStoreMessage() = MessageUiState<HomeAction>(
    eyebrow = "이용 매장 없음",
    title = "현재 경로에\n예약 가능한 매장이 없어요",
    lead = "제휴 매장과 영업 상태를 확인했어요.\n경로를 바꾸거나 나중에 다시 확인해 주세요.",
    actions = listOf(
        MessageAction("시작으로", Basic, HomeAction.Back),
        MessageAction("매장 다시 확인", Filled, HomeAction.FindStores),
    ),
)

/** V01-M 매장 선택 (Figma 2:36597). 예: "양재 DT점 · 도착 6분" */
fun pickStoreMessage(stores: List<StoreOnRoute>, drive: DriveContext) = MessageUiState(
    eyebrow = null,
    tone = MessageTone.Choice,
    title = "가는 길의 매장을\n선택해 주세요",
    lead = "현재 경로에서 이용할 수 있는 매장이에요.\n선택 후 메뉴를 확인해요.",
    actions = choices(stores, drive, ::storeChoiceLabel) { HomeAction.PickStore(it.store) },
)

/** V01-N 메뉴 선택 (Figma 2:36612). 예: "아이스 아메리카노 · 4,500원" */
fun pickMenuMessage(store: StoreSummary, menus: List<MenuOption>, drive: DriveContext, page: Int = 0) = MessageUiState(
    eyebrow = null,
    tone = MessageTone.Choice,
    title = "예약할 메뉴를\n선택해 주세요",
    lead = "${Formats.storeLine(store)}\n주차 중 등록 메뉴를 선택해요.",
    actions = choices(
        items = menus.drop(page * MAX_CHOICES),
        drive = drive,
        label = { "${it.name} · ${it.price.format()}" },
        more = if (menus.size > MAX_CHOICES) MessageAction("다른 메뉴 보기", Basic, HomeAction.NextMenuPage) else null,
    ) { HomeAction.PickMenu(it) },
)

/** 예: "양재 DT점 · 도착 6분". 남은 시간을 모르면 거리: "판교점 · 0.6km" */
fun storeChoiceLabel(store: StoreOnRoute): String {
    val detail = store.etaMinutes?.let { "도착 ${it}분" }
        ?: store.distanceM?.let { String.format(java.util.Locale.ROOT, "%.1fkm", it / 1000.0) }
    return listOfNotNull(store.store.name, detail).joinToString(" · ")
}

/** 첫 항목은 채움, 나머지는 기본, 마지막에 「이전으로」. 주행 중에는 항목을 누를 수 없다 */
private fun <T> choices(
    items: List<T>,
    drive: DriveContext,
    label: (T) -> String,
    more: MessageAction<HomeAction>? = null,
    action: (T) -> HomeAction,
): List<MessageAction<HomeAction>> =
    items.take(MAX_CHOICES).mapIndexed { index, item ->
        MessageAction(label(item), parkedOnly(if (index == 0) Filled else Basic, drive.allowsDetail), action(item))
    } + listOfNotNull(more) + MessageAction("이전으로", Basic, HomeAction.Back)
