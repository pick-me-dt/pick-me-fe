package com.pickmedt.car.feature.progress

import com.pickmedt.car.ui.component.InfoBoxText
import com.pickmedt.car.ui.component.PleosButtonStyle
import com.pickmedt.car.ui.component.SummaryCardText

/** Figma 화면 코드와 대응하는 진행 화면 종류. 주문 단계에 따라 자동으로 바뀌는 화면만 여기 둔다 */
enum class ProgressVariant {
    /** V03 매장 확인 중 */
    AWAITING_STORE,
    /** V03-T 응답 지연 */
    STORE_SLOW,
    /** V03-R 예약 거절 */
    REJECTED,
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
    /** V04-E 연결·ETA 오류 */
    ETA_LOST,
    /** V07 수령 완료 */
    PICKED_UP,
    /** V06-C 취소 확인 중 */
    CANCEL_PENDING,
    /** V06-D 취소 완료 */
    CANCELLED,
    /** V06-E 취소 거절 */
    CANCEL_REJECTED,
}

data class ProgressAction(val label: String, val style: PleosButtonStyle, val kind: Kind) {
    enum class Kind {
        NONE,
        /** 주문 상세 (V06-A) */
        DETAIL,
        /** 끝난 주문을 닫고 홈으로 */
        FINISH,
        /** 서버 상태 다시 조회 */
        REFRESH,
        /** 다른 매장 선택 (V01-M, 다음 화면 계획) */
        CHOOSE_OTHER_STORE,
        /** 취소 거절 안내를 닫고 현재 주문으로 */
        ACK_CANCEL_REJECTION,
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

    /** 하단 버튼. 위에서 아래 순서 */
    val actions: List<ProgressAction>

    /** 카드형 (V03·V03-R·V03-T·V04-E·V06-C·D·E·V07) */
    data class Card(
        override val variant: ProgressVariant,
        val eyebrow: String,
        val title: String,
        val card: SummaryCardText,
        val lead: String,
        val note: String?,
        override val actions: List<ProgressAction>,
        val info: InfoBoxText? = null,
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
        override val actions: List<ProgressAction>,
    ) : ProgressUiState
}
