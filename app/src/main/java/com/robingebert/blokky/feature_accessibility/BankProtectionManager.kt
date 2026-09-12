package com.robingebert.blokky.feature_accessibility

import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.robingebert.blokky.R

object BankProtectionManager {

    private const val TAG = "BankProtectionManager"

    const val ACTION_BANK_DETECTED = "com.robingebert.blokky.ACTION_BANK_DETECTED"
    const val ACTION_BANK_CLOSED = "com.robingebert.blokky.ACTION_BANK_CLOSED"

    const val BANK_ALERT_CHANNEL_ID = "blockfy_bank_protection_channel"
    const val NOTIFICATION_ID_BANK_REACTIVATE = 2001
    const val NOTIFICATION_ID_MONITOR_SERVICE = 2002

    /**
     * Checks whether PACKAGE_USAGE_STATS permission has been granted.
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Log.e(TAG, "Error checking usage stats permission", e)
            false
        }
    }

    /**
     * Checks whether WRITE_SECURE_SETTINGS permission has been granted via ADB.
     */
    fun hasWriteSecureSettingsPermission(context: Context): Boolean {
        return try {
            context.checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
                    PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gets the currently active foreground package name using UsageStatsManager.
     */
    fun getForegroundPackage(context: Context): String? {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null

        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10_000L // Look back 10 seconds

        return try {
            val events = usageStatsManager.queryEvents(startTime, endTime) ?: return null
            val event = UsageEvents.Event()
            var lastForegroundPackage: String? = null

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastForegroundPackage = event.packageName
                }
            }
            lastForegroundPackage
        } catch (e: Exception) {
            Log.e(TAG, "Error querying foreground app", e)
            null
        }
    }

    /**
     * Checks if Blockfy's Accessibility Service is registered as enabled in Settings.Secure.
     */
    fun isAccessibilityEnabledInSettings(context: Context): Boolean {
        return try {
            val serviceComponent = ComponentName(context, ReelsBlockAccessibilityService::class.java).flattenToString()
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            enabledServices.split(":").any { it.trim().equals(serviceComponent, ignoreCase = true) }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Enables or disables Blockfy's Accessibility Service via Settings.Secure (requires WRITE_SECURE_SETTINGS).
     * Returns true if operation succeeded.
     */
    fun setAccessibilityServiceEnabledViaSecureSettings(context: Context, enable: Boolean): Boolean {
        if (!hasWriteSecureSettingsPermission(context)) {
            return false
        }

        return try {
            val serviceComponent = ComponentName(context, ReelsBlockAccessibilityService::class.java).flattenToString()
            val current = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""

            val servicesList = current.split(":")
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.equals(serviceComponent, ignoreCase = true) }
                .toMutableList()

            if (enable) {
                servicesList.add(serviceComponent)
                val newSetting = servicesList.joinToString(":")
                Settings.Secure.putString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    newSetting
                )
                Settings.Secure.putInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED,
                    1
                )
                Log.d(TAG, "Successfully enabled AccessibilityService via WRITE_SECURE_SETTINGS")
            } else {
                val newSetting = servicesList.joinToString(":")
                Settings.Secure.putString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    newSetting
                )
                Log.d(TAG, "Successfully disabled AccessibilityService via WRITE_SECURE_SETTINGS")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed modifying ENABLED_ACCESSIBILITY_SERVICES", e)
            false
        }
    }

    /**
     * Shows a high-priority notification to let the user reactivate Blockfy with one tap
     * after leaving a banking app (used when WRITE_SECURE_SETTINGS is not granted).
     */
    fun showReactivateNotification(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    BANK_ALERT_CHANNEL_ID,
                    "Proteção Bancária Blockfy",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alertas para reativar o bloqueio após fechar bancos"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, BANK_ALERT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_blockfy_logo)
                .setContentTitle("🛡️ Banco fechado • Reative o Blockfy")
                .setContentText("O app de banco foi fechado. Toque aqui para religar a proteção contra Reels/Shorts.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID_BANK_REACTIVATE, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing reactivate notification", e)
        }
    }

    /**
     * Dismisses the reactivate notification.
     */
    fun cancelReactivateNotification(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID_BANK_REACTIVATE)
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling notification", e)
        }
    }
}
