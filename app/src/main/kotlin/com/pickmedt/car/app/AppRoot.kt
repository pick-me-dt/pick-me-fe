package com.pickmedt.car.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickmedt.car.core.model.DriveContext
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.core.model.QuickOrder
import com.pickmedt.car.core.rule.resolveDriveContext
import com.pickmedt.car.data.order.OrderRepository
import com.pickmedt.car.data.order.Quote
import com.pickmedt.car.demo.DemoPanel
import com.pickmedt.car.feature.home.HomeScreen
import com.pickmedt.car.feature.home.toHomeUiState
import com.pickmedt.car.feature.order.DetailAction
import com.pickmedt.car.feature.order.DetailPage
import com.pickmedt.car.feature.order.OrderDetailScreen
import com.pickmedt.car.feature.order.toOrderDetailUiState
import com.pickmedt.car.feature.progress.ProgressAction
import com.pickmedt.car.feature.progress.ProgressPresenter
import com.pickmedt.car.feature.progress.ProgressScreen
import com.pickmedt.car.feature.reserve.ReserveScreen
import com.pickmedt.car.feature.reserve.toReserveUiState
import com.pickmedt.car.ui.theme.PickmeColors
import com.pickmedt.car.ui.theme.PickmeTheme
import com.pickmedt.car.ui.theme.ProvideDesignScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

/**
 * 최상위 화면. 현재 주문이 없으면 홈 흐름(V01 → V02), 있으면 진행 화면(V03~V07).
 * 아래에는 시연 컨트롤을 붙인다.
 */
@Composable
fun AppRoot(container: AppContainer) {
    val order by container.orderRepository.currentOrder.collectAsStateWithLifecycle()
    val signals by container.driveSignalSource.signals.collectAsStateWithLifecycle()
    val drive = resolveDriveContext(signals)
    val now by produceState(container.clock.now()) {
        while (true) {
            delay(1_000)
            value = container.clock.now()
        }
    }
    PickmeTheme {
        Column(Modifier.fillMaxSize().background(PickmeColors.BackgroundPrimary)) {
            ProvideDesignScale(Modifier.weight(1f).fillMaxWidth()) {
                val current = order
                if (current == null) {
                    HomeFlow(container.orderRepository, drive)
                } else {
                    ProgressRoute(current, drive, now, container.orderRepository)
                }
            }
            DemoPanel(container.demo)
        }
    }
}

/** V01 시작 → V02 예약 확인 → 예약 요청. 예약되면 현재 주문이 생겨 진행 화면으로 넘어간다 */
@Composable
private fun HomeFlow(orders: OrderRepository, drive: DriveContext) {
    val scope = rememberCoroutineScope()
    val quick by produceState<QuickOrder?>(null) { value = orders.quickOrders().firstOrNull() }
    var quote by remember { mutableStateOf<Quote?>(null) }
    var submitting by remember { mutableStateOf(false) }
    // 같은 예약 확인 화면에서 다시 눌러도 주문 한 건으로 처리되게 키를 유지한다 (2차 기획 §5)
    val reserveKey = remember(quote) { UUID.randomUUID().toString() }

    val home = quick ?: return
    val confirming = quote
    if (confirming == null) {
        HomeScreen(
            state = toHomeUiState(home, drive),
            onChangeMenu = {},
            onReserve = { scope.launch { runCatching { orders.quote(home.id) }.onSuccess { quote = it } } },
        )
    } else {
        ReserveScreen(
            state = toReserveUiState(confirming, drive, submitting),
            onBack = { quote = null },
            onSubmit = {
                scope.launch {
                    submitting = true
                    runCatching { orders.reserve(confirming.quickOrderId, reserveKey) }
                    submitting = false
                }
            },
        )
    }
}

/**
 * 진행 화면(주문 단계에 따라 자동으로 바뀜)과, 사용자가 연 주문 상세·취소 요청 확인(V06-A·B).
 * 상세는 진행 중 주문에서만 열리고, 취소 요청을 보내면 닫혀 취소 확인 중(V06-C)으로 넘어간다.
 */
@Composable
private fun ProgressRoute(order: Order, drive: DriveContext, now: Instant, orders: OrderRepository) {
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
                    // 다른 매장 선택(V01-M)은 다음 화면 계획. 그 전까지는 홈으로 돌아간다
                    ProgressAction.Kind.FINISH, ProgressAction.Kind.CHOOSE_OTHER_STORE -> orders.clearFinished()
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
