package com.buenotty.blockfy.feature_accessibility

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Whether the blocking service is actually alive. Android's "enabled services" list keeps naming
 * Blockfy after a phone (MIUI and others) kills the service, so the list alone cannot be trusted:
 * the service reports itself here once it is bound.
 */
enum class ServiceHealth {
    /** Not in the system's enabled list: the user never turned it on, or turned it off. */
    OFF,

    /** In the enabled list but not running: the system killed it. Off and on again revives it. */
    STALLED,

    RUNNING;

    companion object {
        private val bound = MutableStateFlow(false)

        /** Flips to true when the service binds and back to false when it unbinds or dies. */
        val serviceBound: StateFlow<Boolean> = bound

        fun markBound(value: Boolean) {
            bound.value = value
        }

        fun of(listed: Boolean, bound: Boolean) = when {
            !listed -> OFF
            bound -> RUNNING
            else -> STALLED
        }
    }
}

/** Phones whose battery managers are known to kill accessibility services in the background. */
fun isAggressiveBatteryVendor(manufacturer: String = Build.MANUFACTURER): Boolean =
    manufacturer.lowercase() in setOf("xiaomi", "redmi", "poco", "oppo", "realme", "vivo", "huawei", "honor", "oneplus")

/** Opens the Xiaomi auto-start page, or the app's own settings where there is none. */
fun Context.openBackgroundSettings() {
    val miui = Intent().setClassName(
        "com.miui.securitycenter",
        "com.miui.permcenter.autostart.AutoStartManagementActivity"
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(miui) }.onFailure {
        runCatching {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:$packageName"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
