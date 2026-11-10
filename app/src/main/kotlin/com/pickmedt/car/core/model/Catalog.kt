package com.pickmedt.car.core.model

/** 현재 경로에서 이용할 수 있는 매장 (Figma V01-M) */
data class StoreOnRoute(
    val store: StoreSummary,
    /** 매장까지 남은 시간(분). 예: 6 → "도착 6분" */
    val etaMinutes: Int,
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

/** 내비게이션에 설정된 경로. 실차 구현에서 담을 값은 게이트 G3 확인 후 정한다 */
data class RouteSnapshot(val destinationName: String?)
