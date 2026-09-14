package com.robingebert.blokky.application

import android.app.Application
import android.net.VpnService
import com.robingebert.blokky.datastore.DataStoreManager
import com.robingebert.blokky.feature_vpn.AdultBlockVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MainApplication: Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MainApplication)
            androidLogger()
            modules(AppModule.modules())
        }

        appScope.launch {
            val settings = DataStoreManager(this@MainApplication).appSettingsFlow.first()
            if (settings.adultContentBlockerEnabled && VpnService.prepare(this@MainApplication) == null) {
                AdultBlockVpnService.start(this@MainApplication)
            }
        }
    }
}
