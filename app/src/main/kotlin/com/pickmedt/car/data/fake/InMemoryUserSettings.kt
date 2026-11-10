package com.pickmedt.car.data.fake

import com.pickmedt.car.data.settings.UserSettings
import com.pickmedt.car.data.settings.UserSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 테스트용 메모리 설정 */
class InMemoryUserSettings(initial: UserSettings = UserSettings()) : UserSettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: StateFlow<UserSettings> = state.asStateFlow()

    override fun agreeConsent() {
        state.value = state.value.copy(consentAgreed = true)
    }

    override fun registerPlate(plate: String) {
        state.value = state.value.copy(plateNumber = plate)
    }

    override fun reset() {
        state.value = UserSettings()
    }
}
