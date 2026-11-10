package com.pickmedt.car.platform

import com.pickmedt.car.core.model.DriveSignals
import com.pickmedt.car.core.model.Gear
import com.pickmedt.car.platform.drive.fake.FakeDriveSignalSource
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeDriveSignalSourceTest {
    @Test fun `처음에는 P단 정지`() =
        assertEquals(DriveSignals(Gear.PARK, 0f, false), FakeDriveSignalSource().signals.value)

    @Test fun `D단으로 바꾸면 주행 속도와 주행 규제가 함께 바뀐다`() {
        val source = FakeDriveSignalSource()
        source.shift(Gear.DRIVE)
        assertEquals(DriveSignals(Gear.DRIVE, 30f, true), source.signals.value)
    }

    @Test fun `P단으로 돌아오면 정지하고 규제가 풀린다`() {
        val source = FakeDriveSignalSource()
        source.shift(Gear.DRIVE)
        source.shift(Gear.PARK)
        assertEquals(DriveSignals(Gear.PARK, 0f, false), source.signals.value)
    }
}
