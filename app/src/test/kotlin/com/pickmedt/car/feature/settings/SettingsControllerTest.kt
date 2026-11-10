package com.pickmedt.car.feature.settings

import com.pickmedt.car.data.fake.InMemoryUserSettings
import com.pickmedt.car.data.settings.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsControllerTest {
    private val settings = InMemoryUserSettings(UserSettings(consentAgreed = true, plateNumber = "12가3456"))
    private val controller = SettingsController(settings)

    @Test fun `차량번호 변경은 등록한 번호를 표시 형식으로 채운다`() {
        controller.editPlate()
        assertEquals(SettingsStep.PlateInput("12가 3456"), controller.step.value)
    }

    @Test fun `새 번호가 맞으면 저장하고 설정으로 돌아간다`() {
        controller.editPlate()
        controller.plateChanged("34나 5678")
        controller.submitPlate()
        assertEquals(SettingsStep.Main, controller.step.value)
        assertEquals("34나5678", settings.settings.value.plateNumber)
    }

    @Test fun `형식이 틀리면 입력 오류, 수정하면 입력으로`() {
        controller.editPlate()
        controller.plateChanged("34나 567")
        controller.submitPlate()
        assertEquals(SettingsStep.PlateError("34나 567"), controller.step.value)
        controller.fixPlate()
        assertEquals(SettingsStep.PlateInput("34나 567"), controller.step.value)
        assertEquals("12가3456", settings.settings.value.plateNumber)
    }

    @Test fun `이용 안내에서 이전으로는 설정`() {
        controller.openLegal()
        assertEquals(SettingsStep.Legal, controller.step.value)
        controller.back()
        assertEquals(SettingsStep.Main, controller.step.value)
    }
}
