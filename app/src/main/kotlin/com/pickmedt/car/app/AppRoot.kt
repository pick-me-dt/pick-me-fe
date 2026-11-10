package com.pickmedt.car.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickmedt.car.core.model.DriveContext
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.model.QuickOrder
import com.pickmedt.car.core.rule.resolveDriveContext
import com.pickmedt.car.data.order.OrderRepository
import com.pickmedt.car.data.order.Quote
import com.pickmedt.car.demo.DemoPanel
import com.pickmedt.car.feature.home.HomeScreen
import com.pickmedt.car.feature.home.toHomeUiState
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

@Composable
private fun ProgressRoute(order: Order, drive: DriveContext, now: Instant, orders: OrderRepository) {
    val presenter = remember { ProgressPresenter() }
    ProgressScreen(
        state = presenter.present(order, drive, now),
        onAction = { kind -> if (kind == ProgressAction.Kind.FINISH) orders.clearFinished() },
    )
}
