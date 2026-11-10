package com.pickmedt.car.ui.text

import com.pickmedt.car.core.model.Money
import com.pickmedt.car.core.model.OrderLine
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.core.model.StoreSummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 화면 문구 형식 (Figma 문구 기준) */
object Formats {
    private val Seoul: ZoneId = ZoneId.of("Asia/Seoul")
    private val ClockFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /** 남은 분. 올림한다. 예: 359초 → 6 */
    fun etaMinutes(seconds: Long): Long = if (seconds <= 0) 0 else (seconds + 59) / 60

    /** 예: "08:08" */
    fun clock(at: Instant): String = ClockFormat.format(at.atZone(Seoul))

    /** 예: "아이스 아메리카노 1잔" */
    fun menuLine(lines: List<OrderLine>): String = lines.joinToString(", ") { "${it.name} ${it.qty}${it.unit}" }

    /** 예: "Regular · 4,500원" */
    fun optionPriceLine(lines: List<OrderLine>, total: Money): String =
        listOfNotNull(lines.firstOrNull()?.option, total.format()).joinToString(" · ")

    /** 홈 정보 줄. 가운뎃점 앞뒤 두 칸. 예: "양재 DT점  ·  Regular  ·  4,500원" */
    fun homeInfoLine(store: StoreSummary, lines: List<OrderLine>, total: Money): String =
        listOfNotNull(store.name, lines.firstOrNull()?.option, total.format()).joinToString("  ·  ")

    /** 진행 화면 주문 줄. 예: "Regular  ·  주문 P-021" */
    fun orderLine(lines: List<OrderLine>, displayNo: String?): String =
        listOfNotNull(lines.firstOrNull()?.option, displayNo?.let { "주문 $it" }).joinToString("  ·  ")

    /** 예: "픽미 커피 · 양재 DT점" */
    fun storeLine(store: StoreSummary): String = "${store.brand} · ${store.name}"

    /** 단계 이름. 단계 표시(StepIndicator)와 같은 말을 쓴다 */
    fun stageLabel(stage: OrderStage): String = when (stage) {
        OrderStage.AWAITING_STORE -> "매장 확인 중"
        OrderStage.WAITING_TO_PREPARE -> "제조 대기"
        OrderStage.PREPARING -> "제조 중"
        OrderStage.READY -> "준비 완료"
        OrderStage.PICKED_UP -> "수령 완료"
        OrderStage.CANCEL_REQUESTED -> "취소 확인 중"
        OrderStage.CANCELLED -> "취소 완료"
    }
}
