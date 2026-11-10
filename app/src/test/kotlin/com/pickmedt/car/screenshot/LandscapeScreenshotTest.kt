package com.pickmedt.car.screenshot

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.pickmedt.car.core.model.CancelReason
import com.pickmedt.car.core.model.DriveContext
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.data.fake.SampleData
import com.pickmedt.car.data.order.Quote
import com.pickmedt.car.feature.home.HomeScreen
import com.pickmedt.car.feature.home.pickStoreMessage
import com.pickmedt.car.feature.home.toHomeUiState
import com.pickmedt.car.feature.launch.ConsentScreen
import com.pickmedt.car.feature.launch.LaunchStep
import com.pickmedt.car.feature.launch.SplashScreen
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
import com.pickmedt.car.ui.component.MessageScreen
import com.pickmedt.car.ui.theme.PickmeTheme
import com.pickmedt.car.ui.theme.ProvideDesignScale
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 가로 2단 배치. Pleos Connect-S-17 가로 앱 영역(1700×1184)으로 그린다.
 * Figma에는 가로 화면이 없어 세로 화면 두 칸(840 + 20 + 840)을 나란히 놓은 배치다.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1700dp-h1184dp-mdpi", sdk = [35], application = Application::class)
class LandscapeScreenshotTest {
    private val now = kst("08:02")

    private fun capture(content: @Composable () -> Unit) = captureRoboImage {
        PickmeTheme { ProvideDesignScale(Modifier.fillMaxSize(), allowLandscape = true) { content() } }
    }

    @Test fun land_v00s_splash() = capture { SplashScreen() }

    @Test fun land_v01_home() = capture {
        HomeScreen(toHomeUiState(SampleData.usualIcedAmericano, DriveContext.PARKED), onChangeMenu = {}, onReserve = {})
    }

    @Test fun land_v02_reserve() = capture {
        val quick = SampleData.usualIcedAmericano
        ReserveScreen(
            toReserveUiState(Quote(quick.id, quick.store, quick.lines, quick.total, issue = null), DriveContext.PARKED, submitting = false),
            onBack = {},
            onSubmit = {},
        )
    }

    @Test fun land_v04a_waiting() = capture {
        val order = testOrder(OrderStage.WAITING_TO_PREPARE).copy(timing = testTiming("08:08"))
        ProgressScreen(toProgressUiState(order, DriveContext.DRIVING, now, testTiming("08:08")), onAction = {})
    }

    @Test fun land_v04c_ready() = capture {
        ProgressScreen(toProgressUiState(testOrder(OrderStage.READY).copy(timing = testTiming("08:08")), DriveContext.DRIVING, now, null), onAction = {})
    }

    @Test fun land_v03r_rejected() = capture {
        val order = testOrder(OrderStage.CANCELLED).copy(cancelReason = CancelReason.STORE_REJECTED, cancelNote = "주문량 증가")
        ProgressScreen(toProgressUiState(order, DriveContext.DRIVING, now, null), onAction = {})
    }

    @Test fun land_v06a_detail() = capture {
        val order = testOrder(OrderStage.WAITING_TO_PREPARE).copy(timing = testTiming("08:08"))
        OrderDetailScreen(toOrderDetailUiState(order, DetailPage.OVERVIEW, DriveContext.PARKED), onAction = {})
    }

    @Test fun land_v00c_consent() = capture {
        ConsentScreen(LaunchStep.Consent(), DriveContext.PARKED, onToggle = {}, onAction = {})
    }

    @Test fun land_v01m_pick_store() = capture {
        MessageScreen(pickStoreMessage(listOf(SampleData.yangjaeOnRoute, SampleData.seochoOnRoute), DriveContext.PARKED), onAction = {})
    }
}
