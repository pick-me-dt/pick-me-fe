package com.pickmedt.car.platform.navi

import com.pickmedt.car.core.model.RouteSnapshot

/**
 * 내비게이션 경로 (Figma V01-R 경로 없음).
 * 실차 구현은 pleos/ 하위에 NaviHelper로 만든다 (게이트 G3 확인 후).
 */
interface RouteSource {
    /** 지금 설정된 경로. 없으면 null */
    suspend fun currentRoute(): RouteSnapshot?

    /** 내비게이션 앱을 연다 (V01-R 「내비게이션 열기」) */
    fun openNavigation()
}
