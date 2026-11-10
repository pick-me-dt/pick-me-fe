package com.pickmedt.car.core.model

enum class Gear { PARK, REVERSE, NEUTRAL, DRIVE }

/** 차량에서 읽은 원신호. 아직 못 읽었거나 미지원·권한 없음이면 null */
data class DriveSignals(
    val gear: Gear?,
    val speedKmh: Float?,
    /** 플랫폼 주행 규제(CarUxRestrictions)가 걸려 있는지 */
    val uxRestricted: Boolean?,
) {
    companion object {
        val Unknown = DriveSignals(gear = null, speedKmh = null, uxRestricted = null)
    }
}

/** 2차 기획 §4 차량 맥락 */
enum class DriveContext {
    /** 주차 확인 */
    PARKED,
    /** 주행 */
    DRIVING,
    /** 일시 정차 */
    STOPPED,
    /** 상태 불명 */
    UNKNOWN;

    /** 상세 조작(예약·설정·주문 상세)은 주차 확인일 때만 */
    val allowsDetail: Boolean get() = this == PARKED
}
