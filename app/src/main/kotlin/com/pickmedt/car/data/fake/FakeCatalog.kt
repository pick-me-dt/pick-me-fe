package com.pickmedt.car.data.fake

import com.pickmedt.car.core.model.MenuOption
import com.pickmedt.car.core.model.RouteSnapshot
import com.pickmedt.car.core.model.StoreOnRoute
import com.pickmedt.car.data.catalog.CatalogRepository

/** Figma V01-M·V01-N 예시 매장·메뉴. 경로와 상관없이 같은 목록을 준다 */
class FakeCatalog : CatalogRepository {
    /** 시연용 「매장 없음」(V01-NO) */
    var noStores: Boolean = false

    override suspend fun storesAlong(route: RouteSnapshot): List<StoreOnRoute> =
        if (noStores) emptyList() else listOf(SampleData.yangjaeOnRoute, SampleData.seochoOnRoute)

    override suspend fun menu(storeId: String): List<MenuOption> =
        listOf(SampleData.icedAmericano, SampleData.hotAmericano)
}
