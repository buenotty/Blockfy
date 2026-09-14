package com.robingebert.blokky.feature_monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.robingebert.blokky.datastore.DataStoreManager
import com.robingebert.blokky.feature_vpn.AdultBlockVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        AppMonitorService.start(context)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = DataStoreManager(context).appSettingsFlow.first()
                if (settings.adultContentBlockerEnabled && VpnService.prepare(context) == null) {
                    AdultBlockVpnService.start(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
