package com.pickmedt.car.data.remote

import com.pickmedt.car.core.model.CancelReason
import com.pickmedt.car.core.model.OrderStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 1차 기획 서버 상태 → 2차 기획 단계 (pick-me-be prisma/schema.prisma OrderStatus) */
class RemoteOrderMappingTest {
    @Test fun `예약·확인 대기는 매장 확인 중`() {
        assertEquals(OrderStage.AWAITING_STORE, stageOf("RESERVED"))
        assertEquals(OrderStage.AWAITING_STORE, stageOf("NEEDS_CONFIRMATION"))
    }

    @Test fun `매장 전송은 제조 대기, 이후는 같은 이름`() {
        assertEquals(OrderStage.WAITING_TO_PREPARE, stageOf("SENT"))
        assertEquals(OrderStage.PREPARING, stageOf("PREPARING"))
        assertEquals(OrderStage.READY, stageOf("READY"))
        assertEquals(OrderStage.PICKED_UP, stageOf("PICKED_UP"))
        assertEquals(OrderStage.CANCELLED, stageOf("CANCELLED"))
    }

    @Test fun `취소 사유`() {
        assertNull(cancelReasonOf(null))
        assertEquals(CancelReason.CANCEL_APPROVED, cancelReasonOf("USER"))
        assertEquals(CancelReason.STORE_REJECTED, cancelReasonOf("STORE"))
        assertEquals(CancelReason.STORE_REJECTED, cancelReasonOf("STORE_REJECTED"))
        assertEquals(CancelReason.OTHER, cancelReasonOf("EXPIRED"))
        assertEquals(CancelReason.OTHER, cancelReasonOf("TEST_CLEANUP"))
    }
}
