package com.robingebert.blokky.feature_accessibility

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.robingebert.blokky.MainActivity
import com.robingebert.blokky.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
class BankProtectionMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var monitorJob: Job? = null
    private var isBankActive = false

    companion object {
        private const val TAG = "BankMonitorService"
        private const val CHANNEL_ID = "blockfy_monitor_channel"

        fun start(context: Context) {
            val intent = Intent(context, BankProtectionMonitorService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed starting BankProtectionMonitorService", e)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BankProtectionMonitorService::class.java)
            try {
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed stopping BankProtectionMonitorService", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = buildPersistentNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                BankProtectionManager.NOTIFICATION_ID_MONITOR_SERVICE,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(BankProtectionManager.NOTIFICATION_ID_MONITOR_SERVICE, notification)
        }

        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        monitorJob?.cancel()
        serviceScope.cancel()
    }

    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (isActive) {
                delay(1200L) // Poll every 1.2 seconds for low CPU overhead

                if (!BankProtectionManager.hasUsageStatsPermission(this@BankProtectionMonitorService)) {
                    continue
                }

                val currentApp = BankProtectionManager.getForegroundPackage(this@BankProtectionMonitorService)
                val isCurrentAppBank = BankAppDetector.isBankApp(currentApp)

                if (isCurrentAppBank && !isBankActive) {
                    // BANK OPENED!
                    isBankActive = true
                    Log.d(TAG, "Banking app opened: $currentApp. Deactivating AccessibilityService...")

                    // 1. If we have WRITE_SECURE_SETTINGS, disable via system settings
                    val disabledViaSettings = BankProtectionManager.setAccessibilityServiceEnabledViaSecureSettings(
                        this@BankProtectionMonitorService,
                        false
                    )

                    // 2. Broadcast to running service to call disableSelf()
                    val broadcastIntent = Intent(BankProtectionManager.ACTION_BANK_DETECTED).apply {
                        setPackage(packageName)
                        putExtra("bank_package", currentApp)
                    }
                    sendBroadcast(broadcastIntent)

                } else if (!isCurrentAppBank && isBankActive) {
                    // BANK CLOSED!
                    isBankActive = false
                    Log.d(TAG, "Banking app closed. Reactivating AccessibilityService...")

                    // 1. If we have WRITE_SECURE_SETTINGS, re-enable automatically
                    val reenabled = BankProtectionManager.setAccessibilityServiceEnabledViaSecureSettings(
                        this@BankProtectionMonitorService,
                        true
                    )

                    if (!reenabled) {
                        // 2. Without ADB permission, show high-priority tap-to-reactivate notification
                        BankProtectionManager.showReactivateNotification(this@BankProtectionMonitorService)
                    }

                    // 3. Broadcast bank closed
                    val broadcastIntent = Intent(BankProtectionManager.ACTION_BANK_CLOSED).apply {
                        setPackage(packageName)
                    }
                    sendBroadcast(broadcastIntent)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Monitor de Proteção Bancária",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantém a proteção bancária ativa em segundo plano"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildPersistentNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_blockfy_logo)
            .setContentTitle("🛡️ Blockfy • Proteção Bancária")
            .setContentText("Desativa o serviço automaticamente ao abrir bancos digitais.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
