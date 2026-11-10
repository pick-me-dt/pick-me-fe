package com.pickmedt.car.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickmedt.car.core.model.DriveContext
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.core.rule.resolveDriveContext
import com.pickmedt.car.data.order.OrderRepository
import com.pickmedt.car.data.settings.UserSettingsRepository
import com.pickmedt.car.data.remote.RemoteDevTools
import com.pickmedt.car.demo.DemoAction
import com.pickmedt.car.demo.DemoPanel
import com.pickmedt.car.feature.home.HomeAction
import com.pickmedt.car.feature.home.HomeController
import com.pickmedt.car.feature.home.HomeScreen
import com.pickmedt.car.feature.home.HomeStep
import com.pickmedt.car.feature.home.emptyMessage
import com.pickmedt.car.feature.home.noRouteMessage
import com.pickmedt.car.feature.home.noStoreMessage
import com.pickmedt.car.feature.home.pickMenuMessage
import com.pickmedt.car.feature.home.pickStoreMessage
import com.pickmedt.car.feature.home.toHomeUiState
import com.pickmedt.car.feature.launch.ConsentScreen
import com.pickmedt.car.feature.launch.LaunchAction
import com.pickmedt.car.feature.launch.LaunchController
import com.pickmedt.car.feature.launch.LaunchStep
import com.pickmedt.car.feature.launch.PlateAction
import com.pickmedt.car.feature.launch.PlateErrorScreen
import com.pickmedt.car.feature.launch.PlateInputScreen
import com.pickmedt.car.feature.launch.SplashScreen
import com.pickmedt.car.feature.launch.accessBlockedMessage
import com.pickmedt.car.feature.launch.failedMessage
import com.pickmedt.car.feature.launch.loadingMessage
import com.pickmedt.car.feature.launch.resumeOrderMessage
import com.pickmedt.car.feature.launch.welcomeMessage
import com.pickmedt.car.feature.order.DetailAction
import com.pickmedt.car.feature.order.DetailPage
import com.pickmedt.car.feature.order.OrderDetailScreen
import com.pickmedt.car.feature.order.toOrderDetailUiState
import com.pickmedt.car.feature.progress.ProgressAction
import com.pickmedt.car.feature.progress.ProgressPresenter
import com.pickmedt.car.feature.progress.ProgressScreen
import com.pickmedt.car.feature.reserve.ReserveIssueAction
import com.pickmedt.car.feature.reserve.ReserveScreen
import com.pickmedt.car.feature.reserve.toReserveIssueMessage
import com.pickmedt.car.feature.reserve.toReserveUiState
import com.pickmedt.car.feature.settings.SettingsAction
import com.pickmedt.car.feature.settings.SettingsController
import com.pickmedt.car.feature.settings.SettingsStep
import com.pickmedt.car.feature.settings.legalMessage
import com.pickmedt.car.feature.settings.settingsMessage
import com.pickmedt.car.ui.component.MessageScreen
import com.pickmedt.car.ui.theme.DESIGN_HEIGHT
import com.pickmedt.car.ui.theme.DESIGN_WIDTH
import com.pickmedt.car.ui.theme.PickmeColors
import com.pickmedt.car.ui.theme.PickmeTheme
import com.pickmedt.car.ui.theme.ProvideDesignScale
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

/** 스플래시(V00-S)를 보여 주는 시간 */
private const val SPLASH_MILLIS = 1_000L

/**
 * 최상위 화면.
 * 시작 흐름(V00)이 끝나기 전에는 시작 화면, 설정을 열면 설정(V08),
 * 그 밖에는 현재 주문이 없으면 홈 흐름(V01·V02), 있으면 진행 화면(V03~V07). 아래에는 시연 컨트롤을 붙인다.
 */
