package com.pickmedt.car.core.rule

import com.pickmedt.car.core.model.DriveContext
import com.pickmedt.car.core.model.DriveSignals
import com.pickmedt.car.core.model.Gear

/**
 * 2차 기획 §4: 주차 확인일 때만 상세를 허용한다.
 * - 기어를 모르면 상태 불명
 * - P단이어도 플랫폼 주행 규제가 걸리면 규제를 따른다
 * - 속도 0만으로는 주차로 보지 않는다 (일시 정차)
 */
fun resolveDriveContext(signals: DriveSignals): DriveContext {
    val gear = signals.gear ?: return DriveContext.UNKNOWN
    if (gear == Gear.PARK && signals.uxRestricted != true) return DriveContext.PARKED
    return if ((signals.speedKmh ?: 0f) > 0f) DriveContext.DRIVING else DriveContext.STOPPED
}
