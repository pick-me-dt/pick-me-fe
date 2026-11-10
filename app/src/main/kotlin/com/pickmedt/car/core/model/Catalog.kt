package com.pickmedt.car.core.model

/** 현재 경로에서 이용할 수 있는 매장 (Figma V01-M) */
data class StoreOnRoute(
    val store: StoreSummary,
    /** 매장까지 남은 시간(분). 예: 6 → "도착 6분". 모르면 null */
    val etaMinutes: Int?,
    /** 매장까지 거리(m). 남은 시간을 모를 때 대신 보여 준다 (1차 기준 서버는 거리만 준다) */
    val distanceM: Int? = null,
)

/** 매장에서 고를 수 있는 메뉴 (Figma V01-N) */
data class MenuOption(
    val id: String,
    val name: String,
    /** 예: "Regular" */
    val option: String?,
    val price: Money,
    val unit: String = "잔",
)

fun MenuOption.toOrderLine(qty: Int = 1) =
    OrderLine(menuItemId = id, name = name, option = option, unitPrice = price, qty = qty, unit = unit)

/** 위도·경도 */
data class GeoPoint(val lat: Double, val lng: Double)

/** 내비게이션에 설정된 경로. [origin]은 지금 차량 위치(실서버 매장 검색에 쓴다) */
data class RouteSnapshot(val destinationName: String?, val origin: GeoPoint? = null)
