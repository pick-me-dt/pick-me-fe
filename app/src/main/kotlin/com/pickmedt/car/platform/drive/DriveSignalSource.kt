package com.pickmedt.car.platform.drive

import com.pickmedt.car.core.model.DriveSignals
import kotlinx.coroutines.flow.StateFlow

/**
 * 차량 주행 신호(기어·속도·주행 규제).
 * 실차 구현은 car/ 하위에 AAOS Car API로 만든다 (게이트 G2·G4 확인 후).
 */
interface DriveSignalSource {
    val signals: StateFlow<DriveSignals>
}
