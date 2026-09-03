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

    private var instaTrackingJob: Job? = null
    private var ytTrackingJob: Job? = null

    private var lastAlertTime = 0L
    private var lastWarnedInstaMinute = -1
    private var lastWarnedYtMinute = -1

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
                "com.instagram.android" -> handleInstagram(root)
                "com.google.android.youtube" -> handleYouTube(root)
                "com.zhiliaoapp.musically" -> handleTikTok()
                else -> {
                    // If user switched to another app, stop active video tracking
                    stopTracking()
                }
            }
        } catch (t: Throwable) {
            Log.e("BlockfyService", "Unhandled error in onAccessibilityEvent", t)
        }
    }

    override fun onInterrupt() {
        stopTracking()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
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

    private fun handleInstagram(root: AccessibilityNodeInfo) {
        val insta = settings.instagram
        val isReelsVisible = isNodeVisible(root, "com.instagram.android:id/clips_swipe_refresh_container")

        if (!isReelsVisible) {
            // User is in Instagram but not in Reels
            stopInstagramTracking()
            return
        }

        if (!insta.blocked) {
            return
        }

        val nowMin = currentMinuteOfDay()
        if (!isWithinInterval(insta.blockedStart, insta.blockedEnd, nowMin)) {
            // Outside blocked schedule
            return
        }

        // Check daily limit if configured
        if (insta.dailyLimitMinutes > 0) {
            val todayUsedSeconds = if (currentUsage.date == getTodayDate()) currentUsage.instagramSeconds else 0L
            val limitSeconds = insta.dailyLimitMinutes * 60L

            if (todayUsedSeconds >= limitSeconds) {
                val title = getString(R.string.alert_limit_title)
                val msg = getString(R.string.toast_limit_reached, insta.dailyLimitMinutes, "Reels")
                notifyAlert(title, msg, isFinal = true)
                exitInstagramReels(root)
            } else {
                // Within daily limit: allow viewing and track usage
                startInstagramTracking(insta.dailyLimitMinutes)
            }
        } else {
            // No daily limit: block immediately
            val title = getString(R.string.alert_limit_title)
            val msg = getString(R.string.toast_limit_reached, insta.dailyLimitMinutes, "Reels")
            notifyAlert(title, msg, isFinal = true)
            exitInstagramReels(root)
        }
    }

    private fun exitInstagramReels(root: AccessibilityNodeInfo?) {
        stopInstagramTracking()

        if (root == null) {
            exitTheDoom(null)
            return
        }

        try {
            val feedTabs = root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/feed_tab")
            val feedTab = feedTabs?.firstOrNull()

            if (feedTab != null && !feedTab.isSelected) {
                exitTheDoom(feedTab)
            } else {
                exitTheDoom(null)
            }
            feedTabs?.forEach { it.recycle() }
        } catch (e: Exception) {
            Log.e("BlockfyService", "Error in exitInstagramReels", e)
            exitTheDoom(null)
        }
    }

    private fun startInstagramTracking(limitMinutes: Int) {
        if (instaTrackingJob?.isActive == true) return

        instaTrackingJob = serviceScope.launch {
            var localUsedSeconds = if (currentUsage.date == getTodayDate()) currentUsage.instagramSeconds else 0L

            while (isActive) {
                delay(1000L)
                try {
                    val root = rootInActiveWindow
                    val stillVisible = root != null && isNodeVisible(root, "com.instagram.android:id/clips_swipe_refresh_container")
                    if (stillVisible) {
                        localUsedSeconds++
                        dataStore.addUsage("Instagram", 1L)

                        if (localUsedSeconds >= limitMinutes * 60L) {
                            serviceScope.launch(Dispatchers.Main) {
                                val title = getString(R.string.alert_limit_title)
                                val msg = getString(R.string.toast_limit_reached, limitMinutes, "Reels")
                                notifyAlert(title, msg, isFinal = true)
                                exitInstagramReels(rootInActiveWindow)
                            }
                            break
                        } else {
                            checkAndNotifyRemainingTime("Reels", localUsedSeconds, limitMinutes, isInsta = true)
                        }
                    } else {
                        break
                    }
                } catch (e: Exception) {
                    Log.e("BlockfyService", "Error in instaTrackingJob", e)
                }
            }
        }
    }

    private fun stopInstagramTracking() {
        instaTrackingJob?.cancel()
        instaTrackingJob = null
    }

    private fun handleYouTube(root: AccessibilityNodeInfo) {
        val yt = settings.youtube
        val isShortsVisible = isNodeVisible(root, "com.google.android.youtube:id/reel_watch_fragment_root")

        if (!isShortsVisible) {
            stopYouTubeTracking()
            return
        }

        if (!yt.blocked) {
            return
        }

        val nowMin = currentMinuteOfDay()
        if (!isWithinInterval(yt.blockedStart, yt.blockedEnd, nowMin)) {
            return
        }

        if (yt.dailyLimitMinutes > 0) {
            val todayUsedSeconds = if (currentUsage.date == getTodayDate()) currentUsage.youtubeSeconds else 0L
            val limitSeconds = yt.dailyLimitMinutes * 60L

            if (todayUsedSeconds >= limitSeconds) {
                val title = getString(R.string.alert_limit_title)
                val msg = getString(R.string.toast_limit_reached, yt.dailyLimitMinutes, "Shorts")
                notifyAlert(title, msg, isFinal = true)
                exitYouTubeShorts(root)
            } else {
                startYouTubeTracking(yt.dailyLimitMinutes)
            }
        } else {
            val title = getString(R.string.alert_limit_title)
            val msg = getString(R.string.toast_limit_reached, yt.dailyLimitMinutes, "Shorts")
            notifyAlert(title, msg, isFinal = true)
            exitYouTubeShorts(root)
        }
    }

    private fun exitYouTubeShorts(root: AccessibilityNodeInfo?) {
        stopYouTubeTracking()

        if (root == null) {
            exitTheDoom(null)
            return
        }

        try {
            val pivotBar = root.findAccessibilityNodeInfosByViewId(
                "com.google.android.youtube:id/pivot_bar"
            ).firstOrNull()
            val homeTab = pivotBar?.getChild(0)?.getChild(0)

            exitTheDoom(homeTab) {
                homeTab?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            homeTab?.recycle()
            pivotBar?.recycle()
        } catch (e: Exception) {
            Log.e("BlockfyService", "Error in exitYouTubeShorts", e)
            exitTheDoom(null)
        }
    }

    private fun startYouTubeTracking(limitMinutes: Int) {
        if (ytTrackingJob?.isActive == true) return

        ytTrackingJob = serviceScope.launch {
            var localUsedSeconds = if (currentUsage.date == getTodayDate()) currentUsage.youtubeSeconds else 0L

            while (isActive) {
                delay(1000L)
                try {
                    val root = rootInActiveWindow
                    val stillVisible = root != null && isNodeVisible(root, "com.google.android.youtube:id/reel_watch_fragment_root")
                    if (stillVisible) {
                        localUsedSeconds++
                        dataStore.addUsage("YouTube", 1L)

                        if (localUsedSeconds >= limitMinutes * 60L) {
                            serviceScope.launch(Dispatchers.Main) {
                                val title = getString(R.string.alert_limit_title)
                                val msg = getString(R.string.toast_limit_reached, limitMinutes, "Shorts")
                                notifyAlert(title, msg, isFinal = true)
                                exitYouTubeShorts(rootInActiveWindow)
                            }
                            break
                        } else {
                            checkAndNotifyRemainingTime("Shorts", localUsedSeconds, limitMinutes, isInsta = false)
                        }
                    } else {
                        break
                    }
                } catch (e: Exception) {
                    Log.e("BlockfyService", "Error in ytTrackingJob", e)
                }
            }
        }
    }

    private fun checkAndNotifyRemainingTime(featureName: String, usedSeconds: Long, limitMinutes: Int, isInsta: Boolean) {
        try {
            val totalSeconds = limitMinutes * 60L
            val remainingSeconds = totalSeconds - usedSeconds
            if (remainingSeconds <= 0L) return

            val remainingMinutes = ((remainingSeconds + 59L) / 60L).toInt()
            val lastWarned = if (isInsta) lastWarnedInstaMinute else lastWarnedYtMinute

            if (remainingMinutes != lastWarned) {
                val shouldWarn = when {
                    remainingMinutes <= 5 -> true
                    remainingMinutes % 5 == 0 -> true
                    else -> false
                }

                if (shouldWarn) {
                    if (isInsta) lastWarnedInstaMinute = remainingMinutes else lastWarnedYtMinute = remainingMinutes
                    val title = getString(R.string.alert_warning_title)
                    val minText = if (remainingMinutes == 1) {
                        getString(R.string.toast_remaining_one_minute, featureName)
                    } else {
                        getString(R.string.toast_remaining_minutes, remainingMinutes, featureName)
                    }
                    notifyAlert(title, minText, isFinal = false)
                }
            }
        } catch (e: Exception) {
            Log.e("BlockfyService", "Error in checkAndNotifyRemainingTime", e)
        }
    }

    private fun stopYouTubeTracking() {
        ytTrackingJob?.cancel()
        ytTrackingJob = null
    }

    private fun handleTikTok() {
        val tt = settings.tiktok
        val nowMin = currentMinuteOfDay()
        if (tt.blocked && isWithinInterval(tt.blockedStart, tt.blockedEnd, nowMin)) {
            exitTheDoom(null) {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }
    }

    private fun stopTracking() {
        stopInstagramTracking()
        stopYouTubeTracking()
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

    /**
     * Centralized alert system:
     * 1. Haptic feedback (Vibration)
     * 2. Visual floating pill banner directly on screen (TYPE_ACCESSIBILITY_OVERLAY)
     * 3. System Heads-Up Notification
     * 4. Android Toast
     */
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

    private fun showOverlayBanner(title: String, message: String, isFinal: Boolean) {
        serviceScope.launch(Dispatchers.Main) {
            try {
                val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                removeOverlayView()

                val density = resources.displayMetrics.density
                fun dp(dp: Int) = (dp * density).toInt()

                val container = LinearLayout(this@ReelsBlockAccessibilityService).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    val padH = dp(16)
                    val padV = dp(12)
                    setPadding(padH, padV, padH, padV)
                    elevation = dp(12).toFloat()

                    val shape = GradientDrawable().apply {
                        this.shape = GradientDrawable.RECTANGLE
                        cornerRadius = dp(20).toFloat()
                        setColor(if (isFinal) Color.parseColor("#1C132A") else Color.parseColor("#101528"))
                        setStroke(dp(2), if (isFinal) Color.parseColor("#FF5252") else Color.parseColor("#7C83FD"))
                    }
                    background = shape
                }

                val iconView = TextView(this@ReelsBlockAccessibilityService).apply {
                    text = if (isFinal) "🛑" else "⏳"
                    textSize = 22f
                    val marginEnd = dp(12)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, marginEnd, 0)
                    }
                }
                container.addView(iconView)

                val textColumn = LinearLayout(this@ReelsBlockAccessibilityService).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val titleView = TextView(this@ReelsBlockAccessibilityService).apply {
                    text = title
                    textSize = 14f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(if (isFinal) Color.parseColor("#FF8A80") else Color.parseColor("#B4B9FE"))
                }
                textColumn.addView(titleView)

                val msgView = TextView(this@ReelsBlockAccessibilityService).apply {
                    text = message
                    textSize = 12f
                    setTextColor(Color.WHITE)
                }
                textColumn.addView(msgView)

                container.addView(textColumn)

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

                // Auto dismiss after 4.5 seconds
                serviceScope.launch(Dispatchers.Main) {
                    delay(4500L)
                    if (currentOverlayView == wrapper) {
                        removeOverlayView()
                    }
                }
            } catch (e: Exception) {
                Log.e("BlockfyService", "Error showing overlay banner", e)
            }
        }
    }

    private fun removeOverlayView() {
        try {
            currentOverlayView?.let {
                val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                wm.removeView(it)
                currentOverlayView = null
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
