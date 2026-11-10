package com.pickmedt.car.feature.launch

import com.pickmedt.car.data.fake.FakeOrderRepository
import com.pickmedt.car.data.fake.FakeServer
import com.pickmedt.car.data.fake.InMemoryUserSettings
import com.pickmedt.car.data.fake.SampleData
import com.pickmedt.car.data.settings.UserSettings
import com.pickmedt.car.testing.MutableClock
import com.pickmedt.car.testing.kst
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchControllerTest {
    private val server = FakeServer(MutableClock(kst("08:02")))
    private val orders = FakeOrderRepository(server)
    private val settings = InMemoryUserSettings()
    private val controller = LaunchController(settings, orders)

    @Test fun `처음에는 스플래시`() = assertEquals(LaunchStep.Splash, controller.step.value)

    @Test fun `첫 이용이면 첫 이용 안내`() = runTest {
        controller.load()
        assertEquals(LaunchStep.Welcome, controller.step.value)
    }

    @Test fun `조회에 실패하면 실행 실패, 다시 시도할 수 있다`() = runTest {
        server.failNextRefresh()
        controller.load()
        assertEquals(LaunchStep.Failed, controller.step.value)
        controller.load()
        assertEquals(LaunchStep.Welcome, controller.step.value)
    }

    @Test fun `조회가 취소되면 실행 실패로 보지 않는다`() = runTest {
        val slow = object : com.pickmedt.car.data.order.OrderRepository by orders {
            override suspend fun refresh() = kotlinx.coroutines.awaitCancellation()
        }
        val cancellable = LaunchController(settings, slow)
        val job = launch { cancellable.load() }
        testScheduler.runCurrent()
        job.cancel()
        testScheduler.runCurrent()
        assertEquals(LaunchStep.Loading, cancellable.step.value)
    }

    @Test fun `진행 중 주문이 있으면 진행 주문 복귀`() = runTest {
        val order = server.reserve(SampleData.usualIcedAmericano.id, "key")
        controller.load()
        assertEquals(LaunchStep.ResumeOrder(order), controller.step.value)
    }

    @Test fun `등록을 마친 사용자는 바로 홈`() = runTest {
        settings.agreeConsent()
        settings.registerPlate("12가3456")
        controller.load()
        assertEquals(LaunchStep.Done, controller.step.value)
    }

    @Test fun `동의는 두 항목을 모두 선택해야 계속할 수 있다`() = runTest {
        controller.load()
        controller.start()
        assertEquals(LaunchStep.Consent(), controller.step.value)
        controller.toggleConsent(ConsentItem.PERSONAL_INFO)
        assertEquals(false, (controller.step.value as LaunchStep.Consent).canAgree)
        controller.toggleConsent(ConsentItem.LOCATION)
        assertTrue((controller.step.value as LaunchStep.Consent).canAgree)
        controller.agree()
        assertEquals(LaunchStep.PlateInput(""), controller.step.value)
        assertTrue(settings.settings.value.consentAgreed)
    }

    @Test fun `모두 선택하지 않으면 동의해도 넘어가지 않는다`() = runTest {
        controller.load()
        controller.start()
        controller.toggleConsent(ConsentItem.PERSONAL_INFO)
        controller.agree()
        assertEquals(LaunchStep.Consent(personalInfo = true), controller.step.value)
    }

    @Test fun `동의하지 않으면 정보 접근 불가, 접근 상태 확인은 동의 화면으로`() = runTest {
        controller.load()
        controller.start()
        controller.decline()
        assertEquals(LaunchStep.AccessBlocked, controller.step.value)
        controller.recheckAccess()
        assertEquals(LaunchStep.Consent(), controller.step.value)
    }

    @Test fun `차량번호 형식이 틀리면 입력 오류, 수정하면 입력으로 돌아간다`() = runTest {
        settings.agreeConsent()
        controller.load()
        controller.plateChanged("12가 345")
        controller.submitPlate()
        assertEquals(LaunchStep.PlateError("12가 345"), controller.step.value)
        controller.fixPlate()
        assertEquals(LaunchStep.PlateInput("12가 345"), controller.step.value)
    }

    @Test fun `차량번호가 맞으면 저장하고 홈`() = runTest {
        settings.agreeConsent()
        controller.load()
        controller.plateChanged("12가 3456")
        controller.submitPlate()
        assertEquals(LaunchStep.Done, controller.step.value)
        assertEquals(UserSettings(consentAgreed = true, plateNumber = "12가3456"), settings.settings.value)
    }
}
