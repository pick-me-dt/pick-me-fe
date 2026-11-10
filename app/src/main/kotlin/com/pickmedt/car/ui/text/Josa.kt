package com.pickmedt.car.ui.text

/** 한국어 조사 붙이기 */
object Josa {
    private const val HANGUL_START = 0xAC00
    private const val HANGUL_END = 0xD7A3
    private const val JONGSEONG_COUNT = 28
    private const val RIEUL = 8

    /** 받침이 있으면 "으로", 없거나 ㄹ 받침이거나 한글이 아니면 "로". 예: "양재 DT점으로", "서울로" */
    fun euro(word: String): String {
        val jongseong = jongseong(word) ?: return word + "로"
        return word + if (jongseong == 0 || jongseong == RIEUL) "로" else "으로"
    }

    /** 받침이 있으면 "을", 없거나 한글이 아니면 "를". 예: "아이스 아메리카노를" */
    fun eulReul(word: String): String {
        val jongseong = jongseong(word) ?: return word + "를"
        return word + if (jongseong == 0) "를" else "을"
    }

    /** 마지막 글자의 받침 번호(0 = 받침 없음). 한글이 아니면 null */
    private fun jongseong(word: String): Int? {
        val last = word.lastOrNull()?.code ?: return null
        if (last !in HANGUL_START..HANGUL_END) return null
        return (last - HANGUL_START) % JONGSEONG_COUNT
    }
}
