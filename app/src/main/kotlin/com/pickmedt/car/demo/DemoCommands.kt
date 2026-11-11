package com.pickmedt.car.demo

import com.pickmedt.car.core.model.Gear
import com.pickmedt.car.core.model.GeoPoint

/** 화면 흐름을 바꾸는 시연 조작. AppRoot가 받아서 처리한다 */
enum class DemoUiEvent { RESTART_APP, OPEN_SETTINGS, RELOAD_HOME }

/**
 * 실서버 시연 조작. 구현은 app 계층이 서버 개발 API와 매장 API(시드 직원 토큰)로 만든다.
 * 서버 호출은 백그라운드로 보내므로, 성공은 「요청을 보냈다」는 뜻이다 (실패는 logcat)
 */
interface ServerDemoActions {
    /** 진행 중 주문을 한 단계 넘긴다 (수락 → 제조 시작 → 준비 완료 → 수령, 취소 확인 중이면 승인) */
    fun next(): Result<Unit>
    fun refresh(): Result<Unit>
    fun reset(): Result<Unit>
    fun accept(): Result<Unit>
    /** [reason]: SOLD_OUT·BUSY·CLOSED·OTHER */
    fun reject(reason: String?): Result<Unit>
    fun prepare(): Result<Unit>
    fun ready(): Result<Unit>
    fun pickup(): Result<Unit>
    fun approveCancel(): Result<Unit>
    fun rejectCancel(): Result<Unit>
    fun soldOut(): Result<Unit>
    fun closeStore(): Result<Unit>
    fun clearStore(): Result<Unit>
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
            // 가짜 서버와 같은 이름. 서버가 판단하거나 서버에 API가 없는 것은 이유를 돌려준다
            add(Command("sold-out", "sold-out", "메뉴 품절") { server.soldOut() })
            add(Command("price-up", "price-up", "가격 변경") { Result.failure(UnsupportedOperationException(NO_PRICE_API)) })
            add(Command("closed", "closed", "접수 종료") { server.closeStore() })
            add(Command("clear", "clear", "조건 해제") { server.clearStore() })
            add(Command("accept", "accept", "매장 수락") { server.accept() })
            add(Command("reject", "reject [사유]", "매장 거절") { arg -> server.reject(arg) })
            add(Command("slow", "slow", "응답 지연") { Result.failure(UnsupportedOperationException(SLOW_BY_SERVER)) })
            add(Command("prepare", "prepare", "제조 시작") { server.prepare() })
            add(Command("ready", "ready", "준비 완료") { server.ready() })
            add(Command("pickup", "pickup", "수령 완료") { server.pickup() })
            add(Command("cancel-ok", "cancel-ok", "취소 승인") { server.approveCancel() })
            add(Command("cancel-no", "cancel-no", "취소 거절") { server.rejectCancel() })
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
        add(Command("navi-route", "navi-route [위도,경도]", "내비 경로(양재)") { arg -> point(arg).mapCatching { controller.naviRoute(it).getOrThrow() } })
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

    private companion object {
        const val NO_PRICE_API = "실서버에는 가격을 바꾸는 API가 없어요 (가짜 서버에서만 시연)"
        const val SLOW_BY_SERVER = "실서버는 예약 후 2분 동안 수락이 없으면 서버가 응답 지연으로 판단해요"
    }

    private fun ui(event: DemoUiEvent): Result<Unit> = Result.success(controller.requestUi(event))

    /** 「37.4199,127.0958」 → 좌표. 없으면 null(원래 양재 좌표) */
    private fun point(arg: String?): Result<GeoPoint?> {
        if (arg == null) return Result.success(null)
        val parts = arg.split(',').map { it.trim().toDoubleOrNull() }
        val lat = parts.getOrNull(0)
        val lng = parts.getOrNull(1)
        return if (parts.size == 2 && lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
            Result.success(GeoPoint(lat, lng))
        } else {
            Result.failure(IllegalArgumentException("좌표는 위도,경도로 넣어 주세요. 예: navi-route 37.4199,127.0958"))
        }
    }

    private fun minutes(arg: String?): Result<Int> =
        arg?.toIntOrNull()?.takeIf { it >= 0 }?.let { Result.success(it) }
            ?: Result.failure(IllegalArgumentException("분을 숫자로 넣어 주세요. 예: eta 6"))
}
