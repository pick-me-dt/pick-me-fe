package com.pickmedt.car.platform

import com.pickmedt.car.core.model.Gear
import com.pickmedt.car.platform.drive.car.gearFromVehicleGear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CarGearMappingTest {
    @Test fun `VehicleGear 값을 기어로 바꾼다`() {
        assertEquals(Gear.NEUTRAL, gearFromVehicleGear(0x1))
        assertEquals(Gear.REVERSE, gearFromVehicleGear(0x2))
        assertEquals(Gear.PARK, gearFromVehicleGear(0x4))
        assertEquals(Gear.DRIVE, gearFromVehicleGear(0x8))
    }

    @Test fun `수동 단수는 전진으로 본다`() = assertEquals(Gear.DRIVE, gearFromVehicleGear(0x20))

    @Test fun `모르는 값은 null`() = assertNull(gearFromVehicleGear(0))
}
