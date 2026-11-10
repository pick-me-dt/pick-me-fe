package com.pickmedt.car.platform.navi

import com.pickmedt.car.core.model.EtaReading

/**
 * 매장 입구까지 남은 시간.
 * 실차 구현은 pleos/ 하위에 NaviHelper로 만든다 (게이트 G3 확인 후).
 */
interface EtaSource {
    /** 지금 남은 시간을 읽는다. 읽을 수 없으면 null */
    suspend fun read(): EtaReading?
}
