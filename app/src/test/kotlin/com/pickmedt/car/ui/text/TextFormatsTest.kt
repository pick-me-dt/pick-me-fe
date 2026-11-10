package com.pickmedt.car.ui.text

import com.pickmedt.car.core.model.Money
import com.pickmedt.car.testing.kst
import com.pickmedt.car.testing.testLine
import com.pickmedt.car.testing.testStore
import org.junit.Assert.assertEquals
import org.junit.Test

class TextFormatsTest {
    @Test fun `받침이 있으면 으로`() = assertEquals("양재 DT점으로", Josa.euro("양재 DT점"))

    @Test fun `받침이 없으면 로`() = assertEquals("판교로", Josa.euro("판교"))

    @Test fun `ㄹ 받침이면 로`() = assertEquals("서울로", Josa.euro("서울"))

    @Test fun `한글이 아니면 로`() = assertEquals("Pickme로", Josa.euro("Pickme"))

    @Test fun `받침이 없으면 를`() = assertEquals("아이스 아메리카노를", Josa.eulReul("아이스 아메리카노"))

    @Test fun `받침이 있으면 을`() = assertEquals("햄치즈 샌드위치를", Josa.eulReul("햄치즈 샌드위치"))

    @Test fun `ㄹ 받침도 을`() = assertEquals("콜드브루 말을", Josa.eulReul("콜드브루 말"))

    @Test fun `분은 올림한다`() {
        assertEquals(6, Formats.etaMinutes(360))
        assertEquals(6, Formats.etaMinutes(359))
        assertEquals(2, Formats.etaMinutes(61))
        assertEquals(0, Formats.etaMinutes(0))
        assertEquals(0, Formats.etaMinutes(-30))
    }

    @Test fun `시각은 한국 시간 HH mm`() = assertEquals("08:08", Formats.clock(kst("08:08")))

    @Test fun `메뉴 줄`() = assertEquals("아이스 아메리카노 1잔", Formats.menuLine(listOf(testLine)))

    @Test fun `옵션과 금액 줄`() =
        assertEquals("Regular · 4,500원", Formats.optionPriceLine(listOf(testLine), Money(4_500)))

    @Test fun `홈 정보 줄은 가운뎃점 앞뒤 두 칸`() =
        assertEquals("양재 DT점  ·  Regular  ·  4,500원", Formats.homeInfoLine(testStore, listOf(testLine), Money(4_500)))

    @Test fun `주문 줄`() = assertEquals("Regular  ·  주문 P-021", Formats.orderLine(listOf(testLine), "P-021"))

    @Test fun `매장 줄`() = assertEquals("픽미 커피 · 양재 DT점", Formats.storeLine(testStore))

    @Test fun `단계 이름은 단계 표시와 같은 말을 쓴다`() {
        assertEquals("매장 확인 중", Formats.stageLabel(com.pickmedt.car.core.model.OrderStage.AWAITING_STORE))
        assertEquals("제조 대기", Formats.stageLabel(com.pickmedt.car.core.model.OrderStage.WAITING_TO_PREPARE))
        assertEquals("제조 중", Formats.stageLabel(com.pickmedt.car.core.model.OrderStage.PREPARING))
        assertEquals("준비 완료", Formats.stageLabel(com.pickmedt.car.core.model.OrderStage.READY))
    }
}
