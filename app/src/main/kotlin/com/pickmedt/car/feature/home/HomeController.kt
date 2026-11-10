package com.pickmedt.car.feature.home

import com.pickmedt.car.core.model.MenuOption
import com.pickmedt.car.core.model.QuickOrder
import com.pickmedt.car.core.model.StoreOnRoute
import com.pickmedt.car.core.model.StoreSummary
import com.pickmedt.car.data.catalog.CatalogRepository
import com.pickmedt.car.data.order.OrderRepository
import com.pickmedt.car.data.order.Quote
import com.pickmedt.car.platform.navi.RouteSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 홈·예약 단계 (Figma V01·V02) */
sealed interface HomeStep {
    data object Loading : HomeStep

    /** V01 */
    data class Start(val quick: QuickOrder) : HomeStep

    /** V01-E */
    data object Empty : HomeStep

    /** V01-R */
    data object NoRoute : HomeStep

    /** V01-NO */
    data object NoStore : HomeStep

    /** V01-M */
    data class PickStore(val stores: List<StoreOnRoute>) : HomeStep

    /** V01-N */
    data class PickMenu(val store: StoreSummary, val menus: List<MenuOption>) : HomeStep

    /** V02(문제 없음), V02-SO·PC·CL(quote.issue) */
    data class Confirm(val quote: Quote, val submitting: Boolean = false) : HomeStep
}

/**
 * V01 → V02 흐름. 매장·메뉴를 바꾸면 경로를 먼저 확인하고(V01-R), 경로상 매장(V01-M) → 메뉴(V01-N) 순으로 고른다.
 * 예약 요청이 받아들여지면 현재 주문이 생겨 진행 화면으로 넘어간다 (AppRoot).
 */
class HomeController(
    private val orders: OrderRepository,
    private val catalog: CatalogRepository,
    private val routes: RouteSource,
) {
    private val state = MutableStateFlow<HomeStep>(HomeStep.Loading)
    val step: StateFlow<HomeStep> = state.asStateFlow()

    private var quick: QuickOrder? = null

    /** 이전으로 돌아갈 때 쓰는 매장 목록 */
    private var lastStores: List<StoreOnRoute> = emptyList()

    suspend fun load() {
        quick = runCatching { orders.quickOrders().firstOrNull() }.getOrNull()
        showStart()
    }

    /** V01 「매장·메뉴 변경」, V01-E 「매장·메뉴 등록」, V01-R 「경로 다시 확인」, V01-NO 「매장 다시 확인」, V02-CL 「다른 매장 선택」 */
    suspend fun changeStoreAndMenu() {
        val route = routes.currentRoute()
        if (route == null) {
            state.value = HomeStep.NoRoute
            return
        }
        val stores = runCatching { catalog.storesAlong(route) }.getOrDefault(emptyList())
        if (stores.isEmpty()) {
            state.value = HomeStep.NoStore
            return
        }
        lastStores = stores
        state.value = HomeStep.PickStore(stores)
    }

    /** V01-R 「내비게이션 열기」 */
    fun openNavigation() = routes.openNavigation()

    suspend fun pickStore(store: StoreSummary) {
        val menus = runCatching { catalog.menu(store.id) }.getOrNull() ?: return
        state.value = HomeStep.PickMenu(store, menus)
    }

    suspend fun pickMenu(menu: MenuOption) {
        val current = state.value as? HomeStep.PickMenu ?: return
        quick = runCatching { orders.saveQuickOrder(current.store, menu) }.getOrNull() ?: return
        showStart()
    }

    /** 「이전으로」: 메뉴 → 매장 → 시작. V01-NO·V02 예외의 「시작으로」도 시작으로 간다 */
    fun back() {
        state.value = when (state.value) {
            is HomeStep.PickMenu -> if (lastStores.isEmpty()) startStep() else HomeStep.PickStore(lastStores)
            else -> startStep()
        }
    }

    /** V01 「이 메뉴로 예약」 → 가격·품절·접수 재확인 (2차 기획 §7) */
    suspend fun reserve() {
        val home = quick ?: return
        val quote = runCatching { orders.quote(home.id) }.getOrNull() ?: return
        state.value = HomeStep.Confirm(quote)
    }

    /** V02-SO 「다른 메뉴 선택」, V02-PC 「메뉴 다시 선택」: 같은 매장의 메뉴로 */
    suspend fun chooseOtherMenu() {
        val confirm = state.value as? HomeStep.Confirm ?: return
        pickStore(confirm.quote.store)
    }

    /** V02 「… 예약 요청」. 같은 키로 다시 보내면 같은 주문이 된다 (2차 기획 §5) */
    suspend fun submit(idempotencyKey: String) {
        val confirm = state.value as? HomeStep.Confirm ?: return
        if (confirm.submitting) return
        state.value = confirm.copy(submitting = true)
        runCatching { orders.reserve(confirm.quote.quickOrderId, idempotencyKey) }
        state.value = (state.value as? HomeStep.Confirm)?.copy(submitting = false) ?: state.value
    }

    private fun showStart() {
        state.value = startStep()
    }

    private fun startStep(): HomeStep = quick?.let { HomeStep.Start(it) } ?: HomeStep.Empty
}
