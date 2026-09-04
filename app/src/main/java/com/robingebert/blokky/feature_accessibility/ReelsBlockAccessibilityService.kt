package com.robingebert.blokky.feature_accessibility

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.robingebert.blokky.R
import com.robingebert.blokky.datastore.AppSettings
import com.robingebert.blokky.datastore.DailyUsage
import com.robingebert.blokky.datastore.DataStoreManager
import com.robingebert.blokky.feature_preferences.repository.models.App
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
import java.time.LocalDate
import java.util.TimeZone

class ReelsBlockAccessibilityService : AccessibilityService(), KoinComponent {

    private val dataStore: DataStoreManager by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var settings = AppSettings()

    @Volatile
    private var currentUsage = DailyUsage()

    private var lastActionTime = 0L
    private val debounceMillis = 700L

    // Debounce maps for Mindfulness Provocation Mode
    private val lastAppEntryProvocation = mutableMapOf<String, Long>()
    private val lastReelsProvocation = mutableMapOf<String, Long>()

    // Tracking jobs
    private var activeAppTotalTrackingJob: Job? = null
    private var activeShortsTrackingJob: Job? = null
    private var currentActivePackage: String? = null

    private var lastAlertTime = 0L
    private val lastWarnedMinutes = mutableMapOf<String, Int>()

