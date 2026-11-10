package com.pickmedt.car.screenshot

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.pickmedt.car.core.model.CancelReason
import com.pickmedt.car.core.model.DriveContext
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.core.model.PickupTiming
import com.pickmedt.car.data.fake.SampleData
import com.pickmedt.car.core.model.Money
import com.pickmedt.car.data.order.Quote
import com.pickmedt.car.data.order.QuoteIssue
import com.pickmedt.car.feature.home.emptyMessage
import com.pickmedt.car.feature.home.noRouteMessage
import com.pickmedt.car.feature.home.noStoreMessage
import com.pickmedt.car.feature.home.pickMenuMessage
import com.pickmedt.car.feature.home.pickStoreMessage
import com.pickmedt.car.feature.launch.ConsentScreen
import com.pickmedt.car.feature.launch.LaunchStep
import com.pickmedt.car.feature.launch.PlateErrorScreen
import com.pickmedt.car.feature.launch.PlateInputScreen
import com.pickmedt.car.feature.launch.SplashScreen
import com.pickmedt.car.feature.launch.accessBlockedMessage
import com.pickmedt.car.feature.launch.failedMessage
import com.pickmedt.car.feature.launch.loadingMessage
import com.pickmedt.car.feature.launch.resumeOrderMessage
import com.pickmedt.car.feature.launch.welcomeMessage
import com.pickmedt.car.feature.reserve.toReserveIssueMessage
import com.pickmedt.car.feature.settings.legalMessage
import com.pickmedt.car.feature.settings.settingsMessage
import com.pickmedt.car.ui.component.MessageScreen
import com.pickmedt.car.ui.component.MessageUiState
import com.pickmedt.car.feature.home.HomeScreen
import com.pickmedt.car.feature.home.toHomeUiState
import com.pickmedt.car.feature.order.DetailPage
import com.pickmedt.car.feature.order.OrderDetailScreen
import com.pickmedt.car.feature.order.toOrderDetailUiState
import com.pickmedt.car.feature.progress.ProgressScreen
import com.pickmedt.car.feature.progress.toProgressUiState
import com.pickmedt.car.feature.reserve.ReserveScreen
import com.pickmedt.car.feature.reserve.toReserveUiState
import com.pickmedt.car.testing.kst
import com.pickmedt.car.testing.testOrder
import com.pickmedt.car.testing.testTiming
import com.pickmedt.car.ui.theme.PickmeTheme
import com.pickmedt.car.ui.theme.ProvideDesignScale
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Figma 앱 영역(840×1184)과 같은 크기로 화면을 그려 PNG로 남긴다.
 * 기록: ./gradlew :app:recordRoborazziDebug → app/build/outputs/roborazzi/
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w840dp-h1184dp-mdpi", sdk = [35], application = Application::class)
class ScreensScreenshotTest {
    private val now = kst("08:02")

    private fun capture(content: @Composable () -> Unit) = captureRoboImage {
        PickmeTheme { ProvideDesignScale(Modifier.fillMaxSize()) { content() } }
    }

    private fun progress(order: Order, baseline: PickupTiming? = null, at: java.time.Instant = now) = capture {
        ProgressScreen(toProgressUiState(order, DriveContext.DRIVING, at, baseline), onAction = {})
    }

    private fun detail(page: DetailPage, drive: DriveContext) = capture {
        val order = testOrder(OrderStage.WAITING_TO_PREPARE).copy(timing = testTiming("08:08"))
        OrderDetailScreen(toOrderDetailUiState(order, page, drive), onAction = {})
    }

    private fun <K> message(state: MessageUiState<K>) = capture { MessageScreen(state, onAction = {}) }

    private fun issue(issue: QuoteIssue, total: Int = 4_500, previous: Int? = null) = message(
        toReserveIssueMessage(
            Quote(
                quickOrderId = SampleData.usualIcedAmericano.id,
                store = SampleData.yangjae,
                lines = SampleData.usualIcedAmericano.lines.map { it.copy(unitPrice = Money(total)) },
                total = Money(total),
                issue = issue,
                previousTotal = previous?.let(::Money),
            ),
            DriveContext.PARKED,
            submitting = false,
        ),
    )

    @Test fun v01_home() = capture {
        HomeScreen(toHomeUiState(SampleData.usualIcedAmericano, DriveContext.PARKED), onChangeMenu = {}, onReserve = {})
    }

    @Test fun v02_reserve() = capture {
        val quick = SampleData.usualIcedAmericano
        ReserveScreen(
            toReserveUiState(Quote(quick.id, quick.store, quick.lines, quick.total, issue = null), DriveContext.PARKED, submitting = false),
            onBack = {},
            onSubmit = {},
        )
    }

    @Test fun v03_awaiting_store() = progress(testOrder(OrderStage.AWAITING_STORE))

    @Test fun v04a_waiting() =
        progress(testOrder(OrderStage.WAITING_TO_PREPARE).copy(timing = testTiming("08:08")), baseline = testTiming("08:08"))

    @Test fun v04b_preparing() = progress(testOrder(OrderStage.PREPARING).copy(timing = testTiming("08:05")))

