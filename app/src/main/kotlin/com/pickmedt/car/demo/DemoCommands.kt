package com.pickmedt.car.demo

import com.pickmedt.car.core.model.Gear

/** 화면 흐름을 바꾸는 시연 조작. AppRoot가 받아서 처리한다 */
enum class DemoUiEvent { RESTART_APP, OPEN_SETTINGS, RELOAD_HOME }

/** 실서버 시연 조작. 구현은 app 계층이 실서버 개발 API로 만든다 */
interface ServerDemoActions {
    /** 진행 중 주문을 한 단계 넘긴다 (예약 직후면 매장 전송) */
    fun next(): Result<Unit>
    fun refresh(): Result<Unit>
    fun reset(): Result<Unit>
}

/**
 * 터미널 시연 명령 (scripts/demo.sh → adb → debug 빌드 수신기). 시연 컨트롤 버튼과 같은 동작을 한다.
 * 영상에 시연 컨트롤이 보이지 않게 하려고 만들었다 (`panel off`).
 * 실서버를 쓰면([server]가 있으면) 가짜 서버용 명령 대신 서버 명령을 준다.
 */
class DemoCommands(
    private val controller: DemoController,
    private val server: ServerDemoActions? = null,
) {
    data class Command(
        val name: String,
        val usage: String,
        val label: String,
        val run: (arg: String?) -> Result<Unit>,
    )

    val commands: List<Command> = buildList {
        add(Command("first-run", "first-run", "첫 실행") { controller.firstRun().onSuccess { controller.requestUi(DemoUiEvent.RESTART_APP) } })
        add(Command("restart", "restart", "앱 다시 시작") { ui(DemoUiEvent.RESTART_APP) })
        add(Command("settings", "settings", "설정 열기") { ui(DemoUiEvent.OPEN_SETTINGS) })
        if (server == null) {
            add(Command("fail-load", "fail-load", "조회 실패") { controller.failNextLoad() })
            add(Command("no-quick", "no-quick", "등록 내역 없음") { controller.clearQuickOrders().onSuccess { controller.requestUi(DemoUiEvent.RELOAD_HOME) } })
            add(Command("no-route", "no-route", "경로 없음") { controller.removeRoute() })
            add(Command("no-store", "no-store", "매장 없음") { controller.noStores() })
            add(Command("sold-out", "sold-out", "메뉴 품절") { controller.soldOut() })
            add(Command("price-up", "price-up", "가격 변경") { controller.priceUp() })
            add(Command("closed", "closed", "접수 종료") { controller.closeStore() })
            add(Command("clear", "clear", "조건 해제") { controller.clearIssues() })
            add(Command("accept", "accept", "매장 수락") { controller.accept() })
            add(Command("reject", "reject", "매장 거절") { controller.reject() })
            add(Command("slow", "slow", "응답 지연") { controller.slowStoreResponse() })
            add(Command("prepare", "prepare", "제조 시작") { controller.startPreparing() })
            add(Command("ready", "ready", "준비 완료") { controller.markReady() })
            add(Command("pickup", "pickup", "수령 완료") { controller.pickUp() })
            add(Command("cancel-ok", "cancel-ok", "취소 승인") { controller.approveCancel() })
            add(Command("cancel-no", "cancel-no", "취소 거절") { controller.rejectCancel() })
        } else {
            add(Command("next", "next", "다음 단계") { server.next() })
            add(Command("refresh", "refresh", "새로고침") { server.refresh() })
            add(Command("server-reset", "server-reset", "서버 초기화") { server.reset().onSuccess { controller.requestUi(DemoUiEvent.RELOAD_HOME) } })
        }
        add(Command("eta", "eta <분>", "ETA 설정") { arg -> minutes(arg).mapCatching { controller.setEta(it * 60).getOrThrow() } })
        add(Command("delay", "delay <분>", "ETA 늘리기") { arg -> minutes(arg).mapCatching { controller.delayEta(it * 60).getOrThrow() } })
        add(Command("disconnect", "disconnect", "연결 끊김") { controller.disconnect() })
        add(Command("reconnect", "reconnect", "연결 복구") { controller.reconnect() })
        add(Command("park", "park", "P단") { controller.shift(Gear.PARK) })
        add(Command("drive", "drive", "D단") { controller.shift(Gear.DRIVE) })
        add(Command("navi-route", "navi-route", "내비 경로(양재)") { controller.naviRoute() })
        add(Command("navi-status", "navi-status", "내비 조회") { controller.naviQuery() })
        add(Command("reset", "reset", "초기화") { controller.reset().onSuccess { controller.requestUi(DemoUiEvent.RELOAD_HOME) } })
        add(Command("panel", "panel on|off", "시연 컨트롤 보이기·숨기기") { arg ->
            when (arg) {
                "on" -> Result.success(controller.setPanelVisible(true))
                "off" -> Result.success(controller.setPanelVisible(false))
                else -> Result.failure(IllegalArgumentException("panel on 또는 panel off"))
            }
        })
    }

    /** 실행 결과 문구. 예: "매장 수락 완료", "매장 수락 실패: …" */
    fun run(name: String, arg: String?): String {
        if (name == "help" || name.isBlank()) return help()
        val command = commands.firstOrNull { it.name == name } ?: return "모르는 명령: $name (help로 목록 확인)"
        return command.run(arg?.takeIf { it.isNotBlank() })
            .fold({ "${command.label} 완료" }, { "${command.label} 실패: ${it.message}" })
    }

    fun help(): String = commands.joinToString("\n") { "${it.usage.padEnd(14)} ${it.label}" }

    private fun ui(event: DemoUiEvent): Result<Unit> = Result.success(controller.requestUi(event))

    private fun minutes(arg: String?): Result<Int> =
        arg?.toIntOrNull()?.takeIf { it >= 0 }?.let { Result.success(it) }
            ?: Result.failure(IllegalArgumentException("분을 숫자로 넣어 주세요. 예: eta 6"))
}
