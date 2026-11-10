package com.pickmedt.car.feature.settings

import com.pickmedt.car.core.rule.PlateNumber
import com.pickmedt.car.data.settings.UserSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 설정 단계 (Figma V08). 차량번호 변경은 첫 이용의 차량번호 화면(V00-I·IE)을 다시 쓴다 */
sealed interface SettingsStep {
    /** V08 */
    data object Main : SettingsStep

    /** V08-L */
    data object Legal : SettingsStep

    data class PlateInput(val text: String) : SettingsStep

    data class PlateError(val text: String) : SettingsStep
}

class SettingsController(private val settings: UserSettingsRepository) {
    private val state = MutableStateFlow<SettingsStep>(SettingsStep.Main)
    val step: StateFlow<SettingsStep> = state.asStateFlow()

    fun open() {
        state.value = SettingsStep.Main
    }

    /** V08 「차량번호 · …」. 등록한 번호를 표시 형식으로 채워 둔다 */
    fun editPlate() {
        val plate = settings.settings.value.plateNumber
        state.value = SettingsStep.PlateInput(plate?.let(PlateNumber::display).orEmpty())
    }

    fun plateChanged(text: String) {
        if (state.value is SettingsStep.PlateInput) state.value = SettingsStep.PlateInput(text)
    }

    fun submitPlate() {
        val input = state.value as? SettingsStep.PlateInput ?: return
        val plate = PlateNumber.normalize(input.text)
        if (plate == null) {
            state.value = SettingsStep.PlateError(input.text)
            return
        }
        settings.registerPlate(plate)
        state.value = SettingsStep.Main
    }

    fun fixPlate() {
        val error = state.value as? SettingsStep.PlateError ?: return
        state.value = SettingsStep.PlateInput(error.text)
    }

    /** V08 「이용 안내·정보 처리」 */
    fun openLegal() {
        state.value = SettingsStep.Legal
    }

    /** V08-L·차량번호 화면의 「이전으로」 → V08 */
    fun back() {
        state.value = SettingsStep.Main
    }
}
