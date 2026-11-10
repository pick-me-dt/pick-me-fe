package com.pickmedt.car.core.rule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlateNumberTest {
    @Test fun `숫자 2자리 한글 숫자 4자리`() = assertEquals("12가3456", PlateNumber.normalize("12가3456"))

    @Test fun `공백은 무시한다`() = assertEquals("12가3456", PlateNumber.normalize(" 12가 3456 "))

    @Test fun `앞 숫자 3자리도 허용`() = assertEquals("123가4567", PlateNumber.normalize("123가4567"))

    @Test fun `뒤 숫자가 4자리가 아니면 오류`() = assertNull(PlateNumber.normalize("12가 345"))

    @Test fun `한글이 없으면 오류`() = assertNull(PlateNumber.normalize("123456"))

    @Test fun `빈 값은 오류`() = assertNull(PlateNumber.normalize(""))

    @Test fun `표시는 뒤 4자리 앞에 한 칸`() = assertEquals("12가 3456", PlateNumber.display("12가3456"))
}
