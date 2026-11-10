package com.pickmedt.car.core.rule

/**
 * 차량번호 형식 (Figma V00-I·V00-IE).
 * 공백을 지운 뒤 「숫자 2~3자리 + 한글 1자 + 숫자 4자리」만 받는다. 예: "12가3456", "123가4567"
 */
object PlateNumber {
    private val Pattern = Regex("^(\\d{2,3})([가-힣])(\\d{4})$")

    /** 저장할 형식(공백 없음)으로 바꾼다. 형식이 틀리면 null */
    fun normalize(input: String): String? {
        val compact = input.filterNot { it.isWhitespace() }
        return if (Pattern.matches(compact)) compact else null
    }

    /** 화면 표시 형식. 뒤 4자리 앞에 한 칸. 예: "12가 3456" */
    fun display(plate: String): String {
        val match = Pattern.matchEntire(plate) ?: return plate
        val (front, hangul, back) = match.destructured
        return "$front$hangul $back"
    }
}