@Composable
fun AppRoot(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val launch = remember { LaunchController(container.userSettings, container.orderRepository) }
    val home = remember { HomeController(container.orderRepository, container.catalog, container.routeSource) }
    val settings = remember { SettingsController(container.userSettings) }
    var settingsOpen by remember { mutableStateOf(false) }
    // 진행 화면에서 「다른 매장 선택」으로 홈에 돌아올 때 바로 매장 선택(V01-M)을 연다
    val homeEntry = remember { HomeEntry() }

    val launchStep by launch.step.collectAsStateWithLifecycle()
    val order by container.orderRepository.currentOrder.collectAsStateWithLifecycle()
    val signals by container.driveSignalSource.signals.collectAsStateWithLifecycle()
    val drive = resolveDriveContext(signals)
    val now by produceState(container.clock.now()) {
        while (true) {
            delay(1_000)
            value = container.clock.now()
        }
    }

    LaunchedEffect(launchStep) {
        if (launchStep == LaunchStep.Splash) {
            delay(SPLASH_MILLIS)
            // load()가 단계를 바꾸면 이 효과는 취소된다. 조회가 같이 취소되지 않게 AppRoot 범위에서 실행한다
            scope.launch { launch.load() }
        }
    }
    val homeVisible = launchStep == LaunchStep.Done && order == null
    LaunchedEffect(homeVisible) {
        if (!homeVisible) return@LaunchedEffect
        home.load()
        if (homeEntry.findStores) {
            homeEntry.findStores = false
            home.changeStoreAndMenu()
        }
    }

    val appContent: @Composable () -> Unit = {
        val current = order
        when {
            launchStep != LaunchStep.Done -> LaunchRoute(launch, launchStep, drive)
            settingsOpen -> SettingsRoute(
                settings = settings,
                userSettings = container.userSettings,
                drive = drive,
                onClose = { settingsOpen = false },
                onManageQuickOrder = {
                    settingsOpen = false
                    scope.launch { home.changeStoreAndMenu() }
                },
            )
            current == null -> HomeRoute(home, drive)
            else -> ProgressRoute(
                order = current,
                drive = drive,
                now = now,
                orders = container.orderRepository,
                onChooseOtherStore = {
                    homeEntry.findStores = true
                    container.orderRepository.clearFinished()
                },
            )
        }
    }
    val demoPanel: @Composable (Modifier) -> Unit = { modifier ->
        DemoPanel(
            controller = container.demo,
            onRestartApp = {
                settingsOpen = false
                launch.restart()
            },
            onOpenSettings = {
                settings.open()
                settingsOpen = true
            },
            onReloadHome = { scope.launch { home.load() } },
            modifier = modifier,
            serverActions = container.serverDev?.let { dev ->
                serverDemoActions(dev, container.orderRepository, scope, onReloadHome = { home.load() })
            },
        )
    }

    PickmeTheme {
        BoxWithConstraints(Modifier.fillMaxSize().background(PickmeColors.BackgroundPrimary)) {
            // 가로 창(Pleos 오른쪽 작업 영역)은 Figma 비율의 앱 영역을 왼쪽에 두고 남는 폭에 시연 컨트롤을 둔다.
            // 세로 창은 Figma 프로토타입처럼 앱 영역 아래에 둔다
            val appWidth = maxHeight * (DESIGN_WIDTH / DESIGN_HEIGHT)
            if (maxWidth - appWidth >= SIDE_PANEL_MIN_WIDTH) {
                Row(Modifier.fillMaxSize()) {
                    ProvideDesignScale(Modifier.width(appWidth).fillMaxHeight(), appContent)
                    demoPanel(Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    ProvideDesignScale(Modifier.weight(1f).fillMaxWidth(), appContent)
                    demoPanel(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/** 시연 컨트롤을 앱 영역 옆에 둘 최소 폭 */
private val SIDE_PANEL_MIN_WIDTH = 360.dp

/**
 * 실서버 시연 버튼. 1차 기획 서버에는 직원 수락이 없어 「다음 단계」로 RESERVED → SENT(매장 전송)를 넘긴다.
 * 제조 시작·준비 완료·수령은 매장 대시보드(서버 /dashboard/)에서 누른다. 결과는 logcat(태그 PickmeRemote)
 */
private fun serverDemoActions(
    dev: RemoteDevTools,
    orders: OrderRepository,
    scope: CoroutineScope,
    onReloadHome: suspend () -> Unit,
): List<DemoAction> {
    fun launch(label: String, block: suspend () -> Unit): Result<Unit> {
        scope.launch {
            runCatching { block() }.onFailure { Log.w("PickmeRemote", "$label 실패: ${it.message}") }
        }
        return Result.success(Unit)
    }
    return listOf(
        "다음 단계" to {
            val orderId = orders.currentOrder.value?.id
            if (orderId == null) {
                Result.failure(IllegalStateException("진행 중 주문이 없어요"))
            } else {
                launch("다음 단계") {
                    dev.advance(orderId)
                    orders.refresh()
                }
            }
        },
        "새로고침" to { launch("새로고침") { orders.refresh() } },
        "서버 초기화" to {
            launch("서버 초기화") {
                dev.reset()
                orders.refresh()
                orders.clearFinished()
                onReloadHome()
            }
        },
    )
}

private class HomeEntry {
    var findStores = false
}

/** V00 시작·첫 이용 */
@Composable
private fun LaunchRoute(launch: LaunchController, step: LaunchStep, drive: DriveContext) {
    val scope = rememberCoroutineScope()
    val onAction: (LaunchAction) -> Unit = { kind ->
        when (kind) {
            LaunchAction.NONE -> Unit
            LaunchAction.RETRY -> scope.launch { launch.load() }
            LaunchAction.FINISH -> launch.finish()
            LaunchAction.START -> launch.start()
            LaunchAction.AGREE -> launch.agree()
            LaunchAction.DECLINE -> launch.decline()
            LaunchAction.RECHECK_ACCESS -> launch.recheckAccess()
            LaunchAction.LATER -> launch.later()
        }
    }
    val onPlate: (PlateAction) -> Unit = { kind ->
        when (kind) {
            PlateAction.BACK -> launch.backFromPlate()
            PlateAction.SUBMIT -> launch.submitPlate()
            PlateAction.FIX -> launch.fixPlate()
        }
    }
    when (step) {
        LaunchStep.Splash -> SplashScreen()
        LaunchStep.Loading -> MessageScreen(loadingMessage(), onAction)
        LaunchStep.Failed -> MessageScreen(failedMessage(), onAction)
        LaunchStep.Welcome -> MessageScreen(welcomeMessage(drive), onAction)
        is LaunchStep.Consent -> ConsentScreen(step, drive, launch::toggleConsent, onAction)
        LaunchStep.AccessBlocked -> MessageScreen(accessBlockedMessage(), onAction)
        is LaunchStep.PlateInput -> PlateInputScreen(step.text, drive, launch::plateChanged, onPlate)
        is LaunchStep.PlateError -> PlateErrorScreen(step.text, onPlate)
        is LaunchStep.ResumeOrder -> MessageScreen(resumeOrderMessage(step.order), onAction)
        LaunchStep.Done -> Unit
    }
}

/** V08 설정. 차량번호 변경은 V00-I·IE 화면을 다시 쓴다 */
@Composable
private fun SettingsRoute(
    settings: SettingsController,
    userSettings: UserSettingsRepository,
    drive: DriveContext,
    onClose: () -> Unit,
    onManageQuickOrder: () -> Unit,
) {
    val step by settings.step.collectAsStateWithLifecycle()
    val saved by userSettings.settings.collectAsStateWithLifecycle()
    val onAction: (SettingsAction) -> Unit = { kind ->
        when (kind) {
            SettingsAction.EDIT_PLATE -> settings.editPlate()
            SettingsAction.MANAGE_QUICK_ORDER -> onManageQuickOrder()
            SettingsAction.OPEN_LEGAL -> settings.openLegal()
            SettingsAction.CLOSE -> onClose()
            SettingsAction.OPEN_DOCUMENT -> Unit
            SettingsAction.BACK -> settings.back()
        }
    }
    val onPlate: (PlateAction) -> Unit = { kind ->
        when (kind) {
            PlateAction.BACK -> settings.back()
            PlateAction.SUBMIT -> settings.submitPlate()
            PlateAction.FIX -> settings.fixPlate()
        }
    }
    when (val current = step) {
        SettingsStep.Main -> MessageScreen(settingsMessage(saved.plateNumber), onAction)
        SettingsStep.Legal -> MessageScreen(legalMessage(), onAction)
        is SettingsStep.PlateInput -> PlateInputScreen(current.text, drive, settings::plateChanged, onPlate)
        is SettingsStep.PlateError -> PlateErrorScreen(current.text, onPlate)
    }
}

/** V01 시작·예외·선택 → V02 예약 확인·예외 → 예약 요청. 예약되면 현재 주문이 생겨 진행 화면으로 넘어간다 */
@Composable
private fun HomeRoute(home: HomeController, drive: DriveContext) {
    val scope = rememberCoroutineScope()
    val step by home.step.collectAsStateWithLifecycle()
    val onHomeAction: (HomeAction) -> Unit = { action ->
        when (action) {
            HomeAction.FindStores -> scope.launch { home.changeStoreAndMenu() }
            HomeAction.OpenNavigation -> home.openNavigation()
            HomeAction.Back -> home.back()
            is HomeAction.PickStore -> scope.launch { home.pickStore(action.store) }
            is HomeAction.PickMenu -> scope.launch { home.pickMenu(action.menu) }
            HomeAction.NextMenuPage -> home.nextMenuPage()
        }
    }
    when (val current = step) {
        HomeStep.Loading -> Unit
        is HomeStep.Start -> HomeScreen(
            state = toHomeUiState(current.quick, drive),
            onChangeMenu = { scope.launch { home.changeStoreAndMenu() } },
            onReserve = { scope.launch { home.reserve() } },
        )
        HomeStep.Empty -> MessageScreen(emptyMessage(drive), onHomeAction)
        HomeStep.NoRoute -> MessageScreen(noRouteMessage(), onHomeAction)
        HomeStep.NoStore -> MessageScreen(noStoreMessage(), onHomeAction)
        is HomeStep.PickStore -> MessageScreen(pickStoreMessage(current.stores, drive), onHomeAction)
        is HomeStep.PickMenu -> MessageScreen(pickMenuMessage(current.store, current.menus, drive, current.page), onHomeAction)
        is HomeStep.Confirm -> {
            // 같은 견적 화면에서 다시 눌러도 주문 한 건으로 처리되게 키를 유지한다 (2차 기획 §5)
            val reserveKey = remember(current.quote) { UUID.randomUUID().toString() }
            if (current.quote.issue == null) {
                ReserveScreen(
                    state = toReserveUiState(current.quote, drive, current.submitting),
                    onBack = home::back,
                    onSubmit = { scope.launch { home.submit(reserveKey) } },
                )
            } else {
                MessageScreen(
                    state = toReserveIssueMessage(current.quote, drive, current.submitting),
                    onAction = { kind ->
                        when (kind) {
                            ReserveIssueAction.BACK_TO_START -> home.back()
                            ReserveIssueAction.CHOOSE_OTHER_MENU -> scope.launch { home.chooseOtherMenu() }
                            ReserveIssueAction.CHOOSE_OTHER_STORE -> scope.launch { home.changeStoreAndMenu() }
                            ReserveIssueAction.SUBMIT -> scope.launch { home.submit(reserveKey) }
                        }
                    },
                )
            }
        }
    }
}

/**
 * 진행 화면(주문 단계에 따라 자동으로 바뀜)과, 사용자가 연 주문 상세·취소 요청 확인(V06-A·B).
 * 상세는 진행 중 주문에서만 열리고, 취소 요청을 보내면 닫혀 취소 확인 중(V06-C)으로 넘어간다.
 */
@Composable
private fun ProgressRoute(
    order: Order,
    drive: DriveContext,
    now: Instant,
    orders: OrderRepository,
    onChooseOtherStore: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val presenter = remember { ProgressPresenter() }
    var detailPage by remember(order.id) { mutableStateOf<DetailPage?>(null) }
    // 취소 거절 확인처럼 presenter 내부 상태만 바뀌는 조작 뒤에 바로 다시 그리기 위한 값
    var renderVersion by remember { mutableIntStateOf(0) }

    val page = detailPage
    if (page != null && order.stage.isActive && order.stage != OrderStage.CANCEL_REQUESTED) {
        OrderDetailScreen(
            state = toOrderDetailUiState(order, page, drive),
            onAction = { kind ->
                when (kind) {
                    DetailAction.Kind.KEEP_ORDER -> detailPage = null
                    DetailAction.Kind.ASK_CANCEL -> detailPage = DetailPage.CONFIRM_CANCEL
                    DetailAction.Kind.SEND_CANCEL -> scope.launch {
                        runCatching { orders.requestCancel(order.id) }
                        detailPage = null
                    }
                }
            },
        )
        return
    }

    key(renderVersion) {
        ProgressScreen(
            state = presenter.present(order, drive, now),
            onAction = { kind ->
                when (kind) {
                    ProgressAction.Kind.NONE -> Unit
                    ProgressAction.Kind.DETAIL -> detailPage = DetailPage.OVERVIEW
                    ProgressAction.Kind.FINISH -> orders.clearFinished()
                    // V03-R 「다른 매장 선택」: 홈으로 돌아가 매장 선택(V01-M)을 연다
                    ProgressAction.Kind.CHOOSE_OTHER_STORE -> onChooseOtherStore()
                    ProgressAction.Kind.REFRESH -> scope.launch { runCatching { orders.refresh() } }
                    ProgressAction.Kind.ACK_CANCEL_REJECTION -> {
                        presenter.acknowledgeCancelRejection(order.id)
                        renderVersion++
                    }
                }
            },
        )
    }
}
