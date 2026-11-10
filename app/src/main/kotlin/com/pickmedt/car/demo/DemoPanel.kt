package com.pickmedt.car.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pickmedt.car.core.model.Gear

private typealias DemoAction = Pair<String, () -> Result<Unit>>

private const val COLUMNS = 4

/**
 * 시연 컨트롤 화면. 앱 영역과 분리해 아래에 붙인다 (Figma 프로토타입 840×1184 앱 영역 + 하단 컨트롤).
 * 버튼이 많아 묶음(시작·홈·예약, 매장, 이동)으로 나눠 보여 준다.
 * 화면 흐름을 바꾸는 조작(앱 다시 시작, 설정 열기, 홈 다시 불러오기)은 AppRoot가 넘겨준다.
 * 촬영할 때 앱 영역을 넓게 쓰도록 접을 수 있다.
 */
@Composable
fun DemoPanel(
    controller: DemoController,
    onRestartApp: () -> Unit,
    onOpenSettings: () -> Unit,
    onReloadHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var message by remember { mutableStateOf("시연 컨트롤 · 실제 주문/차량 연동 없음") }
    var group by remember { mutableIntStateOf(0) }
    var expanded by remember { mutableStateOf(true) }
    val ok = Result.success(Unit)
    val groups: List<Pair<String, List<DemoAction>>> = listOf(
        "시작·홈·예약" to listOf(
            "첫 실행" to { controller.firstRun().onSuccess { onRestartApp() } },
            "앱 다시 시작" to { onRestartApp(); ok },
            "조회 실패" to controller::failNextLoad,
            "설정 열기" to { onOpenSettings(); ok },
            "등록 내역 없음" to { controller.clearQuickOrders().onSuccess { onReloadHome() } },
            "경로 없음" to controller::removeRoute,
            "매장 없음" to controller::noStores,
            "조건 해제" to controller::clearIssues,
            "메뉴 품절" to controller::soldOut,
            "가격 변경" to controller::priceUp,
            "접수 종료" to controller::closeStore,
        ),
        "매장" to listOf(
            "매장 수락" to controller::accept,
            "매장 거절" to controller::reject,
            "응답 지연" to controller::slowStoreResponse,
            "제조 시작" to controller::startPreparing,
            "준비 완료" to controller::markReady,
            "수령 완료" to controller::pickUp,
            "취소 승인" to controller::approveCancel,
            "취소 거절" to controller::rejectCancel,
        ),
        "이동" to listOf(
            "ETA 6분" to { controller.setEta(360) },
            "지연 +4분" to { controller.delayEta(240) },
            "연결 끊김" to controller::disconnect,
            "연결 복구" to controller::reconnect,
            "P단" to { controller.shift(Gear.PARK) },
            "D단" to { controller.shift(Gear.DRIVE) },
            "내비 경로(양재)" to controller::naviRoute,
            "내비 조회" to controller::naviQuery,
        ),
    )
    fun run(label: String, action: () -> Result<Unit>) {
        message = action().fold({ "$label 완료" }, { "$label 실패: ${it.message}" })
    }

    Column(
        modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1D17))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(message, color = Color(0xFFE6E6E6), modifier = Modifier.weight(1f))
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "접기" else "펴기") }
        }
        if (!expanded) return@Column
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            groups.forEachIndexed { index, (name, _) ->
                val selected = index == group
                val colors = if (selected) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                if (selected) {
                    Button(onClick = { group = index }, colors = colors, modifier = Modifier.weight(1f)) { Text(name) }
                } else {
                    OutlinedButton(onClick = { group = index }, colors = colors, modifier = Modifier.weight(1f)) { Text(name) }
                }
            }
            Button(onClick = { run("초기화") { controller.reset().onSuccess { onReloadHome() } } }, modifier = Modifier.weight(1f)) {
                Text("초기화")
            }
        }
        groups[group].second.chunked(COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, action) ->
                    Button(onClick = { run(label, action) }, modifier = Modifier.weight(1f)) { Text(label, maxLines = 1) }
                }
                repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
