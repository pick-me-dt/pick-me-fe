package com.pickmedt.car.data.order

import com.pickmedt.car.core.model.EtaReading
import com.pickmedt.car.core.model.MenuOption
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.model.QuickOrder
import com.pickmedt.car.core.model.StoreSummary
import kotlinx.coroutines.flow.StateFlow

/**
 * 주문 저장소. 화면은 이 인터페이스만 쓰고, 구현(가짜·실서버)은 AppContainer가 고른다.
 * 상태의 기준은 서버다 (2차 기획 §3: 재연결 시 로컬 화면이 아닌 서버 상태를 조회).
 */
interface OrderRepository {
    /** 진행 중이거나 방금 끝난(수령·취소 완료) 주문. 없으면 null */
    val currentOrder: StateFlow<Order?>

    /** 홈에 보여 줄 등록 매장·메뉴 */
    suspend fun quickOrders(): List<QuickOrder>

    /** 등록 매장·메뉴를 바꾼다 (Figma V01-M → V01-N). 바뀐 등록 메뉴를 돌려준다 */
    suspend fun saveQuickOrder(store: StoreSummary, menu: MenuOption): QuickOrder

    /** 예약 전 가격·품절 재확인 (2차 기획 §7) */
    suspend fun quote(quickOrderId: String): Quote

    /** 예약. 같은 키로 다시 부르면 같은 주문을 돌려준다 (2차 기획 §5) */
    suspend fun reserve(quickOrderId: String, idempotencyKey: String): Order

    suspend fun reportEta(orderId: String, reading: EtaReading)

    /** 취소 요청. 완료 여부는 매장 승인 후 currentOrder로 확인한다 */
    suspend fun requestCancel(orderId: String)

    /** 재연결·앱 복귀 시 서버 상태로 다시 맞춘다 */
    suspend fun refresh()

    /** 끝난 주문 화면을 닫고 홈으로 돌아갈 때 */
    fun clearFinished()
}
