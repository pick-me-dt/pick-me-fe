package com.pickmedt.car.data.local

import android.content.Context
import java.util.UUID

/** 서버 기기 로그인(POST /v1/auth/device)에 쓰는 기기 ID. 설치마다 한 번 만들고 유지한다 */
object DeviceIdentity {
    private const val PREFS = "pickme_device"
    private const val KEY = "device_id"

    fun id(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY, null) ?: "car-${UUID.randomUUID()}".also { prefs.edit().putString(KEY, it).apply() }
    }
}
