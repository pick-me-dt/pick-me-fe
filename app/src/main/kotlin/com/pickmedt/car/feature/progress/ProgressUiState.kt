package com.pickmedt.car.feature.progress

import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.ui.component.PleosButtonStyle

/** Figma 화면 코드와 대응하는 진행 화면 종류 */
enum class ProgressVariant {
    /** V03 매장 확인 중 */
    AWAITING_STORE,
    /** V04-A 제조 대기 */
    WAITING,
    /** V05-A 제조 전 지연 */
    WAITING_DELAYED,
    /** V04-B 제조 중 */
    PREPARING,
    /** V05-B 제조 후 지연 */
    PREPARING_DELAYED,
    /** V04-C 준비 완료 */
    READY,
    /** V07 수령 완료 */
    PICKED_UP,
    /** 아직 화면을 만들지 않은 단계 (취소 확인 중·취소 완료) */
    OTHER,
}

data class SummaryCardText(val title: String, val menu: String, val detail: String)

data class ProgressAction(val label: String, val style: PleosButtonStyle, val kind: Kind) {
    enum class Kind {
        NONE,
        /** 주문 상세 (V06) */
        DETAIL,
        /** 끝난 주문을 닫고 홈으로 */
        FINISH,
    }
}

/** 도착 영역 큰 글자 */
sealed interface ArrivalHeadline {
    /** "매장 도착까지 6분" + 도착 시각 줄. largeLine이면 28, 아니면 24 크기 (Figma V04-A만 28) */
    data class Eta(val minutes: String, val line: String, val largeLine: Boolean) : ArrivalHeadline

    /** "픽업 주문번호 P-021" + 안내 (V04-C) */
    data class PickupNumber(val number: String, val guide: String) : ArrivalHeadline
}

sealed interface ProgressUiState {
    val variant: ProgressVariant
    val action: ProgressAction

    /** 주문 요약 카드형 (V03, V07) */
    data class Card(
        override val variant: ProgressVariant,
        val eyebrow: String,
        val title: String,
        val card: SummaryCardText,
        val lead: String,
        val note: String,
        override val action: ProgressAction,
    ) : ProgressUiState

    /** 도착 시간·단계형 (V04-A·B·C, V05-A·B) */
    data class Arrival(
        override val variant: ProgressVariant,
        val movingTo: String,
        val title: String,
        val headline: ArrivalHeadline,
        val step: Int,
        val menuLine: String,
        val orderLine: String,
        override val action: ProgressAction,
    ) : ProgressUiState

    /** 화면을 만들기 전인 단계. 다음 화면 계획(V03-R·V06)에서 바꾼다 */
    data class Other(
        val stage: OrderStage,
        val message: String,
        override val action: ProgressAction,
    ) : ProgressUiState {
        override val variant: ProgressVariant get() = ProgressVariant.OTHER
    }
}
