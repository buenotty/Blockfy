package com.robingebert.blokky.feature_monitor

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
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.robingebert.blokky.MainActivity
import com.robingebert.blokky.R
import com.robingebert.blokky.datastore.AppSettings
import com.robingebert.blokky.datastore.DailyUsage
import com.robingebert.blokky.datastore.DataStoreManager
import com.robingebert.blokky.feature_accessibility.MindfulnessProvocationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AppMonitorService : Service(), KoinComponent {

    private val dataStore: DataStoreManager by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var settings = AppSettings()

    @Volatile
    private var usage = DailyUsage()

    private var monitorJob: Job? = null
    private var lastForegroundPackage: String? = null
    private var lastSampleElapsed = 0L
    private val lastAppEntryProvocation = mutableMapOf<String, Long>()
    private var lastInterruptAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannels()
        startForegroundInternal()
        scope.launch {
            dataStore.appSettingsFlow.collect { settings = it }
        }
        scope.launch {
            dataStore.dailyUsageFlow.collect { usage = it }
        }
        monitorJob = scope.launch {
            lastSampleElapsed = SystemClock.elapsedRealtime()
            while (isActive) {
                try {
                    tick()
                } catch (t: Throwable) {
                    Log.e(TAG, "Monitor tick failed", t)
                }
                delay(POLL_MS)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundInternal()
        return START_STICKY
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun tick() {
        if (!UsageAccess.isGranted(this)) return

        val today = java.time.LocalDate.now().toString()
        if (usage.date != today) {
            usage = DailyUsage(date = today)
            scope.launch { dataStore.ensureTodayUsage() }
        }

        val pkg = ForegroundAppTracker.currentPackage(this) ?: return
        val nowElapsed = SystemClock.elapsedRealtime()
        val elapsedSeconds = ((nowElapsed - lastSampleElapsed) / 1000L).coerceAtLeast(0L)
        lastSampleElapsed = nowElapsed

        if (pkg == packageName) {
            lastForegroundPackage = pkg
            return
        }

        if (!TrackedPackages.isTracked(pkg)) {
            lastForegroundPackage = pkg
            return
        }

        val appName = TrackedPackages.displayName(pkg) ?: return
        if (elapsedSeconds > 0L && elapsedSeconds < 10L) {
            scope.launch {
                dataStore.addTotalAppUsage(appName, elapsedSeconds)
                val config = BlockPolicy.appConfig(settings, appName)
                if (config.blocked) {
                    dataStore.addUsage(appName, elapsedSeconds)
                }
            }
        }

        val minute = BlockPolicy.currentMinuteOfDay()
        val verdict = BlockPolicy.evaluate(pkg, settings, usage, minute)
        val becameForeground = lastForegroundPackage != pkg
        lastForegroundPackage = pkg

        if (verdict.shouldBlock) {
            interrupt(
                kind = InterruptActivity.KIND_BLOCK,
                title = getString(
                    if (verdict.reason == BlockReason.TOTAL_LIMIT) {
                        R.string.alert_app_total_limit_title
                    } else {
                        R.string.alert_limit_title
                    }
                ),
                message = when (verdict.reason) {
                    BlockReason.TOTAL_LIMIT -> getString(
                        R.string.toast_app_total_limit_reached,
                        BlockPolicy.appConfig(settings, appName).appTotalDailyLimitMinutes,
                        appName
                    )
                    BlockReason.DAILY_LIMIT -> getString(
                        R.string.toast_limit_reached,
                        BlockPolicy.appConfig(settings, appName).dailyLimitMinutes,
                        appName
                    )
                    else -> MindfulnessProvocationEngine.getRandomReelsQuote()
                }
            )
            if (becameForeground) {
                scope.launch { dataStore.recordBlockedDistraction(300L) }
            }
            return
        }

        if (becameForeground && settings.provocationModeEnabled) {
            val last = lastAppEntryProvocation[appName] ?: 0L
            val now = System.currentTimeMillis()
            if (now - last > 210_000L) {
                lastAppEntryProvocation[appName] = now
                interrupt(
                    kind = InterruptActivity.KIND_MINDFULNESS,
                    title = getString(R.string.app_name),
                    message = MindfulnessProvocationEngine.getRandomAppEntryQuote()
                )
            }
        }
    }

    private fun interrupt(kind: String, title: String, message: String) {
        val now = System.currentTimeMillis()
        if (now - lastInterruptAt < 1_200L) return
        lastInterruptAt = now

        val intent = InterruptActivity.intent(this, kind, title, message).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to start interrupt activity directly", e)
        }
        showFullScreenNotification(intent, title, message)
    }

    private fun showFullScreenNotification(intent: Intent, title: String, message: String) {
        val pending = PendingIntent.getActivity(
            this,
            42,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_policy)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pending)
            .setFullScreenIntent(pending, true)
            .setAutoCancel(true)
            .build()
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(ALERT_ID, notification)
    }

    private fun startForegroundInternal() {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_policy)
            .setContentTitle(getString(R.string.monitor_notification_title))
            .setContentText(getString(R.string.monitor_notification_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(MONITOR_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(MONITOR_ID, notification)
        }
    }

    private fun createChannels() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_MONITOR, "Blockfy Focus", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERT, "Blockfy Alerts", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    companion object {
        private const val TAG = "BlockfyMonitor"
        private const val POLL_MS = 700L
        private const val MONITOR_ID = 1001
        private const val ALERT_ID = 1002
        const val CHANNEL_MONITOR = "blockfy_monitor"
        const val CHANNEL_ALERT = "blockfy_alerts"

        fun start(context: Context) {
            if (!UsageAccess.isGranted(context)) return
            val intent = Intent(context, AppMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
