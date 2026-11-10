package com.pickmedt.car.data.remote

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URI

/**
 * 차량용 실시간 연결 (Socket.IO /ws/vehicle, 토큰은 handshake auth.token. pick-me-be src/realtime/realtime.gateway.ts:7).
 * 이벤트 내용은 쓰지 않고, 주문이 바뀌었다는 신호로만 받아 서버 상태를 다시 조회한다 (2차 기획 §3: 서버 상태가 기준).
 */
class RealtimeClient(
    private val baseUrl: String,
    private val onOrderEvent: (event: String) -> Unit,
    private val onConnected: () -> Unit,
) {
    private var socket: Socket? = null

    fun connect(token: String) {
        if (socket != null) return
        val options = IO.Options.builder()
            .setAuth(mapOf("token" to token))
            .setTransports(arrayOf("websocket"))
            .setReconnection(true)
            .build()
        val created = IO.socket(URI.create(baseUrl.trimEnd('/') + "/ws/vehicle"), options)
        created.on(Socket.EVENT_CONNECT) {
            Log.i(TAG, "연결됨")
            onConnected()
        }
        created.on(Socket.EVENT_DISCONNECT) { Log.i(TAG, "연결 끊김 ${it.firstOrNull()}") }
        created.on(Socket.EVENT_CONNECT_ERROR) { Log.w(TAG, "연결 실패 ${it.firstOrNull()}") }
        VEHICLE_EVENTS.forEach { name ->
            created.on(name) {
                Log.i(TAG, "이벤트 $name ${it.firstOrNull()}")
                onOrderEvent(name)
            }
        }
        created.connect()
        socket = created
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
    }

    private companion object {
        const val TAG = "PickmeRealtime"

        /** 차량에 오는 이벤트 (pick-me-be src/realtime/realtime.service.ts:17-40, docs/LOCAL_DEV.md 6장) */
        val VEHICLE_EVENTS = listOf("order.status_changed", "order.delayed", "queue.alert")
    }
}
