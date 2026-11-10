package com.pickmedt.car.platform.navi.pleos

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.NavigationStatus
import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.DrivingInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.util.Log

/**
 * 게이트 G3 확인용. NaviHelper로 경로를 요청하고 남은 시간 관련 값을 그대로 로그(태그 PickmeNavi)에 남긴다.
 * remainTime·duration의 단위와 시간에 따른 변화를 확인한 뒤 EtaSource·RouteSource 구현으로 옮긴다.
 */
class PleosNaviProbe(context: Context) {
    private val navi = NaviHelper(context)
    private var ready = false

    private val listener = object : NaviHelperEventListener {
        override fun onRouteStarted(info: RouteStartInfo) = log("경로 시작 $info")
        override fun onRoutingCompleted() = log("경로 탐색 완료")
        override fun onRouteCancelled() = log("경로 취소")
        override fun onRouteEnded() = log("경로 종료")
        override fun onDestinationChanged() = log("목적지 변경")
        override fun onError(code: NaviErrorCode) = log("오류 $code")
        override fun onNavigationStatus(status: NavigationStatus) = log("내비 상태 $status")
        override fun onRouteStateInfo(info: RouteStateInfo) = log("경로 상태 routing=${info.isRouting} mode=${info.naviMode} terms=${info.termsAgreement}")
        override fun onDestinationInfo(info: DestinationInfo) =
            log("목적지 name=${info.name} remainTime=${info.remainTime} remainDistance=${info.remainDistance}")
        override fun onDrivingInfo(info: DrivingInfo) =
            log("주행 정보 destination=${info.destination?.name} duration=${info.destination?.duration} distance=${info.destination?.distance}")
    }

    fun start() {
        navi.initialize(
            {
                ready = true
                log("초기화 성공")
                navi.addListener(listener)
                query()
            },
            { error -> log("초기화 실패 ${error.message}") },
        )
    }

    /** 양재점(서버 시드 좌표)으로 경로를 요청한다 */
    fun requestRouteToYangjae() = whenReady {
        navi.requestRoute(
            RouteInfo(
                YANGJAE_LAT, YANGJAE_LNG, null, null,
                "픽미 DT 양재점", "", "", "",
                RouteOption.RECOMMENDED, "", false,
            ),
        )
        log("경로 요청 보냄")
    }

    /** 지금 경로 상태와 목적지 남은 시간을 묻는다. 답은 리스너 로그로 온다 */
    fun query() = whenReady {
        navi.getRouteStateInfo()
        navi.getDestinationInfo()
    }

    private inline fun whenReady(block: () -> Unit) {
        if (!ready) {
            log("아직 초기화 전")
            return
        }
        runCatching(block).onFailure { log("호출 실패 ${it.message}") }
    }

    private fun log(message: String) {
        Log.i(TAG, message)
    }

    private companion object {
        const val TAG = "PickmeNavi"
        const val YANGJAE_LAT = 37.4705
        const val YANGJAE_LNG = 127.035
    }
}
