package com.pickmedt.car.platform.drive.car

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.drivingstate.CarUxRestrictionsManager
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import com.pickmedt.car.core.model.DriveSignals
import com.pickmedt.car.core.model.Gear
import com.pickmedt.car.platform.drive.DriveSignalSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * AAOS Car API로 읽는 실차 주행 신호.
 * 기어는 GEAR_SELECTION(권한 CAR_POWERTRAIN), 속도는 PERF_VEHICLE_SPEED(권한 CAR_SPEED, 없으면 null),
 * 주행 규제는 CarUxRestrictionsManager의 isRequiresDistractionOptimization으로 읽는다.
 * 값이 바뀔 때마다 로그(태그 PickmeDrive)를 남긴다 (게이트 G2·G4 확인용).
 */
class CarDriveSignalSource(private val context: Context) : DriveSignalSource {
    private val state = MutableStateFlow(DriveSignals.Unknown)
    override val signals: StateFlow<DriveSignals> = state.asStateFlow()

    private var car: Car? = null

    fun start() {
        if (car != null) return
        car = Car.createCar(context, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connected, ready ->
            if (ready) {
                onReady(connected)
            } else {
                Log.w(TAG, "Car 서비스 연결 끊김")
                state.value = DriveSignals.Unknown
            }
        }
    }

    private fun onReady(car: Car) {
        val properties = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
        subscribe(properties, VehiclePropertyIds.GEAR_SELECTION, "기어") { value ->
            val gear = gearFromVehicleGear(value as Int)
            state.update { it.copy(gear = gear) }
            "raw=$value → $gear"
        }
        subscribe(properties, VehiclePropertyIds.PERF_VEHICLE_SPEED, "속도") { value ->
            val kmh = (value as Float) * MPS_TO_KMH
            state.update { it.copy(speedKmh = kmh) }
            "${value}m/s → ${kmh}km/h"
        }

        val ux = car.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
        val onUx = CarUxRestrictionsManager.OnUxRestrictionsChangedListener { restrictions ->
            val restricted = restrictions.isRequiresDistractionOptimization
            state.update { it.copy(uxRestricted = restricted) }
            Log.i(TAG, "주행 규제 restricted=$restricted active=${restrictions.activeRestrictions}")
        }
        ux.registerListener(onUx)
        onUx.onUxRestrictionsChanged(ux.currentCarUxRestrictions)
    }

    private fun subscribe(
        properties: CarPropertyManager,
        propertyId: Int,
        label: String,
        onValue: (Any) -> String,
    ) {
        val callback = object : CarPropertyManager.CarPropertyEventCallback {
            override fun onChangeEvent(value: CarPropertyValue<*>) {
                val raw = value.value ?: return
                Log.i(TAG, "$label ${onValue(raw)}")
            }

            override fun onErrorEvent(propId: Int, zone: Int) {
                Log.w(TAG, "$label 오류 property=$propId zone=$zone")
            }
        }
        try {
            @Suppress("DEPRECATION") // subscribePropertyEvents는 API 35부터. 에뮬레이터는 API 34
            properties.registerCallback(callback, propertyId, CarPropertyManager.SENSOR_RATE_ONCHANGE)
        } catch (e: SecurityException) {
            Log.w(TAG, "$label 권한 없음: ${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "$label 지원 안 함: ${e.message}")
        }
    }

    private companion object {
        const val TAG = "PickmeDrive"
        const val MPS_TO_KMH = 3.6f
    }
}

/**
 * VehicleGear 값 → 기어. 1~9단(GEAR_1 이상)은 전진(D)으로 본다. 모르는 값은 null.
 * 값: NEUTRAL 0x1, REVERSE 0x2, PARK 0x4, DRIVE 0x8, GEAR_1 0x10 … (android.car.VehicleGear)
 */
fun gearFromVehicleGear(value: Int): Gear? = when {
    value == 0x1 -> Gear.NEUTRAL
    value == 0x2 -> Gear.REVERSE
    value == 0x4 -> Gear.PARK
    value == 0x8 || value >= 0x10 -> Gear.DRIVE
    else -> null
}
