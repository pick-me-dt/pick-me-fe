package com.pickmedt.car.screenshot

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 세로 스크린샷과 같은 화면을 Pleos 가로 앱 영역(1700×1184)으로 그린다.
 * Figma에는 가로 화면이 없어 세로 화면 두 칸(840 + 20 + 840)을 나란히 놓은 배치다 (ScreenFrame 참고).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1700dp-h1184dp-mdpi", sdk = [35], application = Application::class)
class LandscapeScreensScreenshotTest : ScreensScreenshotTest() {
    override val landscape = true
}
