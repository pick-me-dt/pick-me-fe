package com.pickmedt.car.data.remote

import com.pickmedt.car.core.model.CancelReason
import com.pickmedt.car.core.model.OrderStage
import kotlinx.serialization.json.decodeFromJsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * 2차 기획 서버 주문 → 앱 주문 (pick-me-be 1ef2b1c order-view.ts).
 * 본문은 로컬 서버(docker compose)에서 받은 실제 응답을 줄인 것이다.
 */
class RemoteOrderMappingTest {
    private val json = HttpJson("http://localhost", token = { null }).json

    private fun order(status: String, extra: String = "", timing: String = NO_ETA): OrderDto = json.decodeFromJsonElement(
        json.parseToJsonElement(
            """
            {"id":"o1","orderNo":"P-021","status":"$status","source":"SCREEN",
             "store":{"id":"s1","name":"픽미 양재 DT점","lat":37.4705,"lng":127.035},
             "items":[{"menuItemId":"m1","name":"아이스 아메리카노","unitPrice":4500,"qty":1}],
             "totalAmount":4500,"plateNumber":"12가3456","timing":$timing,"acceptOverdue":false,
             "createdAt":"2026-11-11T00:14:16.729Z","cancelRequest":null $extra}
            """.trimIndent(),
        ),
    )

    @Test fun `서버 상태 → 주문 단계`() {
        assertEquals(OrderStage.AWAITING_STORE, stageOf("PENDING_ACCEPT"))
        assertEquals(OrderStage.WAITING_TO_PREPARE, stageOf("ACCEPTED"))
        assertEquals(OrderStage.PREPARING, stageOf("PREPARING"))
        assertEquals(OrderStage.READY, stageOf("READY"))
        assertEquals(OrderStage.PICKED_UP, stageOf("PICKED_UP"))
        assertEquals(OrderStage.CANCEL_REQUESTED, stageOf("CANCEL_REQUESTED"))
        assertEquals(OrderStage.CANCELLED, stageOf("REJECTED"))
        assertEquals(OrderStage.CANCELLED, stageOf("CANCELLED"))
    }

    @Test fun `예약 직후 주문번호는 서버 값, ETA 전에는 제조 타이밍이 없다`() {
        val order = order("PENDING_ACCEPT").toOrder()
        assertEquals("P-021", order.displayNo)
        assertEquals(OrderStage.AWAITING_STORE, order.stage)
        assertNull(order.timing)
        assertEquals(Instant.parse("2026-11-11T00:14:16.729Z"), order.requestedAt)
        assertEquals(false, order.acceptOverdue)
    }

    @Test fun `ETA를 받은 뒤 제조 타이밍은 서버 계산을 그대로 쓴다`() {
        val timing = order("ACCEPTED", timing = WITH_ETA).toOrder().timing!!
        assertEquals(Instant.parse("2026-11-11T00:19:16.747Z"), timing.entranceEtaAt)
        assertEquals(Instant.parse("2026-11-11T00:21:16.747Z"), timing.expectedPickupAt)
        assertEquals(Instant.parse("2026-11-11T00:18:16.747Z"), timing.recommendedStartAt)
        assertEquals(Instant.parse("2026-11-11T00:14:16.747Z"), timing.etaReceivedAt)
        assertEquals(0, timing.delaySec)
    }

