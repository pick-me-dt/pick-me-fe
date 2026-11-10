package com.pickmedt.car.data.local

import android.content.Context
import com.pickmedt.car.data.settings.UserSettings
import com.pickmedt.car.data.settings.UserSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** SharedPreferences에 동의 여부와 차량번호를 남긴다 */
class PrefsUserSettings(context: Context) : UserSettingsRepository {
    private val prefs = context.getSharedPreferences("pickme_user_settings", Context.MODE_PRIVATE)

    private val state = MutableStateFlow(
        UserSettings(
            consentAgreed = prefs.getBoolean(KEY_CONSENT, false),
            plateNumber = prefs.getString(KEY_PLATE, null),
        ),
    )
    override val settings: StateFlow<UserSettings> = state.asStateFlow()

    override fun agreeConsent() {
        prefs.edit().putBoolean(KEY_CONSENT, true).apply()
        state.value = state.value.copy(consentAgreed = true)
    }

    override fun registerPlate(plate: String) {
        prefs.edit().putString(KEY_PLATE, plate).apply()
        state.value = state.value.copy(plateNumber = plate)
    }

    override fun reset() {
        prefs.edit().clear().apply()
        state.value = UserSettings()
    }

    private companion object {
        const val KEY_CONSENT = "consent_agreed"
        const val KEY_PLATE = "plate_number"
    }
}
