package com.buenotty.blockfy.feature_setup

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri

/** True when the system will not put Blockfy to sleep to save battery. */
fun Context.isIgnoringBatteryOptimizations(): Boolean {
    val manager = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return manager.isIgnoringBatteryOptimizations(packageName)
}

/**
 * Shows the system's own "let this app run in the background" prompt. If a phone does not offer
 * it, falls back to the battery optimization list.
 */
fun Context.requestIgnoreBatteryOptimizations() {
    val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = "package:$packageName".toUri()
    }
    runCatching { startActivity(direct) }.onFailure {
        runCatching { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
    }
}
