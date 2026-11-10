package com.pickmedt.car.platform.drive.fake

import com.pickmedt.car.core.model.DriveSignals
import com.pickmedt.car.core.model.Gear
import com.pickmedt.car.platform.drive.DriveSignalSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 에뮬레이터 없이 개발·시연할 때 쓰는 주행 신호. 시연 컨트롤에서 값을 바꾼다. */
class FakeDriveSignalSource(
    initial: DriveSignals = DriveSignals(Gear.PARK, speedKmh = 0f, uxRestricted = false),
) : DriveSignalSource {
    private val state = MutableStateFlow(initial)
    override val signals: StateFlow<DriveSignals> = state.asStateFlow()

    fun set(signals: DriveSignals) {
        state.value = signals
    }

    /** 기어를 바꾸면 실차처럼 속도와 주행 규제도 함께 바뀐다 */
    fun shift(gear: Gear) {
        val parked = gear == Gear.PARK
        state.value = DriveSignals(gear, speedKmh = if (parked) 0f else DRIVING_SPEED_KMH, uxRestricted = !parked)
    }

    private companion object {
        const val DRIVING_SPEED_KMH = 30f
    }
}
