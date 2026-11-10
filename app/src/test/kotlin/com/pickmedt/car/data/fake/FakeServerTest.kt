package com.pickmedt.car.data.fake

import com.pickmedt.car.core.model.CancelReason
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.testing.MutableClock
import com.pickmedt.car.testing.kst
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class FakeServerTest {
    private val clock = MutableClock(kst("08:02"))
    private val server = FakeServer(clock)
    private val usual = SampleData.usualIcedAmericano.id

    private fun reserved() = server.reserve(usual, "key-1")

    private fun stage() = server.order.value?.stage

    @Test fun `예약하면 매장 확인 중이고 주문번호는 P-021`() {
        val order = reserved()
        assertEquals(OrderStage.AWAITING_STORE, order.stage)
        assertEquals("P-021", order.displayNo)
        assertEquals("4,500원", order.total.format())
    }

    @Test fun `같은 요청 키로 다시 예약하면 같은 주문`() {
        val first = reserved()
        val second = server.reserve(usual, "key-1")
        assertEquals(first.id, second.id)
    }

    @Test(expected = IllegalStateException::class)
    fun `진행 중 주문이 있으면 다른 키로 예약할 수 없다`() {
        reserved()
        server.reserve(usual, "key-2")
    }

    @Test fun `직원이 수락하면 제조 대기`() {
        reserved()
        server.accept()
        assertEquals(OrderStage.WAITING_TO_PREPARE, stage())
    }

    @Test fun `2차 기획 예시 - 08시02분 ETA 6분이면 입구 08시08분 수령 08시10분 추천 08시07분`() {
        val order = reserved()
        server.accept()
        server.reportEta(order.id, etaSec = 360)
        val timing = server.order.value!!.timing!!
        assertEquals(kst("08:08"), timing.entranceEtaAt)
        assertEquals(kst("08:10"), timing.expectedPickupAt)
        assertEquals(kst("08:07"), timing.recommendedStartAt)
        assertEquals(0, timing.delaySec)
        assertEquals(kst("08:02"), timing.etaReceivedAt)
    }

    @Test fun `제조 전 지연 - ETA 10분이면 추천이 08시11분으로 늦춰진다`() {
        val order = reserved()
        server.accept()
        server.reportEta(order.id, etaSec = 360)
        server.reportEta(order.id, etaSec = 600)
        assertEquals(kst("08:11"), server.order.value!!.timing!!.recommendedStartAt)
    }

    @Test fun `제조 후 지연 - 추천은 고정되고 늦어진 시간만 공유한다`() {
        val order = reserved()
        server.accept()
        server.reportEta(order.id, etaSec = 360)
        server.startPreparing()
        clock.advance(Duration.ofMinutes(3))
        server.reportEta(order.id, etaSec = 300)
        val timing = server.order.value!!.timing!!
        assertEquals(kst("08:07"), timing.recommendedStartAt)
        assertEquals(kst("08:12"), timing.expectedPickupAt)
        assertEquals(120, timing.delaySec)
    }

    @Test(expected = IllegalStateException::class)
    fun `수락 없이 제조를 시작할 수 없다`() {
        reserved()
        server.startPreparing()
    }

    @Test fun `준비 완료 후 수령하면 수령 완료`() {
        reserved()
        server.accept()
        server.startPreparing()
        server.markReady()
        server.pickUp()
        assertEquals(OrderStage.PICKED_UP, stage())
        assertEquals(kst("08:02"), server.order.value!!.pickedUpAt)
    }

    @Test fun `매장이 거절하면 취소 완료이고 사유는 매장 거절`() {
        reserved()
        server.reject()
        assertEquals(OrderStage.CANCELLED, stage())
        assertEquals(CancelReason.STORE_REJECTED, server.order.value!!.cancelReason)
    }

    @Test fun `취소 요청 후 승인되면 취소 완료`() {
        val order = reserved()
        server.accept()
        server.requestCancel(order.id)
        assertEquals(OrderStage.CANCEL_REQUESTED, stage())
        server.approveCancel()
        assertEquals(OrderStage.CANCELLED, stage())
        assertEquals(CancelReason.CANCEL_APPROVED, server.order.value!!.cancelReason)
    }

    @Test fun `취소 요청이 거절되면 요청 전 단계로 돌아간다`() {
        val order = reserved()
        server.accept()
        server.startPreparing()
        server.requestCancel(order.id)
        server.rejectCancel()
        assertEquals(OrderStage.PREPARING, stage())
        assertTrue(server.order.value!!.cancelRejected)
    }

    @Test fun `수령 후에는 새 주문을 받을 수 있고 번호가 이어진다`() {
        val first = reserved()
        server.accept(); server.startPreparing(); server.markReady(); server.pickUp()
        val second = server.reserve(usual, "key-2")
        assertNotEquals(first.id, second.id)
        assertEquals("P-022", second.displayNo)
    }

    @Test fun `초기화하면 주문이 없다`() {
        reserved()
        server.reset()
        assertNull(server.order.value)
    }
}
