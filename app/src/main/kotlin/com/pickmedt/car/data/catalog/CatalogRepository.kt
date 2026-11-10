package com.pickmedt.car.data.catalog

import com.pickmedt.car.core.model.MenuOption
import com.pickmedt.car.core.model.RouteSnapshot
import com.pickmedt.car.core.model.StoreOnRoute

/** 매장·메뉴 조회 (Figma V01-M·V01-N·V01-NO). 실서버 API는 연동(11/13) 때 정한다 */
interface CatalogRepository {
    /** 경로에서 이용할 수 있는 매장. 가까운 순. 없으면 빈 목록 */
    suspend fun storesAlong(route: RouteSnapshot): List<StoreOnRoute>

    suspend fun menu(storeId: String): List<MenuOption>
}
