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
data class ReserveRequest(
    val source: String,
    val storeId: String,
    val favoriteId: String? = null,
    val items: List<OrderLineDto>? = null,
)

@Serializable
data class OrderItemDto(val menuItemId: String, val name: String, val unitPrice: Int, val qty: Int)

@Serializable
data class QuoteDto(
    val storeId: String,
    val storeName: String,
    val items: List<OrderItemDto>,
    val totalAmount: Int,
    val estPrepSec: Int = 0,
)

@Serializable
data class OrderStoreDto(val id: String, val name: String, val lat: Double? = null, val lng: Double? = null)

@Serializable
data class OrderDto(
    val id: String,
    val status: String,
    val store: OrderStoreDto,
    val items: List<OrderItemDto>,
    val totalAmount: Int,
    val plateNumber: String? = null,
    val estPrepSec: Int? = null,
    val externalOrderId: String? = null,
    val createdAt: String? = null,
    val sentAt: String? = null,
    val readyAt: String? = null,
    val pickedUpAt: String? = null,
    val cancelledAt: String? = null,
    val cancelReason: String? = null,
)

@Serializable
data class ReserveResponseDto(val order: OrderDto, val confirmationReasons: List<String> = emptyList())

@Serializable
data class CancelRequest(val reason: String? = null)

@Serializable
data class TelemetryRequest(
    val lat: Double,
    val lng: Double,
    val speedKmh: Double,
    val etaSec: Int? = null,
    val gear: String? = null,
)
