package com.pickmedt.car.data.remote

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * 시연·테스트용 서버 조작 (pick-me-be 2차).
 * - 개발 API(/v1/dev, 서버가 ENABLE_DEV_ROUTES=true일 때만): 단계 넘기기·초기화·시드 직원 토큰
 * - 매장 API(/v1/store): 시드 직원 토큰으로 매장 대시보드와 같은 조작(수락·거절·제조 시작·품절·접수 종료 등)
 */
class RemoteDevTools(private val session: RemoteSession) {
    private val staffTokens = mutableMapOf<String, String>()
    private val lock = Mutex()

    /** PENDING_ACCEPT → ACCEPTED → PREPARING → READY → PICKED_UP 한 단계, CANCEL_REQUESTED는 승인 (dev-fixtures.service.ts advance) */
    suspend fun advance(orderId: String) {
        session.authorized { http -> http.post("/v1/dev/orders/$orderId/advance") }
    }

    /** 진행 중 주문 취소, 시뮬레이션 중지, 목데이터(위치·준비 시간·품절·영업) 복구, 양재 DT점 다음 주문번호 P-021 */
    suspend fun reset() {
        session.authorized { http -> http.post("/v1/dev/reset") }
        lock.withLock { staffTokens.clear() }
    }

    /** 매장 주문 조작. [action]은 accept·start·ready·pickup·cancel-request/approve·cancel-request/reject 등 */
    suspend fun storeOrder(storeId: String, orderId: String, action: String, body: Map<String, String> = emptyMap()) {
        staff(storeId).post("/v1/store/orders/$orderId/$action", JsonObject(body.mapValues { JsonPrimitive(it.value) }))
    }

    /** 메뉴 품절 설정 (PATCH /v1/store/menu/:menuItemId) */
    suspend fun setSoldOut(storeId: String, menuItemId: String, soldOut: Boolean) {
        staff(storeId).patch("/v1/store/menu/$menuItemId", JsonObject(mapOf("soldOut" to JsonPrimitive(soldOut))))
    }

    /** 매장 메뉴 ID 전체 (GET /v1/store/menu) */
    suspend fun storeMenuIds(storeId: String): List<String> {
        val http = staff(storeId)
        return http.json.decodeFromJsonElement<List<StoreMenuItemDto>>(http.get("/v1/store/menu")).map { it.id }
    }

    /** 영업 여부 (PATCH /v1/store/settings isOpen) */
    suspend fun setOpen(storeId: String, open: Boolean) {
        staff(storeId).patch("/v1/store/settings", JsonObject(mapOf("isOpen" to JsonPrimitive(open))))
    }

    private suspend fun staff(storeId: String): HttpJson {
        val token = lock.withLock {
            staffTokens[storeId] ?: run {
                val response = session.http.json.decodeFromJsonElement<SeedTokensDto>(session.http.post("/v1/dev/tokens"))
                response.staff.forEach { staffTokens[it.storeId] = it.token }
                checkNotNull(staffTokens[storeId]) { "이 매장의 시드 직원 계정이 없어요: $storeId" }
            }
        }
        return HttpJson(session.baseUrl, token = { token })
    }
}

@Serializable
private data class SeedStaffDto(val storeId: String, val token: String)

@Serializable
private data class SeedTokensDto(val staff: List<SeedStaffDto>)

@Serializable
private data class StoreMenuItemDto(val id: String)
