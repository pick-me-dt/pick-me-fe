package com.pickmedt.car.app

import android.app.Application
import com.pickmedt.car.data.local.PrefsUserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class PickmeApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(appScope, PrefsUserSettings(this))
        container.etaReporter.start()
    }
}
