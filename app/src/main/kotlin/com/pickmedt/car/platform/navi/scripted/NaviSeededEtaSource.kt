package com.pickmedt.car.platform.navi.scripted

import com.pickmedt.car.core.model.EtaReading
import com.pickmedt.car.platform.navi.EtaSource

/**
 * 실제 내비의 첫 남은 시간으로 모의 ETA를 시작한다.
 * 에뮬레이터는 차량 위치가 움직이지 않아 실측 남은 시간이 줄지 않는다(게이트 G3).
 * 그래서 주문마다 첫 값만 실측(NAVI)으로 보내고, 이후에는 그 값에서 실제 시간만큼 줄어드는 모의 값(SCRIPTED)을 보낸다.
 * 시연 컨트롤로 모의 ETA를 정하면(ETA 6분·지연 +4분) 그 값이 우선한다.
 */
class NaviSeededEtaSource(
    private val navi: EtaSource,
    private val scripted: ScriptedEtaSource,
) : EtaSource {
    override suspend fun read(): EtaReading? {
        scripted.read()?.let { return it }
        val measured = navi.read() ?: return null
        scripted.set(measured.etaSec)
        return measured
    }

    /** 새 주문은 다시 실측 값으로 시작한다 */
    override fun startNewTrip() = scripted.clear()
}
