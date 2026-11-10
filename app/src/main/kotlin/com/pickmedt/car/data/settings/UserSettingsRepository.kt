package com.pickmedt.car.data.settings

import kotlinx.coroutines.flow.StateFlow

/** 첫 이용 설정 (Figma V00-C 동의, V00-I 차량번호) */
data class UserSettings(
    val consentAgreed: Boolean = false,
    /** 공백 없는 저장 형식. 예: "12가3456". 등록 전이면 null */
    val plateNumber: String? = null,
)

/**
 * 기기에 남기는 사용자 설정. 앱은 SharedPreferences 구현(data/local), 테스트는 메모리 구현(data/fake)을 쓴다.
 * 서버에 차량번호를 등록하는 API는 실서버 연동(11/13) 때 정한다.
 */
interface UserSettingsRepository {
    val settings: StateFlow<UserSettings>

    fun agreeConsent()

    /** [plate]는 PlateNumber.normalize를 통과한 값 */
    fun registerPlate(plate: String)

    /** 시연용 「첫 실행」. 동의와 차량번호를 지운다 */
    fun reset()
}
