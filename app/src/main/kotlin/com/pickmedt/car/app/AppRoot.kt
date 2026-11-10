package com.pickmedt.car.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickmedt.car.core.model.DriveContext
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.rule.resolveDriveContext
import com.pickmedt.car.data.order.OrderRepository
import com.pickmedt.car.demo.DemoPanel
import com.pickmedt.car.ui.theme.PickmeTheme
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 최상위 화면. 앱 영역 + 시연 컨트롤.
 * 앱 영역은 뼈대 확인용 상태 표시이고, 화면 구현 계획에서 feature/ 화면으로 바꾼다.
 */
@Composable
fun AppRoot(container: AppContainer) {
    val order by container.orderRepository.currentOrder.collectAsStateWithLifecycle()
    val signals by container.driveSignalSource.signals.collectAsStateWithLifecycle()
    PickmeTheme {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp)) {
                SkeletonStatus(order, resolveDriveContext(signals), container.orderRepository)
            }
            DemoPanel(container.demo)
        }
    }
}

@Composable
private fun SkeletonStatus(order: Order?, drive: DriveContext, orders: OrderRepository) {
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("주행 맥락: $drive", color = MaterialTheme.colorScheme.onBackground)
        if (order == null) {
            Text("진행 중 주문 없음", color = MaterialTheme.colorScheme.onBackground)
            Button(
                enabled = drive.allowsDetail,
                onClick = {
                    scope.launch {
                        val quick = orders.quickOrders().first()
                        orders.reserve(quick.id, UUID.randomUUID().toString())
                    }
                },
            ) { Text("이 메뉴로 예약") }
        } else {
            Text("${order.displayNo} · ${order.stage}", color = MaterialTheme.colorScheme.onBackground)
            Text("${order.store.name} · ${order.total.format()}", color = MaterialTheme.colorScheme.onBackground)
            order.timing?.let {
                Text("입구 ${it.entranceEtaAt} · 추천 ${it.recommendedStartAt} · 지연 ${it.delaySec}초", color = MaterialTheme.colorScheme.onBackground)
            }
            if (!order.stage.isActive) {
                Button(onClick = { orders.clearFinished() }) { Text("닫기") }
            }
        }
    }
}
