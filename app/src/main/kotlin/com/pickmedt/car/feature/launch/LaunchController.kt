package com.pickmedt.car.feature.launch

import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.rule.PlateNumber
import com.pickmedt.car.data.order.OrderRepository
import com.pickmedt.car.data.settings.UserSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConsentItem { PERSONAL_INFO, LOCATION }

/** 앱 시작·첫 이용 단계 (Figma V00). Done이면 홈 또는 진행 화면으로 넘어간다 */
sealed interface LaunchStep {
    /** V00-S */
    data object Splash : LaunchStep

    /** V00-L */
    data object Loading : LaunchStep

    /** V00-E */
    data object Failed : LaunchStep

    /** V00-W */
    data object Welcome : LaunchStep

    /** V00-C */
    data class Consent(val personalInfo: Boolean = false, val location: Boolean = false) : LaunchStep {
        val canAgree: Boolean get() = personalInfo && location
    }

    /** V00-P */
    data object AccessBlocked : LaunchStep

    /** V00-I */
    data class PlateInput(val text: String) : LaunchStep

    /** V00-IE */
    data class PlateError(val text: String) : LaunchStep

    /** V00-RE */
    data class ResumeOrder(val order: Order) : LaunchStep

    data object Done : LaunchStep
}

/**
 * V00 흐름. 서버 상태를 먼저 조회하고(2차 기획 §3), 진행 중 주문이 있으면 복귀, 없으면 첫 이용 설정을 확인한다.
 * Figma에 연결 대상이 없는 버튼은 docs/plans/2026-11-10-screens-v01-v07.md 「Figma에 없어 정한 것」을 따른다.
 */
class LaunchController(
    private val settings: UserSettingsRepository,
    private val orders: OrderRepository,
) {
    private val state = MutableStateFlow<LaunchStep>(LaunchStep.Splash)
    val step: StateFlow<LaunchStep> = state.asStateFlow()

    /** 시연용 「첫 실행」. 스플래시부터 다시 */
    fun restart() {
        state.value = LaunchStep.Splash
    }

    /** V00-L → V00-E·V00-RE·V00-W·V00-I·홈. V00-E 「다시 시도」도 여기로 온다 */
    suspend fun load() {
        state.value = LaunchStep.Loading
        if (runCatching { orders.refresh() }.isFailure) {
            state.value = LaunchStep.Failed
            return
        }
        val order = orders.currentOrder.value
        state.value = if (order != null && order.stage.isActive) LaunchStep.ResumeOrder(order) else nextSetupStep()
    }

    /** V00-E 「나중에 확인」, V00-RE 「진행 중 주문 보기」 */
    fun finish() {
        state.value = LaunchStep.Done
    }

    /** V00-W 「시작하기」 */
    fun start() = moveFrom<LaunchStep.Welcome> { LaunchStep.Consent() }

    fun toggleConsent(item: ConsentItem) = moveFrom<LaunchStep.Consent> {
        when (item) {
            ConsentItem.PERSONAL_INFO -> it.copy(personalInfo = !it.personalInfo)
            ConsentItem.LOCATION -> it.copy(location = !it.location)
        }
    }

    /** V00-C 「동의하고 계속」. 두 항목을 모두 선택해야 넘어간다 */
    fun agree() = moveFrom<LaunchStep.Consent> {
        if (!it.canAgree) return@moveFrom it
        settings.agreeConsent()
        nextSetupStep()
    }

    /** V00-C 「동의하지 않음」 */
    fun decline() = moveFrom<LaunchStep.Consent> { LaunchStep.AccessBlocked }

    /** V00-P 「접근 상태 확인」 */
    fun recheckAccess() = moveFrom<LaunchStep.AccessBlocked> { LaunchStep.Consent() }

    /** V00-P 「나중에 설정」 */
    fun later() = moveFrom<LaunchStep.AccessBlocked> { LaunchStep.Welcome }

    fun plateChanged(text: String) = moveFrom<LaunchStep.PlateInput> { LaunchStep.PlateInput(text) }

    /** V00-I 「이 차량으로 등록」 */
    fun submitPlate() = moveFrom<LaunchStep.PlateInput> {
        val plate = PlateNumber.normalize(it.text) ?: return@moveFrom LaunchStep.PlateError(it.text)
        settings.registerPlate(plate)
        LaunchStep.Done
    }

    /** V00-I 「이전으로」 */
    fun backFromPlate() = moveFrom<LaunchStep.PlateInput> { LaunchStep.Welcome }

    /** V00-IE 「입력 수정」 */
    fun fixPlate() = moveFrom<LaunchStep.PlateError> { LaunchStep.PlateInput(it.text) }

    private fun nextSetupStep(): LaunchStep {
        val current = settings.settings.value
        return when {
            !current.consentAgreed -> LaunchStep.Welcome
            current.plateNumber == null -> LaunchStep.PlateInput("")
            else -> LaunchStep.Done
        }
    }

    /** 지금 단계가 [S]일 때만 바꾼다. 다른 화면에서 늦게 들어온 입력은 무시한다 */
    private inline fun <reified S : LaunchStep> moveFrom(next: (S) -> LaunchStep) {
        val current = state.value as? S ?: return
        state.value = next(current)
    }
}
