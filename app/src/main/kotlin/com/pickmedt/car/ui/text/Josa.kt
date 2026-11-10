package com.pickmedt.car.ui.text

/** 한국어 조사 붙이기 */
object Josa {
    private const val HANGUL_START = 0xAC00
    private const val HANGUL_END = 0xD7A3
    private const val JONGSEONG_COUNT = 28
    private const val RIEUL = 8

    /** 받침이 있으면 "으로", 없거나 ㄹ 받침이거나 한글이 아니면 "로". 예: "양재 DT점으로", "서울로" */
    fun euro(word: String): String {
        val last = word.lastOrNull()?.code ?: return word
        if (last !in HANGUL_START..HANGUL_END) return word + "로"
        val jongseong = (last - HANGUL_START) % JONGSEONG_COUNT
        return word + if (jongseong == 0 || jongseong == RIEUL) "로" else "으로"
    }
}
