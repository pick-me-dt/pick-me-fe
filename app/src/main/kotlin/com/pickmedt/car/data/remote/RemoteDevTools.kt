package com.pickmedt.car.data.remote

/**
 * 시연·테스트용 서버 개발 API (/v1/dev, 서버가 ENABLE_DEV_ROUTES=true일 때만).
 * 1차 기획 서버에는 직원 수락이 없고, 예약 주문은 ETA 조건이 맞아야 매장에 전송된다.
 * 그래서 「매장 수락」 대신 다음 단계로 넘기는 개발 API를 쓴다.
 */
class RemoteDevTools(private val session: RemoteSession) {
    /** RESERVED → SENT → PREPARING → READY → PICKED_UP 한 단계 */
    suspend fun advance(orderId: String) {
        session.authorized { http -> http.post("/v1/dev/orders/$orderId/advance") }
    }

    /** 진행 중 주문 취소, 시뮬레이션 중지, 목데이터 복구 */
    suspend fun reset() {
        session.authorized { http -> http.post("/v1/dev/reset") }
    }
}