    private var currentOverlayView: View? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceScope.launch {
            try {
                dataStore.appSettingsFlow.collect { latest ->
                    settings = latest
                }
            } catch (e: Exception) {
                Log.e("BlockfyService", "Error collecting app settings", e)
            }
        }
        serviceScope.launch {
            try {
                dataStore.dailyUsageFlow.collect { latest ->
                    currentUsage = latest
                }
            } catch (e: Exception) {
                Log.e("BlockfyService", "Error collecting daily usage", e)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            val pkg = event?.packageName?.toString() ?: return
            val root = rootInActiveWindow ?: return

            when (pkg) {
                "com.instagram.android" -> handleTrackedApp(pkg, "Instagram", settings.instagram, root)
                "com.google.android.youtube" -> handleTrackedApp(pkg, "YouTube", settings.youtube, root)
                "com.zhiliaoapp.musically" -> handleTrackedApp(pkg, "TikTok", settings.tiktok, root)
                "com.facebook.katana" -> handleTrackedApp(pkg, "Facebook", settings.facebook, root)
                "com.twitter.android" -> handleTrackedApp(pkg, "X", settings.x, root)
                else -> {
                    // Switched to unrelated app
                    if (currentActivePackage != null) {
                        stopAllTracking()
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e("BlockfyService", "Unhandled error in onAccessibilityEvent", t)
        }
    }

    override fun onInterrupt() {
        stopAllTracking()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAllTracking()
        serviceScope.cancel()
        removeOverlayView()
    }

    private fun getTodayDate(): String = LocalDate.now().toString()

    private fun currentMinuteOfDay(): Int {
        val now = System.currentTimeMillis()
        val offset = TimeZone.getDefault().getOffset(now)
        return (((now + offset) / 60000) % 1440).toInt()
    }

    private fun isWithinInterval(start: Int, end: Int, minute: Int): Boolean {
        return if (start <= end) {
            minute in start..end
        } else {
            minute >= start || minute <= end
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

    private fun isNodeWithTextVisible(root: AccessibilityNodeInfo, text: String): Boolean {
        return try {
            val nodes = root.findAccessibilityNodeInfosByText(text)
            if (nodes.isNullOrEmpty()) return false
            val visible = nodes.any { it.isVisibleToUser }
            nodes.forEach { it.recycle() }
            visible
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Master handler for tracked apps supporting:
     * 1. Mindfulness Provocation Engine (App Entry & Reels Entry)
     * 2. Total App Daily Limit Tracking (closes whole app if exceeded)
     * 3. Short Video / Reels Detection & Limit Tracking (closes Reels if exceeded)
     */
    private fun handleTrackedApp(pkg: String, appName: String, appConfig: App, root: AccessibilityNodeInfo) {
        val now = System.currentTimeMillis()

        // 1. Provocation on App Entry (Mindfulness to snap out of automatic loop)
        if (settings.provocationModeEnabled) {
            val lastEntryTime = lastAppEntryProvocation[appName] ?: 0L
            if (now - lastEntryTime > 210_000L) { // At most once every 3.5 min
                lastAppEntryProvocation[appName] = now
                val quote = MindfulnessProvocationEngine.getRandomAppEntryQuote()
                showOverlayBanner("Blockfy", quote, isFinal = false, customBorderColor = Color.parseColor("#7C83FD"))
            }
        }

        // 2. Total App Daily Limit Enforcement
        if (appConfig.appTotalDailyLimitMinutes > 0) {
            val totalSecondsUsed = when (appName) {
                "Instagram" -> currentUsage.instagramTotalSeconds
                "YouTube" -> currentUsage.youtubeTotalSeconds
                "TikTok" -> currentUsage.tiktokTotalSeconds
                "Facebook" -> currentUsage.facebookTotalSeconds
                "X" -> currentUsage.xTotalSeconds
                else -> 0L
            }

            if (totalSecondsUsed >= appConfig.appTotalDailyLimitMinutes * 60L) {
                // Total app limit exhausted: close app completely
                notifyAlert(
                    getString(R.string.alert_app_total_limit_title),
                    getString(R.string.toast_app_total_limit_reached, appConfig.appTotalDailyLimitMinutes, appName),
                    isFinal = true
                )
                serviceScope.launch {
                    dataStore.recordBlockedDistraction(300L)
                }
                exitTheDoom(null) {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
                stopAllTracking()
                return
            } else {
                startAppTotalTracking(appName, appConfig.appTotalDailyLimitMinutes)
            }
        }

        // 3. Detect Short Videos (Reels / Shorts / TikTok)
        val isShortsVisible = when (appName) {
            "Instagram" -> isNodeVisible(root, "com.instagram.android:id/clips_swipe_refresh_container")
            "YouTube" -> isNodeVisible(root, "com.google.android.youtube:id/reel_watch_fragment_root")
            "TikTok" -> true // TikTok is 100% short videos
            "Facebook" -> isNodeVisible(root, "com.facebook.katana:id/fb_shorts_container") ||
                    isNodeVisible(root, "com.facebook.katana:id/reels_viewer") ||
                    isNodeWithTextVisible(root, "Reels")
            else -> false
        }

        if (!isShortsVisible) {
            stopShortsTracking()
            return
        }

        // 4. Provocation on Short Videos Entry (Deep Reflection)
        if (settings.provocationModeEnabled) {
            val lastReelsTime = lastReelsProvocation[appName] ?: 0L
            if (now - lastReelsTime > 180_000L) { // At most once every 3 min
                lastReelsProvocation[appName] = now
                val reflectionQuote = MindfulnessProvocationEngine.getRandomReelsQuote()
                showOverlayBanner("Blockfy", reflectionQuote, isFinal = false, customBorderColor = Color.parseColor("#7C83FD"))
            }
        }

        // 5. Short Video Blocking & Daily Limit
        if (!appConfig.blocked) {
            return
        }

        val nowMin = currentMinuteOfDay()
        if (!isWithinInterval(appConfig.blockedStart, appConfig.blockedEnd, nowMin)) {
            return
        }

        val todayShortsSeconds = when (appName) {
            "Instagram" -> currentUsage.instagramSeconds
            "YouTube" -> currentUsage.youtubeSeconds
            "TikTok" -> currentUsage.tiktokSeconds
            "Facebook" -> currentUsage.facebookSeconds
            else -> 0L
        }

        if (appConfig.dailyLimitMinutes > 0) {
            val limitSeconds = appConfig.dailyLimitMinutes * 60L
            if (todayShortsSeconds >= limitSeconds) {
                val title = getString(R.string.alert_limit_title)
                val msg = getString(R.string.toast_limit_reached, appConfig.dailyLimitMinutes, "Vídeos Curtos")
                notifyAlert(title, msg, isFinal = true)
                serviceScope.launch {
                    dataStore.recordBlockedDistraction(300L)
                }
                exitShorts(appName, root)
            } else {
                startShortsTracking(appName, appConfig.dailyLimitMinutes)
            }
        } else {
            // Blocked immediately (no daily allowance)
            val title = getString(R.string.alert_limit_title)
            val msg = getString(R.string.toast_limit_reached, appConfig.dailyLimitMinutes, "Vídeos Curtos")
            notifyAlert(title, msg, isFinal = true)
            serviceScope.launch {
                dataStore.recordBlockedDistraction(300L)
            }
            exitShorts(appName, root)
        }
    }

    private fun startAppTotalTracking(appName: String, limitMinutes: Int) {
        if (currentActivePackage == appName && activeAppTotalTrackingJob?.isActive == true) return
        currentActivePackage = appName

        activeAppTotalTrackingJob?.cancel()
        activeAppTotalTrackingJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                dataStore.addTotalAppUsage(appName, 1L)
            }
        }
    }

    private fun startShortsTracking(appName: String, limitMinutes: Int) {
        if (activeShortsTrackingJob?.isActive == true) return

        activeShortsTrackingJob = serviceScope.launch {
            var localSeconds = when (appName) {
                "Instagram" -> currentUsage.instagramSeconds
                "YouTube" -> currentUsage.youtubeSeconds
                "TikTok" -> currentUsage.tiktokSeconds
                "Facebook" -> currentUsage.facebookSeconds
                else -> 0L
            }

            while (isActive) {
                delay(1000L)
                localSeconds++
                dataStore.addUsage(appName, 1L)

                if (localSeconds >= limitMinutes * 60L) {
                    serviceScope.launch(Dispatchers.Main) {
                        val title = getString(R.string.alert_limit_title)
                        val msg = getString(R.string.toast_limit_reached, limitMinutes, "Vídeos Curtos")
                        notifyAlert(title, msg, isFinal = true)
                        dataStore.recordBlockedDistraction(300L)
                        exitShorts(appName, rootInActiveWindow)
                    }
                    break
                } else {
                    checkAndNotifyRemainingTime(appName, localSeconds, limitMinutes)
                }
            }
        }
    }

    private fun stopShortsTracking() {
        activeShortsTrackingJob?.cancel()
        activeShortsTrackingJob = null
    }

    private fun stopAllTracking() {
        activeAppTotalTrackingJob?.cancel()
        activeAppTotalTrackingJob = null
        activeShortsTrackingJob?.cancel()
        activeShortsTrackingJob = null
        currentActivePackage = null
    }

    private fun exitShorts(appName: String, root: AccessibilityNodeInfo?) {
        stopShortsTracking()

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
                        Log.e("BlockfyService", "Error exiting Instagram Reels", e)
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
                        Log.e("BlockfyService", "Error exiting YouTube Shorts", e)
                    }
                }
                exitTheDoom(null)
            }
            "Facebook" -> {
                // Exit Facebook Reels to feed/home
                exitTheDoom(null)
            }
            "TikTok" -> {
                exitTheDoom(null) {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
            }
            else -> {
                exitTheDoom(null)
            }
        }
    }

    private fun checkAndNotifyRemainingTime(appName: String, usedSeconds: Long, limitMinutes: Int) {
        try {
            val totalSeconds = limitMinutes * 60L
            val remainingSeconds = totalSeconds - usedSeconds
            if (remainingSeconds <= 0L) return

            val remainingMinutes = ((remainingSeconds + 59L) / 60L).toInt()
            val lastWarned = lastWarnedMinutes[appName] ?: -1

            if (remainingMinutes != lastWarned) {
                val shouldWarn = when {
                    remainingMinutes <= 5 -> true
                    remainingMinutes % 5 == 0 -> true
                    else -> false
                }

                if (shouldWarn) {
                    lastWarnedMinutes[appName] = remainingMinutes
                    val title = getString(R.string.alert_warning_title)
                    val minText = if (remainingMinutes == 1) {
                        getString(R.string.toast_remaining_one_minute, appName)
                    } else {
                        getString(R.string.toast_remaining_minutes, remainingMinutes, appName)
                    }
                    notifyAlert(title, minText, isFinal = false)
                }
            }
        } catch (e: Exception) {
            Log.e("BlockfyService", "Error in checkAndNotifyRemainingTime", e)
        }
    }

    private fun exitTheDoom(
        node: AccessibilityNodeInfo?,
        extra: (() -> Unit)? = null
    ) {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < debounceMillis) return
        lastActionTime = now

        try {
            if (node != null) {
                val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (!clicked) {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
            } else {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            extra?.invoke()
        } catch (e: Exception) {
            Log.e("BlockfyService", "Error in exitTheDoom", e)
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private fun notifyAlert(title: String, message: String, isFinal: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!isFinal && now - lastAlertTime < 4000L) return
        lastAlertTime = now

        triggerVibration(isFinal)
        showOverlayBanner(title, message, isFinal)
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
                if (v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val timings = if (isFinal) longArrayOf(0, 180, 100, 220) else longArrayOf(0, 100)
                        val amplitudes = if (isFinal) intArrayOf(0, 255, 0, 255) else intArrayOf(0, 180)
                        v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(if (isFinal) 350L else 120L)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BlockfyService", "Error vibrating", e)
        }
    }

    private fun showOverlayBanner(
        title: String,
        message: String,
        isFinal: Boolean,
        customBorderColor: Int? = null
    ) {
        serviceScope.launch(Dispatchers.Main) {
            try {
                val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                dismissCurrentOverlayImmediate()

                val density = resources.displayMetrics.density
                fun dp(dp: Int) = (dp * density).toInt()

                val container = LinearLayout(this@ReelsBlockAccessibilityService).apply {
                    orientation = LinearLayout.VERTICAL
                    val padH = dp(18)
                    val padV = dp(14)
                    setPadding(padH, padV, padH, padV)
                    elevation = dp(16).toFloat()

                    val strokeColor = customBorderColor ?: if (isFinal) Color.parseColor("#FF5252") else Color.parseColor("#7C83FD")
                    val bgColor = if (isFinal) Color.parseColor("#1B1226") else Color.parseColor("#101426")

                    val shape = GradientDrawable().apply {
                        this.shape = GradientDrawable.RECTANGLE
                        cornerRadius = dp(18).toFloat()
                        setColor(bgColor)
                        setStroke(dp(1).coerceAtLeast(1), strokeColor)
                    }
                    background = shape

                    // Start invisible and slightly above for smooth slide-in
                    alpha = 0f
                    translationY = -dp(24).toFloat()
                }

                val titleView = TextView(this@ReelsBlockAccessibilityService).apply {
                    text = title.uppercase()
                    textSize = 11f
                    letterSpacing = 0.08f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(if (isFinal) Color.parseColor("#FF6E6E") else Color.parseColor("#9EA6FF"))
                }
                container.addView(titleView)

                val msgView = TextView(this@ReelsBlockAccessibilityService).apply {
                    text = message
                    textSize = 13f
                    setTypeface(typeface, Typeface.NORMAL)
                    setTextColor(Color.parseColor("#F8FAFC"))
                    setLineSpacing(dp(3).toFloat(), 1f)
                    val marginTop = dp(4)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, marginTop, 0, 0)
                    }
                }
                container.addView(msgView)

                val wrapper = FrameLayout(this@ReelsBlockAccessibilityService).apply {
                    val marginH = dp(16)
                    setPadding(marginH, 0, marginH, 0)
                    addView(container)
                }

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    y = dp(52)
                }

                wm.addView(wrapper, params)
                currentOverlayView = wrapper

                // Smooth Entrance Animation (fade in & slide down)
                container.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(350L)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()

                // Auto dismiss with Smooth Exit Animation after 4.5 seconds
                serviceScope.launch(Dispatchers.Main) {
                    delay(4500L)
                    if (currentOverlayView == wrapper) {
                        container.animate()
                            .alpha(0f)
                            .translationY(-dp(24).toFloat())
                            .setDuration(300L)
                            .setInterpolator(android.view.animation.AccelerateInterpolator())
                            .withEndAction {
                                removeOverlayView(wrapper)
                            }
                            .start()
                    }
                }
            } catch (e: Exception) {
                Log.e("BlockfyService", "Error showing overlay banner", e)
            }
        }
    }

    private fun dismissCurrentOverlayImmediate() {
        try {
            currentOverlayView?.let { view ->
                val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                wm.removeView(view)
                currentOverlayView = null
            }
        } catch (e: Exception) {
            currentOverlayView = null
        }
    }

    private fun removeOverlayView(specificView: View? = null) {
        try {
            val target = specificView ?: currentOverlayView
            if (target != null) {
                if (currentOverlayView == target) {
                    currentOverlayView = null
                }
                val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                wm.removeView(target)
            }
        } catch (e: Exception) {
            currentOverlayView = null
        }
    }

    private fun showNotification(title: String, message: String, isFinal: Boolean) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "blockfy_alerts_channel"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Alertas do Blockfy",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alertas de limite de tempo e avisos do Blockfy"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val builder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_blockfy_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)

            notificationManager.notify(if (isFinal) 1001 else 1002, builder.build())
        } catch (e: Exception) {
            Log.e("BlockfyService", "Error showing notification", e)
        }
    }

    private fun showToast(message: String) {
        try {
            serviceScope.launch(Dispatchers.Main) {
                try {
                    Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Log.e("BlockfyService", "Error displaying toast", e)
                }
            }
        } catch (e: Exception) {
            Log.e("BlockfyService", "Error in showToast", e)
        }
    }
}