    @Test fun `매장 거절은 사유 문구와 함께 STORE_REJECTED`() {
        val order = order("REJECTED", """, "rejectedAt":"2026-11-11T00:15:00Z", "rejectReason":"BUSY"""").toOrder()
        assertEquals(OrderStage.CANCELLED, order.stage)
        assertEquals(CancelReason.STORE_REJECTED, order.cancelReason)
        assertEquals("주문량 증가", order.cancelNote)
        assertEquals(Instant.parse("2026-11-11T00:15:00Z"), order.cancelledAt)
    }

    @Test fun `운전자 취소 요청을 매장이 승인하면 CANCEL_APPROVED`() {
        val approved = order(
            "CANCELLED",
            """, "cancelledAt":"2026-11-11T00:16:00Z", "cancelReason":"USER",
               "cancelRequest":{"requestedAt":"2026-11-11T00:15:30Z","note":null,"statusBefore":"ACCEPTED"}""",
        ).toOrder()
        assertEquals(CancelReason.CANCEL_APPROVED, approved.cancelReason)
        assertNull(approved.cancelNote)
    }

    @Test fun `수락 전 취소와 매장 취소는 서버 안내 문구`() {
        val beforeAccept = order("CANCELLED", """, "cancelledAt":"2026-11-11T00:16:00Z", "cancelReason":"USER"""").toOrder()
        assertEquals(CancelReason.OTHER, beforeAccept.cancelReason)
        assertEquals("주문이 취소됐어요.", beforeAccept.cancelNote)

        val noShow = order("CANCELLED", """, "cancelledAt":"2026-11-11T00:16:00Z", "cancelReason":"NO_SHOW"""").toOrder()
        assertEquals("매장에서 미수령으로 주문을 종료했어요.", noShow.cancelNote)
    }

    @Test fun `취소 확인 중에서 진행 상태로 돌아오면 취소 거절`() {
        assertTrue(cancelRejectedAfter(OrderStage.CANCEL_REQUESTED, OrderStage.WAITING_TO_PREPARE, rejectedBefore = false))
        assertTrue(cancelRejectedAfter(OrderStage.CANCEL_REQUESTED, OrderStage.PREPARING, rejectedBefore = false))
        // 승인돼 취소되면 거절이 아니다
        assertFalse(cancelRejectedAfter(OrderStage.CANCEL_REQUESTED, OrderStage.CANCELLED, rejectedBefore = false))
        // 거절 뒤에는 다음 단계로 넘어가도 유지, 다시 취소를 요청하면 지운다
        assertTrue(cancelRejectedAfter(OrderStage.WAITING_TO_PREPARE, OrderStage.PREPARING, rejectedBefore = true))
        assertFalse(cancelRejectedAfter(OrderStage.WAITING_TO_PREPARE, OrderStage.CANCEL_REQUESTED, rejectedBefore = true))
        assertFalse(cancelRejectedAfter(null, OrderStage.AWAITING_STORE, rejectedBefore = false))
    }

    @Test fun `예약 409 본문`() {
        val conflict = json.decodeFromJsonElement<ReserveConflictDto>(
            json.parseToJsonElement(
                """
                {"code":"PRICE_CHANGED","message":"가격이 바뀌었습니다. 새 금액을 확인해 주세요",
                 "quote":{"storeId":"s1","storeName":"픽미 양재 DT점",
                  "items":[{"menuItemId":"m1","name":"아이스 아메리카노","unitPrice":4500,"qty":1,"soldOut":false}],
                  "totalAmount":4500,"available":true,"prepSec":180,"afterEntrySec":120}}
                """.trimIndent(),
            ),
        )
        assertEquals("PRICE_CHANGED", conflict.code)
        assertEquals(4500, conflict.quote!!.totalAmount)
    }

    private companion object {
        const val NO_ETA = """{"etaSec":null,"etaUpdatedAt":null,"etaStale":false,"entranceArrivalAt":null,"expectedPickupAt":null,"recommendedStartAt":null,"prepSec":180,"afterEntrySec":120,"delaySec":0}"""
        const val WITH_ETA = """{"etaSec":300,"etaUpdatedAt":"2026-11-11T00:14:16.747Z","etaStale":false,"entranceArrivalAt":"2026-11-11T00:19:16.747Z","expectedPickupAt":"2026-11-11T00:21:16.747Z","recommendedStartAt":"2026-11-11T00:18:16.747Z","prepSec":180,"afterEntrySec":120,"delaySec":0}"""
    }
}
