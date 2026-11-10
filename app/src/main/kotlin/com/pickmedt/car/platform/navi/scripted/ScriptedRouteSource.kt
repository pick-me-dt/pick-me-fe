package com.pickmedt.car.platform.navi.scripted

import com.pickmedt.car.core.model.RouteSnapshot
import com.pickmedt.car.platform.navi.RouteSource

/** 시연용 모의 경로. 처음에는 경로가 있다 */
class ScriptedRouteSource(initial: RouteSnapshot? = DEFAULT) : RouteSource {
    private var route: RouteSnapshot? = initial

    /** 내비게이션 열기를 누른 횟수. 모의 구현은 실제 앱을 열지 않는다 */
    var openNavigationCount: Int = 0
        private set

    fun setRoute(value: RouteSnapshot?) {
        route = value
    }

    override suspend fun currentRoute(): RouteSnapshot? = route

    override fun openNavigation() {
        openNavigationCount++
    }

    companion object {
        val DEFAULT = RouteSnapshot(destinationName = null)
    }
}
