package com.robingebert.blokky.feature_accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
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

    private var lastToastTime = 0L
    private var lastWarnedInstaMinute = -1
    private var lastWarnedYtMinute = -1

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
                showToast(getString(R.string.toast_limit_reached, insta.dailyLimitMinutes, "Reels"))
                exitInstagramReels(root)
            } else {
                // Within daily limit: allow viewing and track usage
                startInstagramTracking(insta.dailyLimitMinutes)
            }
        } else {
            // No daily limit: block immediately
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

            // Only click feedTab if it exists AND is NOT already selected.
            // If feedTab is already selected, clicking it reloads and scrolls to top in Instagram!
            if (feedTab != null && !feedTab.isSelected) {
                exitTheDoom(feedTab)
            } else {
                // Modal reel or already on feed tab -> use BACK action to exit overlay safely
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
                                showToast(getString(R.string.toast_limit_reached, limitMinutes, "Reels"))
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
                showToast(getString(R.string.toast_limit_reached, yt.dailyLimitMinutes, "Shorts"))
                exitYouTubeShorts(root)
            } else {
                startYouTubeTracking(yt.dailyLimitMinutes)
            }
        } else {
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
                                showToast(getString(R.string.toast_limit_reached, limitMinutes, "Shorts"))
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
                    val minText = if (remainingMinutes == 1) {
                        getString(R.string.toast_remaining_one_minute, featureName)
                    } else {
                        getString(R.string.toast_remaining_minutes, remainingMinutes, featureName)
                    }
                    showToast(minText)
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

    private fun showToast(message: String) {
        try {
            val now = System.currentTimeMillis()
            if (now - lastToastTime < 3000L) return
            lastToastTime = now
            serviceScope.launch(Dispatchers.Main) {
                try {
                    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("BlockfyService", "Error displaying toast", e)
                }
            }
        } catch (e: Exception) {
            Log.e("BlockfyService", "Error in showToast", e)
        }
    }
}
