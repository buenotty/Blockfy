package com.buenotty.blockfy.feature_accessibility

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.IntentFilter
import android.content.Intent
import android.content.BroadcastReceiver
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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.buenotty.blockfy.feature_accessibility.overlay.ChallengeBank
import com.buenotty.blockfy.feature_accessibility.overlay.ChallengeOverlay
import com.buenotty.blockfy.feature_monitor.ListeningScope
import com.buenotty.blockfy.R
import com.buenotty.blockfy.datastore.AppSettings
import com.buenotty.blockfy.datastore.DailyUsage
import com.buenotty.blockfy.datastore.DataStoreManager
import com.buenotty.blockfy.feature_monitor.BlockPolicy
import com.buenotty.blockfy.feature_monitor.BlockReason
import com.buenotty.blockfy.feature_monitor.BlockVerdict
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

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.buenotty.blockfy.AppLocale.wrap(newBase))
    }

    private val dataStore: DataStoreManager by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var settings = AppSettings()

    @Volatile
    private var currentUsage = DailyUsage()

    private var lastActionTime = 0L
    private val debounceMillis = 700L

    private val lastProvocation = mutableMapOf<String, Long>()
    private var lastSoftAlertTime = 0L
    private var lastFinalAlertTime = 0L
    private val lastWarnedMinutes = mutableMapOf<String, Int>()
    private val lastBlockElapsed = mutableMapOf<String, Long>()

    private var lastCreditPkg: String? = null
    private var lastCreditElapsed = 0L
    private var lastDeepScanElapsed = 0L
    private var lastDeepShorts = false
    private var lastEventElapsed = 0L

    private val pendingTotal = mutableMapOf<String, Long>()
    private val pendingShorts = mutableMapOf<String, Long>()
    private var lastFlushElapsed = 0L

    private var channelsCreated = false

    private var overlay: ChallengeOverlay? = null
    private var lastChallengeId: String? = null
    private var lastOverlayElapsed = 0L

    private fun overlay(): ChallengeOverlay = (overlay ?: ChallengeOverlay(this).also { overlay = it }).also {
        it.dark = when (settings.themeMode) {
            "DARK" -> true
            "LIGHT" -> false
            else -> (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        runCatching { (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(OFF_NOTICE_ID) }
        serviceScope.launch {
            dataStore.appSettingsFlow.collect {
                settings = it
                applyListeningScope()
            }
        }
        serviceScope.launch {
            dataStore.dailyUsageFlow.collect { currentUsage = it }
        }
        // Re-check the scope when the screen turns on, and every so often while it is on, so a
        // schedule that just started begins listening without waiting for a settings change.
        ContextCompat.registerReceiver(
            this,
            screenOnReceiver,
            IntentFilter(Intent.ACTION_SCREEN_ON),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        serviceScope.launch {
            while (isActive) {
                delay(SCOPE_REFRESH_MILLIS)
                applyListeningScope()
            }
        }
    }

    private val screenOnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = applyListeningScope()
    }

    @Volatile
    private var appliedScope: Set<String> = emptySet()

    /**
     * Android only delivers events from the packages listed in the service info, so listing just
     * the apps with an active rule (and nothing outside their schedule) keeps every other app
     * from waking the service. Only the live info is mutated, never replaced by a blank one.
     */
    private fun applyListeningScope() {
        val wanted = ListeningScope.packages(settings)
        if (wanted == appliedScope) return
        serviceScope.launch(Dispatchers.Main) {
            try {
                val info = serviceInfo ?: return@launch
                info.packageNames = wanted.toTypedArray()
                serviceInfo = info
                appliedScope = wanted
            } catch (e: Exception) {
                Log.e(TAG, "Error updating the listening scope", e)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null) return
            val pkg = event.packageName?.toString() ?: return

            // Bursts of content changes carry no new information; window changes always do.
            val nowElapsed = SystemClock.elapsedRealtime()
            if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                nowElapsed - lastEventElapsed < MIN_EVENT_GAP_MILLIS
            ) {
                return
            }
            lastEventElapsed = nowElapsed

            maybeFlagAdultSite(event, pkg)
            val appName = TrackedPackages.displayName(pkg)
            if (appName == null) {
                flushUsage()
                lastCreditPkg = null
                lastCreditElapsed = 0L
                return
            }

            val app = BlockPolicy.appConfig(settings, appName)
            val ruleOn = BlockPolicy.wholeRuleOn(app) || BlockPolicy.shortsRuleOn(app)
            if (!app.blocked || !ruleOn || !BlockPolicy.isScheduleActive(settings)) {
                // Nothing to enforce or count here right now.
                flushUsage()
                lastCreditPkg = null
                lastCreditElapsed = 0L
                return
            }

            val today = java.time.LocalDate.now().toString()
            if (currentUsage.date != today) {
                currentUsage = DailyUsage(date = today)
                serviceScope.launch { dataStore.ensureTodayUsage() }
            }
            handleTrackedApp(pkg, appName, event)
        } catch (t: Throwable) {
            Log.e(TAG, "Unhandled error in onAccessibilityEvent", t)
        }
    }

    /** Accessibility was switched off while protection is set up: say so, loudly, with a way back. */
    override fun onUnbind(intent: Intent?): Boolean {
        overlay?.dismiss()
        if (settings.setupDone && BlockPolicy.hasAnyProtection(settings)) {
            runCatching {
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                ensureChannels(manager)
                val open = android.app.PendingIntent.getActivity(
                    this, 0,
                    Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
                manager.notify(
                    OFF_NOTICE_ID,
                    NotificationCompat.Builder(this, ALERT_CHANNEL)
                        .setSmallIcon(R.drawable.ic_stat_blockfy)
                        .setContentTitle(getString(R.string.service_off_title))
                        .setContentText(getString(R.string.service_off_body))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(open)
                        .setOngoing(true)
                        .build()
                )
            }
        }
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {
        overlay?.dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(screenOnReceiver) }
        overlay?.dismiss()
        flushUsage()
        serviceScope.cancel()
    }

    private fun handleTrackedApp(pkg: String, appName: String, event: AccessibilityEvent) {
        val app = BlockPolicy.appConfig(settings, appName)
        val canHaveShorts = !BlockPolicy.isWholeAppOnly(appName)
        // Reading the screen is the expensive part, so only do it when something needs it: the
        // Reels/Shorts rule, the mindfulness reminder or the open Diagnostics card.
        val needShorts = canHaveShorts && (BlockPolicy.shortsRuleOn(app) || settings.provocationModeEnabled)
        val root = if (needShorts) rootFromEvent(event, pkg) else null
        try {
            process(pkg, appName, app, canHaveShorts && needShorts, root)
        } finally {
            root?.recycle()
        }
    }

    private fun process(pkg: String, appName: String, app: App, needShorts: Boolean, root: AccessibilityNodeInfo?) {
        val nowElapsed = SystemClock.elapsedRealtime()

        // Run the cheap id lookup on every event and the slower fallback scan at most every
        // DEEP_SCAN_INTERVAL_MILLIS. Enforcement only trusts what this very event saw, so
        // leaving Reels never triggers one stray "back".
        val canScan = needShorts && root != null
        val deep = canScan && nowElapsed - lastDeepScanElapsed >= DEEP_SCAN_INTERVAL_MILLIS
        if (deep) lastDeepScanElapsed = nowElapsed
        val shortsVisible = canScan && ShortsDetector.isVisible(appName, root!!, deepScan = deep)
        if (deep) lastDeepShorts = shortsVisible
        // Time counting may lean on the last deep scan so a fallback-detected Reels session
        // is not under-counted between scans.
        val shortsForCredit = shortsVisible || (canScan && !deep && lastDeepShorts)

        creditUsage(pkg, appName, shortsForCredit)

        val usage = usageWithPending()
        val verdict = BlockPolicy.evaluate(
            packageName = pkg,
            settings = settings,
            usage = usage,
            shortsVisible = shortsVisible
        )

        if (verdict.shouldBlock) {
            enforce(verdict, app, root)
            return
        }

        if (shortsVisible || appName == "TikTok") maybeProvoke(appName)

        if (app.blocked && BlockPolicy.isScheduleActive(settings)) {
            if (BlockPolicy.wholeRuleOn(app) && app.appTotalDailyLimitMinutes > 0) {
                checkAndNotifyRemainingTime(
                    key = "$appName:whole",
                    label = appName,
                    usedSeconds = BlockPolicy.totalSeconds(usage, appName),
                    limitMinutes = app.appTotalDailyLimitMinutes
                )
            }
            if (BlockPolicy.shortsRuleOn(app) && shortsVisible && app.dailyLimitMinutes > 0) {
                checkAndNotifyRemainingTime(
                    key = "$appName:shorts",
                    label = shortsLabel(appName),
                    usedSeconds = BlockPolicy.featureSeconds(usage, appName),
                    limitMinutes = app.dailyLimitMinutes
                )
            }
        }
    }

    private fun shortsLabel(appName: String) = if (appName == "YouTube") "YouTube Shorts" else "$appName Reels"

    /** Alert once per episode, but keep pushing the user out until the app actually leaves. */
    private fun enforce(verdict: BlockVerdict, app: App, root: AccessibilityNodeInfo?) {
        val appName = verdict.appName
        val now = SystemClock.elapsedRealtime()
        val newEpisode = now - (lastBlockElapsed[appName] ?: 0L) > EPISODE_GAP_MILLIS
        lastBlockElapsed[appName] = now

        if (newEpisode) {
            val (title, message) = when (verdict.reason) {
                BlockReason.SCHEDULE -> getString(R.string.alert_blocked_title) to if (verdict.wholeApp) {
                    getString(R.string.toast_app_window_blocked, appName)
                } else {
                    getString(R.string.toast_shorts_window_blocked, appName)
                }
                BlockReason.DAILY_LIMIT -> getString(R.string.alert_limit_title) to
                    getString(R.string.toast_limit_reached, app.dailyLimitMinutes, shortsLabel(appName))
                else -> getString(R.string.alert_app_total_limit_title) to
                    getString(R.string.toast_app_total_limit_reached, app.appTotalDailyLimitMinutes, appName)
            }
            alertOrChallenge(title, message)
            serviceScope.launch { dataStore.recordBlockedDistraction(300L) }
        }

        if (verdict.wholeApp) {
            exitTheDoom(null) { performGlobalAction(GLOBAL_ACTION_HOME) }
        } else {
            exitShorts(appName, root)
        }
    }

    /**
     * A block fired for a new episode. With fun challenges on, the user gets a short, light trick
     * question and a useful suggestion; otherwise (or if the window cannot be shown) the classic
     * notification and toast.
     */
    private fun alertOrChallenge(title: String, message: String) {
        if (settings.funChallengesEnabled && showChallenge(message)) return
        notifyAlert(title, withWhy(message), isFinal = true)
    }

    /** True when the overlay is up (or was just up), so the caller skips the classic alert. */
    private fun showChallenge(contextLine: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastOverlayElapsed < MIN_OVERLAY_GAP_MILLIS) return overlay?.isShowing == true
        lastOverlayElapsed = now
        val challenge = ChallengeBank.next(lastChallengeId)
        lastChallengeId = challenge.id
        return overlay().showChallenge(challenge, contextLine, randomSuggestion())
    }

    private fun showCheckIn(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastOverlayElapsed < MIN_OVERLAY_GAP_MILLIS) return overlay?.isShowing == true
        lastOverlayElapsed = now
        val titles = resources.getStringArray(R.array.checkin_titles)
        val bodies = resources.getStringArray(R.array.checkin_bodies)
        val i = kotlin.random.Random.nextInt(minOf(titles.size, bodies.size))
        return overlay().showCheckIn(titles[i], bodies[i], randomSuggestion(), getString(R.string.checkin_help)) {
            performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        }
    }

    private fun randomSuggestion(): String = resources.getStringArray(R.array.challenge_suggestions).random()

    /** Adds the user's own reason for changing under a block message, when they wrote one. */
    private fun withWhy(message: String): String {
        val why = settings.myWhy
        return if (why.isBlank()) message else "$message\n\u201C$why\u201D"
    }

    private fun maybeProvoke(appName: String) {
        if (!settings.provocationModeEnabled) return
        val now = System.currentTimeMillis()
        val last = lastProvocation[appName] ?: 0L
        if (now - last <= PROVOCATION_INTERVAL_MILLIS) return
        lastProvocation[appName] = now
        notifyAlert(
            getString(R.string.app_name),
            MindfulnessProvocationEngine.getRandomReelsQuote(this),
            isFinal = false
        )
    }

    private fun usageWithPending(): DailyUsage {
        var usage = currentUsage
        pendingTotal.forEach { (app, seconds) -> usage = usage.plusTotal(app, seconds) }
        pendingShorts.forEach { (app, seconds) -> usage = usage.plusShorts(app, seconds) }
        return usage
    }

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

    /**
     * One DataStore write per interval instead of one per accessibility event. Until the
     * write lands, [usageWithPending] keeps limit checks accurate.
     */
    private fun flushUsage() {
        lastFlushElapsed = SystemClock.elapsedRealtime()
        if (pendingTotal.isEmpty() && pendingShorts.isEmpty()) return
        val total = pendingTotal.toMap()
        val shorts = pendingShorts.toMap()
        // Fold into currentUsage right away so nothing is counted twice or lost while the
        // write is in flight.
        var merged = currentUsage
        total.forEach { (app, seconds) -> merged = merged.plusTotal(app, seconds) }
        shorts.forEach { (app, seconds) -> merged = merged.plusShorts(app, seconds) }
        currentUsage = merged
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
            else -> exitTheDoom(null)
        }
    }

    private fun checkAndNotifyRemainingTime(key: String, label: String, usedSeconds: Long, limitMinutes: Int) {
        val remainingSeconds = limitMinutes * 60L - usedSeconds
        if (remainingSeconds <= 0L) return
        val remainingMinutes = ((remainingSeconds + 59L) / 60L).toInt()
        val lastWarned = lastWarnedMinutes[key] ?: -1
        if (remainingMinutes == lastWarned) return
        val shouldWarn = remainingMinutes <= 5 || remainingMinutes % 5 == 0
        if (!shouldWarn) return
        lastWarnedMinutes[key] = remainingMinutes
        val minText = if (remainingMinutes == 1) {
            getString(R.string.toast_remaining_one_minute, label)
        } else {
            getString(R.string.toast_remaining_minutes, remainingMinutes, label)
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
        if (isFinal) {
            if (now - lastFinalAlertTime < MIN_FINAL_ALERT_GAP_MILLIS) return
            lastFinalAlertTime = now
        } else {
            if (now - lastSoftAlertTime < MIN_SOFT_ALERT_GAP_MILLIS) return
            lastSoftAlertTime = now
        }
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

    private fun ensureChannels(manager: NotificationManager) {
        if (channelsCreated) return
        manager.createNotificationChannel(
            NotificationChannel(ALERT_CHANNEL, getString(R.string.focus_alerts_channel_name), NotificationManager.IMPORTANCE_HIGH)
        )
        manager.createNotificationChannel(
            NotificationChannel(REMINDER_CHANNEL, getString(R.string.focus_reminders_channel_name), NotificationManager.IMPORTANCE_LOW)
        )
        channelsCreated = true
    }

    private fun showNotification(title: String, message: String, isFinal: Boolean) {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            ensureChannels(manager)
            val builder = NotificationCompat.Builder(this, if (isFinal) ALERT_CHANNEL else REMINDER_CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_blockfy)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(if (isFinal) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
                .setTimeoutAfter(NOTIFICATION_TIMEOUT_MILLIS)
                .setAutoCancel(true)
            manager.notify(if (isFinal) 1001 else 1002, builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification", e)
        }
    }

    private fun showToast(message: String) {
        serviceScope.launch(Dispatchers.Main) {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Walk up from the event source. Never inspect whichever window is in
     * front - that would include a banking app after the user switches apps.
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
        if (now - lastAdultScanElapsed < ADULT_SCAN_GAP_MILLIS) return
        lastAdultScanElapsed = now

        val root = rootFromEvent(event, pkg) ?: return
        try {
            val addressBarText = readAddressBar(root, pkg)
            if (addressBarText != null &&
                (AdultContentDetector.isAdultContent(addressBarText) || AdultContentDetector.isAdultSearch(addressBarText))
            ) {
                leaveAdultScreen()
            }
        } finally {
            root.recycle()
        }
    }

    /**
     * Known address-bar view ids first (exact and cheap), then the generic "first visible,
     * non-password EditText" search for browsers whose ids we do not know.
     */
    private fun readAddressBar(root: AccessibilityNodeInfo, pkg: String): String? {
        val ids = (ADDRESS_BAR_IDS[pkg] ?: emptyList()) + "$pkg:id/url_bar"
        for (id in ids) {
            val nodes = try {
                root.findAccessibilityNodeInfosByViewId(id)
            } catch (_: Exception) {
                null
            } ?: continue
            var text: String? = null
            for (node in nodes) {
                if (text == null && node.isVisibleToUser && !node.isPassword) {
                    text = node.text?.toString()?.takeIf { it.isNotBlank() }
                }
                node.recycle()
            }
            if (text != null) return text
        }
        return findAddressBarText(root, remaining = intArrayOf(MAX_ADDRESS_BAR_SCAN_NODES))
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
        performGlobalAction(GLOBAL_ACTION_HOME)
        if (settings.funChallengesEnabled && showCheckIn()) return
        val message = AdultContentDetector.getRandomWarning(this)
        notifyAlert(getString(R.string.adult_blocker_title), withWhy(message), isFinal = true)
    }

    companion object {
        private const val TAG = "BlockfyService"
        private const val ALERT_CHANNEL = "blockfy_alerts_channel"
        private const val REMINDER_CHANNEL = "blockfy_reminders_channel"
        private const val MAX_CREDIT_SECONDS = 5L
        private const val FLUSH_INTERVAL_MILLIS = 10_000L
        private const val MAX_ADDRESS_BAR_SCAN_NODES = 40
        private const val DEEP_SCAN_INTERVAL_MILLIS = 700L
        private const val MIN_EVENT_GAP_MILLIS = 150L
        private const val SCOPE_REFRESH_MILLIS = 30_000L
        private const val EPISODE_GAP_MILLIS = 8_000L
        private const val PROVOCATION_INTERVAL_MILLIS = 180_000L
        private const val MIN_SOFT_ALERT_GAP_MILLIS = 4_000L
        private const val MIN_OVERLAY_GAP_MILLIS = 6_000L
        private const val ADULT_SCAN_GAP_MILLIS = 400L
        private const val OFF_NOTICE_ID = 1003
        private const val MIN_FINAL_ALERT_GAP_MILLIS = 2_000L
        private const val NOTIFICATION_TIMEOUT_MILLIS = 15_000L

        private val ADDRESS_BAR_IDS = mapOf(
            "com.android.chrome" to listOf("com.android.chrome:id/url_bar"),
            "com.sec.android.app.sbrowser" to listOf("com.sec.android.app.sbrowser:id/location_bar_edit_text"),
            "org.mozilla.firefox" to listOf(
                "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
                "org.mozilla.firefox:id/url_bar_title"
            ),
            "com.opera.browser" to listOf("com.opera.browser:id/url_field"),
            "com.duckduckgo.mobile.android" to listOf("com.duckduckgo.mobile.android:id/omnibarTextInput")
        )
    }
}
