package com.pickmedt.car.data.remote

import com.pickmedt.car.core.model.MenuOption
import com.pickmedt.car.core.model.Money
import com.pickmedt.car.core.model.RouteSnapshot
import com.pickmedt.car.core.model.StoreOnRoute
import com.pickmedt.car.core.model.StoreSummary
import com.pickmedt.car.data.catalog.CatalogRepository
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

/**
 * 실서버 매장·메뉴.
 * - 매장: POST /v1/stores/search (차량 위치 기준 반경 검색). 서버는 남은 시간 대신 거리만 준다
 * - 메뉴: GET /v1/stores/:id/menu. 옵션(Regular 등)은 비어 있다
 */
class RemoteCatalog(private val session: RemoteSession) : CatalogRepository {
    override suspend fun storesAlong(route: RouteSnapshot): List<StoreOnRoute> {
        val origin = route.origin ?: return emptyList()
        return session.authorized { http ->
            val body = http.json.encodeToJsonElement(SearchStoresRequest(origin.lat, origin.lng, SEARCH_RADIUS_M))
            http.json.decodeFromJsonElement<List<StoreCandidateDto>>(http.post("/v1/stores/search", body))
        }.map { StoreOnRoute(StoreSummary(it.id, it.name, BRAND), etaMinutes = null, distanceM = it.distanceM.toInt()) }
    }

    override suspend fun menu(storeId: String): List<MenuOption> = session.authorized { http ->
        http.json.decodeFromJsonElement<MenuDto>(http.get("/v1/stores/$storeId/menu"))
    }.items.map { MenuOption(id = it.id, name = it.name, option = null, price = Money(it.price)) }

    companion object {
        /** 서버 응답에 브랜드 이름이 없어 매장 이름(예: "픽미 양재 DT점")만 쓴다 */
        const val BRAND = ""

        /** 경로상 매장 검색 반경. 서버 기본값을 쓰면 10km 거리의 양재점이 빠진다 (확인: 판교 근처 좌표) */
        const val SEARCH_RADIUS_M = 15_000
    }
}