    @Test fun v04c_ready() = progress(testOrder(OrderStage.READY).copy(timing = testTiming("08:08")))

    /** 실서버 주문번호처럼 긴 번호 (Figma에는 없음). 한 줄 안에서 글자가 줄어드는지 확인용 */
    @Test fun v04c_ready_long_number() =
        progress(testOrder(OrderStage.READY).copy(displayNo = "MOCK-CDB5BE39", timing = testTiming("08:08")))

    @Test fun v05a_waiting_delayed() =
        progress(testOrder(OrderStage.WAITING_TO_PREPARE).copy(timing = testTiming("08:12")), baseline = testTiming("08:08"))

    @Test fun v05b_preparing_delayed() =
        progress(testOrder(OrderStage.PREPARING).copy(timing = testTiming("08:12", delaySec = 240)))

    @Test fun v07_picked_up() = progress(testOrder(OrderStage.PICKED_UP).copy(pickedUpAt = kst("08:10")))

    // ── 2순위: 예외·취소 ──────────────────────

    @Test fun v03r_rejected() = progress(
        testOrder(OrderStage.CANCELLED).copy(cancelReason = CancelReason.STORE_REJECTED, cancelNote = "주문량 증가"),
    )

    @Test fun v03t_store_slow() = progress(testOrder(OrderStage.AWAITING_STORE).copy(requestedAt = kst("08:00")))

    @Test fun v04e_eta_lost() = progress(
        testOrder(OrderStage.WAITING_TO_PREPARE).copy(timing = testTiming("08:08", receivedAt = "08:02")),
        at = kst("08:04"),
    )

    @Test fun v06a_detail() = detail(DetailPage.OVERVIEW, DriveContext.PARKED)

    @Test fun v06b_confirm_cancel() = detail(DetailPage.CONFIRM_CANCEL, DriveContext.PARKED)

    @Test fun v06c_cancel_pending() = progress(testOrder(OrderStage.CANCEL_REQUESTED))

    @Test fun v06d_cancelled() = progress(
        testOrder(OrderStage.CANCELLED).copy(cancelReason = CancelReason.CANCEL_APPROVED, cancelledAt = kst("08:03")),
    )

    @Test fun v06e_cancel_rejected() = progress(testOrder(OrderStage.PREPARING).copy(cancelRejected = true))

    @Test fun v06l_driving_locked() = detail(DetailPage.OVERVIEW, DriveContext.DRIVING)

    // ── 3순위: 시작·첫 이용 (V00) ──────────────

    @Test fun v00s_splash() = capture { SplashScreen() }

    @Test fun v00l_loading() = message(loadingMessage())

    @Test fun v00e_failed() = message(failedMessage())

    @Test fun v00w_welcome() = message(welcomeMessage(DriveContext.PARKED))

    @Test fun v00c_consent() = capture {
        ConsentScreen(LaunchStep.Consent(), DriveContext.PARKED, onToggle = {}, onAction = {})
    }

    /** Figma에 선택 모양이 없어 비교 대상은 없다. 선택 모양 확인용 */
    @Test fun v00c_consent_checked() = capture {
        ConsentScreen(LaunchStep.Consent(personalInfo = true, location = true), DriveContext.PARKED, onToggle = {}, onAction = {})
    }

    @Test fun v00p_access_blocked() = message(accessBlockedMessage())

    @Test fun v00i_plate_input() = capture {
        PlateInputScreen("12가 3456", DriveContext.PARKED, onTextChange = {}, onAction = {})
    }

    @Test fun v00ie_plate_error() = capture { PlateErrorScreen("12가 345", onAction = {}) }

    @Test fun v00re_resume_order() = message(resumeOrderMessage(testOrder(OrderStage.WAITING_TO_PREPARE)))

    // ── 3순위: 홈 예외·선택 (V01) ──────────────

    @Test fun v01e_empty() = message(emptyMessage(DriveContext.PARKED))

    @Test fun v01m_pick_store() =
        message(pickStoreMessage(listOf(SampleData.yangjaeOnRoute, SampleData.seochoOnRoute), DriveContext.PARKED))

    @Test fun v01n_pick_menu() =
        message(pickMenuMessage(SampleData.yangjae, listOf(SampleData.icedAmericano, SampleData.hotAmericano), DriveContext.PARKED))

    @Test fun v01r_no_route() = message(noRouteMessage())

    @Test fun v01no_no_store() = message(noStoreMessage())

    // ── 3순위: 예약 예외 (V02) ────────────────

    @Test fun v02so_sold_out() = issue(QuoteIssue.SOLD_OUT)

    @Test fun v02pc_price_changed() = issue(QuoteIssue.PRICE_CHANGED, total = 4_800, previous = 4_500)

    @Test fun v02cl_store_closed() = issue(QuoteIssue.STORE_CLOSED)

    // ── 3순위: 설정 (V08) ─────────────────────

    @Test fun v08_settings() = message(settingsMessage(SampleData.PLATE_NUMBER))

    @Test fun v08l_legal() = message(legalMessage())
}
