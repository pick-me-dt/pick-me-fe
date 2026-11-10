package com.pickmedt.car.platform.navi

import com.pickmedt.car.core.model.EtaReading

/**
 * 매장 입구까지 남은 시간.
 * 실차 구현은 pleos/PleosNavi(NaviHelper), 모의 구현은 scripted/ 하위에 있다.
 */
interface EtaSource {
    /** 지금 남은 시간을 읽는다. 읽을 수 없으면 null */
    suspend fun read(): EtaReading?

    /** 새 주문의 ETA 전송을 시작할 때 부른다. 주문마다 측정을 새로 시작하는 구현만 쓴다 */
    fun startNewTrip() = Unit
}
