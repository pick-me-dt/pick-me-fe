package com.pickmedt.car.data.remote

import kotlinx.serialization.Serializable

// 서버 응답·요청 모양. pick-me-be Swagger(/docs-json, d43e2d2)와 실제 응답으로 확인한 필드만 둔다.

@Serializable
data class RegisterDeviceRequest(val deviceId: String, val plateNumber: String)

@Serializable
data class DriverTokenDto(val accessToken: String, val userId: String, val vehicleId: String)

@Serializable
data class OrderLineDto(val menuItemId: String, val qty: Int)

@Serializable
data class FavoriteDto(val id: String, val alias: String, val storeId: String? = null, val items: List<OrderLineDto>)

@Serializable
data class CreateFavoriteRequest(val alias: String, val storeId: String, val items: List<OrderLineDto>)

@Serializable
data class LatLngDto(val lat: Double, val lng: Double)

@Serializable
data class SearchStoresRequest(val lat: Double, val lng: Double, val radiusM: Int? = null)

@Serializable
data class StoreCandidateDto(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val distanceM: Double,
    val queueCars: Int = 0,
    val queueWaitSec: Int = 0,
)

@Serializable
data class MenuItemDto(val id: String, val name: String, val price: Int, val prepSec: Int = 0)

@Serializable
data class MenuDto(val storeId: String, val storeName: String, val items: List<MenuItemDto>)

@Serializable
data class OrderRequest(val storeId: String, val favoriteId: String? = null, val items: List<OrderLineDto>? = null)

@Serializable
data class OrderItemDto(
    val menuItemId: String,
    val name: String,
    val unitPrice: Int,
    val qty: Int,
    /** 견적에만 있다 */
    val soldOut: Boolean = false,
)

/** 견적 (2차: available·soldOut, 매장 준비 시간·입구 이후 시간) */
@Serializable
data class QuoteDto(
    val storeId: String,
    val storeName: String,
    val items: List<OrderItemDto>,
    val totalAmount: Int,
    val available: Boolean = true,
    val prepSec: Int? = null,
    val afterEntrySec: Int? = null,
)

@Serializable
data class OrderStoreDto(val id: String, val name: String, val lat: Double? = null, val lng: Double? = null)

/** 서버가 계산한 제조 타이밍 (pick-me-be src/timing/pickup-timing.ts PickupTiming) */
@Serializable
data class TimingDto(
    val etaSec: Int? = null,
    val etaUpdatedAt: String? = null,
    val etaStale: Boolean = false,
    val entranceArrivalAt: String? = null,
    val expectedPickupAt: String? = null,
    val recommendedStartAt: String? = null,
    val prepSec: Int? = null,
    val afterEntrySec: Int? = null,
    val delaySec: Int = 0,
)

@Serializable
data class CancelRequestDto(val requestedAt: String? = null, val note: String? = null, val statusBefore: String? = null)

/** 주문 (pick-me-be src/orders/order-view.ts toOrderView, 2차) */
@Serializable
data class OrderDto(
    val id: String,
    val orderNo: String? = null,
    val status: String,
    val store: OrderStoreDto,
    val items: List<OrderItemDto>,
    val totalAmount: Int,
    val plateNumber: String? = null,
    val externalOrderId: String? = null,
    val timing: TimingDto? = null,
    val acceptOverdue: Boolean = false,
    val createdAt: String? = null,
    val acceptedAt: String? = null,
    val rejectedAt: String? = null,
    val rejectReason: String? = null,
    val preparingAt: String? = null,
    val readyAt: String? = null,
    val pickedUpAt: String? = null,
    val laneEnteredAt: String? = null,
    val cancelRequest: CancelRequestDto? = null,
    val cancelledAt: String? = null,
    val cancelReason: String? = null,
    val cancelNote: String? = null,
)

@Serializable
data class ReserveRequest(
    val storeId: String,
    val favoriteId: String? = null,
    val items: List<OrderLineDto>? = null,
    val source: String = "SCREEN",
    /** 사용자가 확인한 금액. 다르면 409 PRICE_CHANGED */
    val expectedTotal: Int? = null,
)

@Serializable
data class ReserveResponseDto(val order: OrderDto)

/** 예약 409 본문: code(ACTIVE_ORDER_EXISTS·SOLD_OUT·PRICE_CHANGED)와 새 견적 */
@Serializable
data class ReserveConflictDto(val code: String? = null, val message: String? = null, val orderId: String? = null, val quote: QuoteDto? = null)

@Serializable
data class CancelRequest(val reason: String? = null)

/** 텔레메트리 (2차: etaSec 필수, 위치·속도는 기록용 선택) */
@Serializable
data class TelemetryRequest(
    val etaSec: Int,
    val lat: Double? = null,
    val lng: Double? = null,
    val speedKmh: Double? = null,
    val gear: String? = null,
)

@Serializable
data class OrderEnvelopeDto(val order: OrderDto)
