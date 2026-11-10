package com.pickmedt.car.app

import android.app.Application
import android.content.pm.PackageManager
import com.pickmedt.car.core.time.Clock
import com.pickmedt.car.BuildConfig
import com.pickmedt.car.data.local.DeviceIdentity
import com.pickmedt.car.data.local.PrefsUserSettings
import com.pickmedt.car.platform.drive.car.CarDriveSignalSource
import com.pickmedt.car.platform.navi.pleos.PleosNavi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class PickmeApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // 차량(AAOS)에서는 Car API 주행 신호와 Pleos 내비를 쓴다
        val automotive = packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
        val carDrive = if (automotive) CarDriveSignalSource(this).also { it.start() } else null
        val carNavi = if (automotive) PleosNavi(this, Clock.System).also { it.start() } else null
        // 빌드 옵션 -Ppickme.serverUrl이 있으면 실서버에 붙는다
        val server = BuildConfig.SERVER_URL.takeIf { it.isNotBlank() }
            ?.let { AppContainer.ServerConfig(it, DeviceIdentity.id(this)) }
        container = AppContainer(appScope, PrefsUserSettings(this), carDrive, carNavi, server)
        container.etaReporter.start()
    }
}
