package com.pickmedt.car.platform.navi.pleos

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.NaviMode
import ai.pleos.playground.navi.constants.NavigationStatus
import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.NavigationScreenRequest
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.util.Log
import com.pickmedt.car.core.model.EtaOrigin
import com.pickmedt.car.core.model.EtaReading
import com.pickmedt.car.core.model.RouteSnapshot
import com.pickmedt.car.core.time.Clock
import com.pickmedt.car.platform.navi.EtaSource
import com.pickmedt.car.platform.navi.RouteSource
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Pleos 내비게이션(NaviHelper) 연결. 경로 유무·남은 시간·내비 화면 열기를 제공한다.
 *
 * 에뮬레이터 확인 결과 (docs/plans/2026-11-10-emulator-gates.md G3):
 * - 권한: 조회 NAVI_ROUTE, 경로 요청 NAVI_ROUTE_SEARCH
 * - remainTime은 초, remainDistance는 미터
 * - 안내 중에도 RouteStateInfo.isRouting은 false라서 경로 유무는 naviMode로 판단한다
 *
 * 조회는 요청 후 리스너로 답이 오는 방식이라, 요청마다 다음 답을 기다린다(최대 2초).
 * 모든 이벤트는 로그(태그 PickmeNavi)에 남긴다.
 */
class PleosNavi(context: Context, private val clock: Clock) : RouteSource, EtaSource {
    private val navi = NaviHelper(context)

    @Volatile private var ready = false

    private val routeStates = answers<RouteStateInfo>()
    private val destinations = answers<DestinationInfo>()

    private val listener = object : NaviHelperEventListener {
        override fun onRouteStateInfo(info: RouteStateInfo) {
            log("경로 상태 mode=${info.naviMode} routing=${info.isRouting}")
            routeStates.tryEmit(info)
        }

        override fun onDestinationInfo(info: DestinationInfo) {
            log("목적지 name=${info.name} remainTime=${info.remainTime}s remainDistance=${info.remainDistance}m")
            destinations.tryEmit(info)
        }

        override fun onRoutingCompleted() = log("경로 탐색 완료")
        override fun onRouteCancelled() = log("경로 취소")
        override fun onRouteEnded() = log("경로 종료")
        override fun onError(code: NaviErrorCode) = log("오류 $code")
        override fun onNavigationStatus(status: NavigationStatus) = log("내비 상태 $status")
    }

    fun start() {
        navi.initialize(
            {
                ready = true
                navi.addListener(listener)
                log("초기화 성공")
            },
            { error -> log("초기화 실패 ${error.message}") },
        )
    }

    /** 안내(실제·모의 주행) 중일 때만 경로가 있다고 본다 */
    override suspend fun currentRoute(): RouteSnapshot? {
        if (!isGuiding()) return null
        val destination = ask(destinations) { navi.getDestinationInfo() }
        return RouteSnapshot(destinationName = destination?.name?.takeIf { it.isNotBlank() })
    }

    /** V01-R 「내비게이션 열기」 */
    override fun openNavigation() {
        call { navi.requestNavigationScreen(NavigationScreenRequest.Foreground) }
    }

    /** 목적지까지 남은 시간. 안내 중이 아니면 null */
    override suspend fun read(): EtaReading? {
        if (!isGuiding()) return null
        val destination = ask(destinations) { navi.getDestinationInfo() } ?: return null
        return EtaReading(destination.remainTime, clock.now(), EtaOrigin.NAVI)
    }

    /** 시연용: 좌표로 경로를 요청한다. 내비가 열리고 경로 선택 화면이 나온다 */
    fun requestRoute(latitude: Double, longitude: Double, name: String) {
        call {
            navi.requestRoute(RouteInfo(latitude, longitude, null, null, name, "", "", "", RouteOption.RECOMMENDED, "", false))
            log("경로 요청 $name")
        }
    }

    /** 시연용: 경로 상태와 남은 시간을 조회해 로그로 남긴다 */
    fun logStatus() {
        call {
            navi.getRouteStateInfo()
            navi.getDestinationInfo()
        }
    }

    private suspend fun isGuiding(): Boolean {
        val state = ask(routeStates) { navi.getRouteStateInfo() } ?: return false
        return state.naviMode == NaviMode.ROUTE_GUIDING || state.naviMode == NaviMode.SIMULATION_GUIDING
    }

    /** 요청하고 다음 답을 기다린다. 초기화 전·호출 실패·시간 초과면 null */
    private suspend fun <T> ask(answers: SharedFlow<T>, request: () -> Unit): T? {
        if (!ready) return null
        return withTimeoutOrNull(ANSWER_TIMEOUT_MS) {
            coroutineScope {
                val answer = async(start = CoroutineStart.UNDISPATCHED) { answers.first() }
                if (call(request)) {
                    answer.await()
                } else {
                    answer.cancel()
                    null
                }
            }
        }
    }

    private fun call(block: () -> Unit): Boolean {
        if (!ready) {
            log("아직 초기화 전")
            return false
        }
        return runCatching(block).onFailure { log("호출 실패 ${it.message}") }.isSuccess
    }

    private fun log(message: String) {
        Log.i(TAG, message)
    }

    private companion object {
        const val TAG = "PickmeNavi"
        const val ANSWER_TIMEOUT_MS = 2_000L

        fun <T> answers() = MutableSharedFlow<T>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
}
