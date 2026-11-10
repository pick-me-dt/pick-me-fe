package com.pickmedt.car.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pickmedt.car.core.model.Gear

/** 시연 컨트롤 화면. 앱 영역과 분리해 아래에 붙인다 (Figma 프로토타입 840×1184 앱 영역 + 하단 컨트롤) */
@Composable
fun DemoPanel(controller: DemoController, modifier: Modifier = Modifier) {
    var message by remember { mutableStateOf("시연 컨트롤 · 실제 주문/차량 연동 없음") }
    val actions: List<Pair<String, () -> Result<Unit>>> = listOf(
        "매장 수락" to controller::accept,
        "매장 거절" to controller::reject,
        "응답 지연" to controller::slowStoreResponse,
        "제조 시작" to controller::startPreparing,
        "준비 완료" to controller::markReady,
        "수령 완료" to controller::pickUp,
        "취소 승인" to controller::approveCancel,
        "취소 거절" to controller::rejectCancel,
        "ETA 6분" to { controller.setEta(360) },
        "지연 +4분" to { controller.delayEta(240) },
        "연결 끊김" to controller::disconnect,
        "연결 복구" to controller::reconnect,
        "P단" to { controller.shift(Gear.PARK) },
        "D단" to { controller.shift(Gear.DRIVE) },
        "초기화" to controller::reset,
    )
    Column(
        modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1D17))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(message, color = Color(0xFFE6E6E6))
        actions.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, action) ->
                    Button(
                        onClick = { message = action().fold({ "$label 완료" }, { "$label 실패: ${it.message}" }) },
                        modifier = Modifier.weight(1f),
                    ) { Text(label) }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
