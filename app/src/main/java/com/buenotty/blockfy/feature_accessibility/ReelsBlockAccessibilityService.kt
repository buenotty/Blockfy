package com.buenotty.blockfy.feature_accessibility

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.buenotty.blockfy.R
import com.buenotty.blockfy.datastore.AppSettings
import com.buenotty.blockfy.datastore.DailyUsage
import com.buenotty.blockfy.datastore.DataStoreManager
import com.buenotty.blockfy.feature_monitor.BlockPolicy
import com.buenotty.blockfy.feature_monitor.BlockReason
import com.buenotty.blockfy.feature_monitor.TrackedPackages
import com.buenotty.blockfy.feature_preferences.repository.models.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ReelsBlockAccessibilityService : AccessibilityService(), KoinComponent {

    private val dataStore: DataStoreManager by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var settings = AppSettings()

    @Volatile
    private var currentUsage = DailyUsage()

    private var lastActionTime = 0L
    private val debounceMillis = 700L

    private val lastReelsProvocation = mutableMapOf<String, Long>()
    private var lastAlertTime = 0L
    private val lastWarnedMinutes = mutableMapOf<String, Int>()

    private var lastCreditPkg: String? = null
    private var lastCreditElapsed = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        lockToSocialPackages()
        serviceScope.launch {
            dataStore.appSettingsFlow.collect { settings = it }
        }
        serviceScope.launch {
            dataStore.dailyUsageFlow.collect { currentUsage = it }
        }
    }

    /**
     * Banks query enabled services and treat a null packageNames filter as
     * "this service can watch us". Only mutate the filter on the live info —
     * never replace it with a blank AccessibilityServiceInfo().
     */
    private fun lockToSocialPackages() {
        try {
            val info = serviceInfo ?: return
            info.packageNames = SOCIAL_PACKAGES
            serviceInfo = info
        } catch (e: Exception) {
            Log.e(TAG, "Error locking AccessibilityServiceInfo", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null) return
            val pkg = event.packageName?.toString() ?: return
            maybeFlagAdultSite(event, pkg)
            if (!TrackedPackages.isTracked(pkg)) {
                lastCreditPkg = null
                lastCreditElapsed = 0L
                return
            }

            val today = java.time.LocalDate.now().toString()
            if (currentUsage.date != today) {
                currentUsage = DailyUsage(date = today)
                serviceScope.launch { dataStore.ensureTodayUsage() }
            }

            val appName = TrackedPackages.displayName(pkg) ?: return
            val root = rootFromEvent(event, pkg) ?: return
            try {
                handleTrackedApp(pkg, appName, root)
            } finally {
                root.recycle()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Unhandled error in onAccessibilityEvent", t)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        flushUsage()
        serviceScope.cancel()
    }

    private fun handleTrackedApp(pkg: String, appName: String, root: AccessibilityNodeInfo) {
        val appConfig = BlockPolicy.appConfig(settings, appName)
        val isShortsVisible = when (appName) {
            "Instagram" -> isNodeVisible(root, "com.instagram.android:id/clips_swipe_refresh_container")
            "YouTube" -> isNodeVisible(root, "com.google.android.youtube:id/reel_watch_fragment_root")
            "Facebook" -> isNodeVisible(root, "com.facebook.katana:id/fb_shorts_container") ||
                isNodeVisible(root, "com.facebook.katana:id/reels_viewer")
            else -> false
        }

        creditUsage(pkg, appName, isShortsVisible)

        val minute = BlockPolicy.currentMinuteOfDay()
        val totalVerdict = BlockPolicy.evaluate(pkg, settings, currentUsage, minute)
        if (totalVerdict.shouldBlock) {
            val limit = appConfig.appTotalDailyLimitMinutes
            val title = if (totalVerdict.reason == BlockReason.SCHEDULE) {
                getString(R.string.alert_limit_title)
            } else {
                getString(R.string.alert_app_total_limit_title)
            }
            val message = if (totalVerdict.reason == BlockReason.SCHEDULE) {
                getString(R.string.toast_app_window_blocked, appName)
            } else {
                getString(R.string.toast_app_total_limit_reached, limit, appName)
            }
            notifyAlert(title, message, isFinal = true)
            serviceScope.launch { dataStore.recordBlockedDistraction(300L) }
            exitTheDoom(null) { performGlobalAction(GLOBAL_ACTION_HOME) }
            return
        }

        if (BlockPolicy.isWholeAppOnly(appName)) {
            if (appName == "TikTok") {
                maybeProvoke(appName)
            }
            if (appConfig.blocked && appConfig.appTotalDailyLimitMinutes > 0) {
                checkAndNotifyRemainingTime(
                    appName,
                    BlockPolicy.totalSeconds(currentUsage, appName),
                    appConfig.appTotalDailyLimitMinutes
                )
            }
            return
        }

        if (!isShortsVisible) return

        maybeProvoke(appName)

        val shortsVerdict = BlockPolicy.evaluateShorts(appName, settings, currentUsage, minute)
        if (!shortsVerdict.shouldBlock) {
            if (appConfig.blocked && appConfig.dailyLimitMinutes > 0) {
                checkAndNotifyRemainingTime(
                    appName,
                    BlockPolicy.featureSeconds(currentUsage, appName),
                    appConfig.dailyLimitMinutes
                )
            }
            return
        }

        val title = getString(R.string.alert_limit_title)
        val msg = getString(R.string.toast_limit_reached, appConfig.dailyLimitMinutes, getString(R.string.short_videos_label))
        notifyAlert(title, msg, isFinal = true)
        serviceScope.launch { dataStore.recordBlockedDistraction(300L) }
        exitShorts(appName, root)
    }

    private fun maybeProvoke(appName: String) {
        if (!settings.provocationModeEnabled) return
        val now = System.currentTimeMillis()
        val lastReelsTime = lastReelsProvocation[appName] ?: 0L
        if (now - lastReelsTime <= 180_000L) return
        lastReelsProvocation[appName] = now
        notifyAlert(
            getString(R.string.app_name),
            MindfulnessProvocationEngine.getRandomReelsQuote(this),
            isFinal = false
        )
    }

    private val pendingTotal = mutableMapOf<String, Long>()
    private val pendingShorts = mutableMapOf<String, Long>()
    private var lastFlushElapsed = 0L

    private fun creditUsage(pkg: String, appName: String, shortsVisible: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (lastCreditPkg != pkg || lastCreditElapsed == 0L) {
            flushUsage()
            lastCreditPkg = pkg
            lastCreditElapsed = now
            lastFlushElapsed = now
            return
        }
        val delta = ((now - lastCreditElapsed) / 1000L).coerceAtMost(MAX_CREDIT_SECONDS)
        if (delta > 0L) {
            lastCreditElapsed = now
            pendingTotal.merge(appName, delta, Long::plus)
            if (shortsVisible) pendingShorts.merge(appName, delta, Long::plus)
        }
        if (now - lastFlushElapsed >= FLUSH_INTERVAL_MILLIS) flushUsage()
    }

    /** One DataStore write per interval instead of one per accessibility event. */
    private fun flushUsage() {
        lastFlushElapsed = SystemClock.elapsedRealtime()
        if (pendingTotal.isEmpty() && pendingShorts.isEmpty()) return
        val total = pendingTotal.toMap()
        val shorts = pendingShorts.toMap()
        pendingTotal.clear()
        pendingShorts.clear()
        serviceScope.launch {
            total.forEach { (app, seconds) -> dataStore.addTotalAppUsage(app, seconds) }
            shorts.forEach { (app, seconds) -> dataStore.addUsage(app, seconds) }
        }
    }

    private fun exitShorts(appName: String, root: AccessibilityNodeInfo?) {
        when (appName) {
            "Instagram" -> {
                if (root != null) {
                    try {
                        val feedTabs = root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/feed_tab")
                        val feedTab = feedTabs?.firstOrNull()
                        if (feedTab != null && !feedTab.isSelected) {
                            exitTheDoom(feedTab)
                        } else {
                            exitTheDoom(null)
                        }
                        feedTabs?.forEach { it.recycle() }
                        return
                    } catch (e: Exception) {
                        Log.e(TAG, "Error exiting Instagram Reels", e)
                    }
                }
                exitTheDoom(null)
            }
            "YouTube" -> {
                if (root != null) {
                    try {
                        val pivotBar = root.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/pivot_bar").firstOrNull()
                        val homeTab = pivotBar?.getChild(0)?.getChild(0)
                        exitTheDoom(homeTab) {
                            homeTab?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        }
                        homeTab?.recycle()
                        pivotBar?.recycle()
                        return
                    } catch (e: Exception) {
                        Log.e(TAG, "Error exiting YouTube Shorts", e)
                    }
                }
                exitTheDoom(null)
            }
            "TikTok", "X" -> exitTheDoom(null) { performGlobalAction(GLOBAL_ACTION_HOME) }
            else -> exitTheDoom(null)
        }
    }

    private fun checkAndNotifyRemainingTime(appName: String, usedSeconds: Long, limitMinutes: Int) {
        val remainingSeconds = limitMinutes * 60L - usedSeconds
        if (remainingSeconds <= 0L) return
        val remainingMinutes = ((remainingSeconds + 59L) / 60L).toInt()
        val lastWarned = lastWarnedMinutes[appName] ?: -1
        if (remainingMinutes == lastWarned) return
        val shouldWarn = remainingMinutes <= 5 || remainingMinutes % 5 == 0
        if (!shouldWarn) return
        lastWarnedMinutes[appName] = remainingMinutes
        val minText = if (remainingMinutes == 1) {
            getString(R.string.toast_remaining_one_minute, appName)
        } else {
            getString(R.string.toast_remaining_minutes, remainingMinutes, appName)
        }
        notifyAlert(getString(R.string.alert_warning_title), minText, isFinal = false)
    }

    private fun exitTheDoom(node: AccessibilityNodeInfo?, extra: (() -> Unit)? = null) {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < debounceMillis) return
        lastActionTime = now
        try {
            if (node != null) {
                val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (!clicked) performGlobalAction(GLOBAL_ACTION_BACK)
            } else {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            extra?.invoke()
        } catch (e: Exception) {
            Log.e(TAG, "Error in exitTheDoom", e)
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private fun notifyAlert(title: String, message: String, isFinal: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!isFinal && now - lastAlertTime < 4000L) return
        lastAlertTime = now
        triggerVibration(isFinal)
        showNotification(title, message, isFinal)
        showToast(message)
    }

    private fun triggerVibration(isFinal: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.let { v ->
                if (!v.hasVibrator()) return@let
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = if (isFinal) longArrayOf(0, 180, 100, 220) else longArrayOf(0, 100)
                    val amplitudes = if (isFinal) intArrayOf(0, 255, 0, 255) else intArrayOf(0, 180)
                    v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(if (isFinal) 350L else 120L)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error vibrating", e)
        }
    }

    private fun showNotification(title: String, message: String, isFinal: Boolean) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "blockfy_alerts_channel"
            notificationManager.createNotificationChannel(
                NotificationChannel(channelId, getString(R.string.focus_alerts_channel_name), NotificationManager.IMPORTANCE_HIGH)
            )
            val builder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_policy)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
            notificationManager.notify(if (isFinal) 1001 else 1002, builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification", e)
        }
    }

    private fun showToast(message: String) {
        serviceScope.launch(Dispatchers.Main) {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun isNodeVisible(root: AccessibilityNodeInfo, viewId: String): Boolean {
        return try {
            val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
            if (nodes.isNullOrEmpty()) return false
            val visible = nodes.any { it.isVisibleToUser }
            nodes.forEach { it.recycle() }
            visible
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Walk up from the event source. Never inspect whichever window is in
     * front — that would include Nubank after the user switches apps.
     */
    private fun rootFromEvent(event: AccessibilityEvent, expectedPkg: String): AccessibilityNodeInfo? {
        var node = event.source ?: return null
        if (node.packageName?.toString() != expectedPkg) {
            node.recycle()
            return null
        }
        while (true) {
            val parent = try {
                node.parent
            } catch (_: Exception) {
                null
            } ?: return node
            if (parent.packageName?.toString() != expectedPkg) {
                parent.recycle()
                return node
            }
            node.recycle()
            node = parent
        }
    }

    private var lastAdultScanElapsed = 0L

    /**
     * Reads on-screen and typed text only inside apps that commonly carry
     * adult content, and only on window changes or typing. Scrolling events
     * are ignored so the service does not walk the tree on every frame.
     */
    /**
     * Adult-site blocking reads ONLY the browser's address bar, not any other
     * text on screen and not what you type inside apps. It never inspects
     * banking, messaging or social-app content.
     */
    private fun maybeFlagAdultSite(event: AccessibilityEvent, pkg: String) {
        if (!settings.adultContentBlockerEnabled) return
        if (pkg !in AdultContentDetector.BROWSER_PACKAGES) return
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }
        val now = SystemClock.elapsedRealtime()
        if (now - lastAdultScanElapsed < 1500L) return
        lastAdultScanElapsed = now

        val root = rootFromEvent(event, pkg) ?: return
        try {
            val addressBarText = findAddressBarText(root, remaining = intArrayOf(MAX_ADDRESS_BAR_SCAN_NODES))
            if (addressBarText != null && AdultContentDetector.isAdultContent(addressBarText)) {
                leaveAdultScreen()
            }
        } finally {
            root.recycle()
        }
    }

    /**
     * The address bar is the one editable field browsers keep visible at the
     * top of the window. We do not know each browser's exact view id, so we
     * look for a visible, non-password EditText instead.
     */
    private fun findAddressBarText(node: AccessibilityNodeInfo, remaining: IntArray): String? {
        if (remaining[0] <= 0) return null
        remaining[0]--
        if (node.isVisibleToUser && !node.isPassword && node.className == "android.widget.EditText") {
            val text = node.text?.toString()
            if (!text.isNullOrBlank()) return text
        }
        for (i in 0 until node.childCount) {
            if (remaining[0] <= 0) return null
            val child = node.getChild(i) ?: continue
            try {
                val found = findAddressBarText(child, remaining)
                if (found != null) return found
            } finally {
                child.recycle()
            }
        }
        return null
    }


    private fun leaveAdultScreen() {
        val message = AdultContentDetector.getRandomWarning(this)
        notifyAlert(getString(R.string.adult_blocker_title), message, isFinal = true)
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    companion object {
        private const val TAG = "BlockfyService"
        private const val MAX_CREDIT_SECONDS = 3L
        private const val FLUSH_INTERVAL_MILLIS = 10_000L
        private const val MAX_ADDRESS_BAR_SCAN_NODES = 40
        val SOCIAL_PACKAGES: Array<String> = (
            TrackedPackages.ALL.keys + AdultContentDetector.BROWSER_PACKAGES
            ).distinct().toTypedArray()
    }
}
