package com.robingebert.blokky.application

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.net.VpnService
import com.robingebert.blokky.datastore.DataStoreManager
import com.robingebert.blokky.feature_monitor.AppMonitorService
import com.robingebert.blokky.feature_monitor.UsageAccess
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

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(AppMonitorService.CHANNEL_MONITOR, "Blockfy Focus", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(AppMonitorService.CHANNEL_ALERT, "Blockfy Alerts", NotificationManager.IMPORTANCE_HIGH)
        )

        if (UsageAccess.isGranted(this)) {
            AppMonitorService.start(this)
        }

        appScope.launch {
            val settings = DataStoreManager(this@MainApplication).appSettingsFlow.first()
            if (settings.adultContentBlockerEnabled && VpnService.prepare(this@MainApplication) == null) {
                AdultBlockVpnService.start(this@MainApplication)
            }
        }
    }
}
