package com.pickmedt.car.core.rule

import com.pickmedt.car.core.model.DriveContext
import com.pickmedt.car.core.model.DriveSignals
import com.pickmedt.car.core.model.Gear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveContextRulesTest {
    @Test fun `기어를 모르면 상태 불명`() =
        assertEquals(DriveContext.UNKNOWN, resolveDriveContext(DriveSignals.Unknown))

    @Test fun `P단이고 주행 규제가 없으면 주차 확인`() =
        assertEquals(DriveContext.PARKED, resolveDriveContext(DriveSignals(Gear.PARK, 0f, false)))

    @Test fun `P단이고 주행 규제를 모르면 기어를 따라 주차 확인`() =
        assertEquals(DriveContext.PARKED, resolveDriveContext(DriveSignals(Gear.PARK, null, null)))

    @Test fun `P단이어도 플랫폼이 주행 규제를 걸면 주차로 보지 않는다`() =
        assertEquals(DriveContext.STOPPED, resolveDriveContext(DriveSignals(Gear.PARK, 0f, true)))

    @Test fun `D단 속도 0은 일시 정차`() =
        assertEquals(DriveContext.STOPPED, resolveDriveContext(DriveSignals(Gear.DRIVE, 0f, true)))

    @Test fun `D단 속도를 모르면 일시 정차`() =
        assertEquals(DriveContext.STOPPED, resolveDriveContext(DriveSignals(Gear.DRIVE, null, null)))

    @Test fun `D단 속도가 있으면 주행`() =
        assertEquals(DriveContext.DRIVING, resolveDriveContext(DriveSignals(Gear.DRIVE, 30f, true)))

    @Test fun `R단 속도가 있으면 주행`() =
        assertEquals(DriveContext.DRIVING, resolveDriveContext(DriveSignals(Gear.REVERSE, 5f, true)))

    @Test fun `상세 조작은 주차 확인일 때만 허용`() {
        assertTrue(DriveContext.PARKED.allowsDetail)
        assertFalse(DriveContext.DRIVING.allowsDetail)
        assertFalse(DriveContext.STOPPED.allowsDetail)
        assertFalse(DriveContext.UNKNOWN.allowsDetail)
    }
}
