package com.pickmedt.car.data.remote

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

/**
 * 차량 기기 로그인 (POST /v1/auth/device). 처음 보는 기기면 서버가 사용자·차량을 새로 만든다.
 * 토큰은 메모리에만 두고, 401이 오면 다시 로그인한다.
 */
class RemoteSession(
    val baseUrl: String,
    private val deviceId: String,
    private val plateNumber: () -> String?,
) {
    @Volatile private var token: String? = null
    private val lock = Mutex()

    /** 토큰 없이 부르는 로그인 전용 클라이언트 */
    private val anonymous = HttpJson(baseUrl, token = { null })

    /** 토큰을 붙이는 클라이언트. 401이면 [authorized]가 다시 로그인한다 */
    val http = HttpJson(baseUrl, token = { token })

    /** 차량번호를 등록해야 로그인할 수 있다 (첫 이용 V00-I 전에는 불가) */
    val canLogin: Boolean get() = plateNumber() != null

    suspend fun ensureLoggedIn(): String = lock.withLock {
        token ?: login().also { token = it }
    }

    /** 로그인한 상태로 호출하고, 토큰이 만료·무효면 한 번 다시 로그인해 재시도한다 */
    suspend fun <T> authorized(block: suspend (HttpJson) -> T): T {
        ensureLoggedIn()
        return try {
            block(http)
        } catch (e: ApiException) {
            if (e.status != 401) throw e
            lock.withLock { token = login() }
            block(http)
        }
    }

    private suspend fun login(): String {
        val plate = checkNotNull(plateNumber()) { "차량번호를 등록해야 서버에 로그인할 수 있어요" }
        val body = anonymous.json.encodeToJsonElement(RegisterDeviceRequest(deviceId, plate))
        val response = anonymous.post("/v1/auth/device", body)
        return anonymous.json.decodeFromJsonElement<DriverTokenDto>(response).accessToken
    }

    val currentToken: String? get() = token
}
