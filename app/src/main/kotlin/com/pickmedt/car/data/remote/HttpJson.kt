package com.pickmedt.car.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** 서버가 2xx가 아닌 응답을 준 경우. [code]는 서버 오류 코드(있으면) */
class ApiException(val status: Int, val code: String?, message: String) : IOException("HTTP $status ${code.orEmpty()} $message".trim())

/** 픽미 서버 REST 호출. JSON 본문을 주고받고, 토큰이 있으면 Bearer로 붙인다 */
class HttpJson(
    private val baseUrl: String,
    private val token: () -> String?,
    private val client: OkHttpClient = defaultClient(),
) {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    suspend fun get(path: String): JsonElement = call("GET", path, body = null)

    suspend fun post(path: String, body: JsonElement? = null, headers: Map<String, String> = emptyMap()): JsonElement =
        call("POST", path, body, headers)

    suspend fun patch(path: String, body: JsonElement): JsonElement = call("PATCH", path, body)

    private suspend fun call(
        method: String,
        path: String,
        body: JsonElement?,
        headers: Map<String, String> = emptyMap(),
    ): JsonElement = withContext(Dispatchers.IO) {
        val requestBody = when {
            body != null -> json.encodeToString(JsonElement.serializer(), body).toRequestBody(JSON)
            method == "POST" -> "{}".toRequestBody(JSON)
            else -> null
        }
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + path)
            .method(method, requestBody)
            .apply {
                token()?.let { header("Authorization", "Bearer $it") }
                headers.forEach { (name, value) -> header(name, value) }
            }
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw toApiException(response.code, text)
            if (text.isBlank()) JsonObject(emptyMap()) else json.parseToJsonElement(text)
        }
    }

    private fun toApiException(status: Int, text: String): ApiException {
        val obj = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()
        val code = obj?.get("code")?.jsonPrimitive?.contentOrNull
        val message = obj?.get("message")?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else it.toString() }
        return ApiException(status, code, message ?: text.take(200))
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}
