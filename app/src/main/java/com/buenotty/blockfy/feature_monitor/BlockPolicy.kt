package com.buenotty.blockfy.feature_monitor

import com.buenotty.blockfy.datastore.AppSettings
import com.buenotty.blockfy.datastore.DailyUsage
import com.buenotty.blockfy.feature_preferences.repository.models.App
import java.util.Calendar
import java.util.TimeZone

enum class BlockReason {
    NONE,
    SCHEDULE,
    DAILY_LIMIT,
    TOTAL_LIMIT
}

data class BlockVerdict(
    val reason: BlockReason,
    val appName: String,
    val packageName: String
) {
    val shouldBlock: Boolean get() = reason != BlockReason.NONE
}

/**
 * One rule for every app:
 *
 *  1. The master switch (`blocked`) must be on.
 *  2. The moment must be inside the schedule (weekday + hours).
 *  3. The scope decides what is blocked: Reels/Shorts only, or the whole app.
 *  4. With no limit the scope is blocked for the whole schedule. With a limit it is
 *     allowed until the day's usage reaches it.
 */
object BlockPolicy {

    fun currentMinuteOfDay(nowMillis: Long = System.currentTimeMillis(), timeZone: TimeZone = TimeZone.getDefault()): Int {
        val calendar = Calendar.getInstance(timeZone)
        calendar.timeInMillis = nowMillis
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    fun isActiveWeekday(mask: Int, nowMillis: Long = System.currentTimeMillis(), timeZone: TimeZone = TimeZone.getDefault()): Boolean {
        val calendar = Calendar.getInstance(timeZone)
        calendar.timeInMillis = nowMillis
        val bit = 1 shl (calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY)
        return mask and bit != 0
    }

    fun isWithinInterval(start: Int, end: Int, minute: Int): Boolean {
        return if (start <= end) {
            minute in start..end
        } else {
            minute >= start || minute <= end
        }
    }

    /**
     * Whether the schedule covers [minute] of weekday [dayIndex] (0 = Sunday). A window that
     * crosses midnight (22:00-06:00) belongs to the weekday it starts on, so the 02:00 part of a
     * Friday night window still counts as Friday.
     */
    fun scheduleCovers(app: App, dayIndex: Int, minute: Int): Boolean {
        if (!isWithinInterval(app.blockedStart, app.blockedEnd, minute)) return false
        val crossesMidnight = app.blockedStart > app.blockedEnd
        val windowDay = if (crossesMidnight && minute <= app.blockedEnd) (dayIndex + 6) % 7 else dayIndex
        return app.blockedWeekdays and (1 shl windowDay) != 0
    }

    fun isScheduleActive(
        app: App,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Boolean {
        val calendar = Calendar.getInstance(timeZone)
        calendar.timeInMillis = nowMillis
        val minute = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        return scheduleCovers(app, calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY, minute)
    }

    /**
     * True when [new] lets the user use the app in some situation where [old] would have blocked
     * it: the block is switched off, the scope narrows, the limit grows, or any minute of the
     * week stops being covered. Strict mode forbids these and the editor asks for a pause first.
     */
    fun isLoosening(old: App, new: App): Boolean {
        if (!old.blocked) return false
        if (!new.blocked) return true

        val oldWhole = isWholeScope(old)
        val newWhole = isWholeScope(new)
        if (oldWhole && !newWhole) return true
        if (oldWhole == newWhole) {
            val oldLimit = limitMinutes(old)
            val newLimit = limitMinutes(new)
            if (oldLimit == 0 && newLimit > 0) return true
            if (oldLimit > 0 && newLimit > oldLimit) return true
        }

        for (day in 0 until 7) {
            for (minute in 0 until 1440) {
                if (scheduleCovers(old, day, minute) && !scheduleCovers(new, day, minute)) return true
            }
        }
        return false
    }

    fun isWholeAppOnly(appName: String): Boolean = appName == "TikTok" || appName == "X"

    fun isWholeScope(app: App): Boolean = isWholeAppOnly(app.name) || app.wholeApp

    fun limitMinutes(app: App): Int =
        if (isWholeScope(app)) app.appTotalDailyLimitMinutes else app.dailyLimitMinutes

    fun usedSeconds(app: App, usage: DailyUsage): Long =
        if (isWholeScope(app)) totalSeconds(usage, app.name) else featureSeconds(usage, app.name)

    /**
     * @param shortsVisible whether a Reels/Shorts screen is on display right now. It only
     * matters when the scope is Reels/Shorts; whole-app rules ignore it.
     */
    fun evaluate(
        packageName: String,
        settings: AppSettings,
        usage: DailyUsage,
        nowMillis: Long = System.currentTimeMillis(),
        shortsVisible: Boolean = true,
        timeZone: TimeZone = TimeZone.getDefault()
    ): BlockVerdict {
        val appName = TrackedPackages.displayName(packageName)
            ?: return BlockVerdict(BlockReason.NONE, "", packageName)
        val none = BlockVerdict(BlockReason.NONE, appName, packageName)

        val app = appConfig(settings, appName)
        if (!app.blocked) return none
        if (!isScheduleActive(app, nowMillis, timeZone)) return none

        val whole = isWholeScope(app)
        if (!whole && !shortsVisible) return none

        val limit = limitMinutes(app)
        if (limit <= 0) return BlockVerdict(BlockReason.SCHEDULE, appName, packageName)
        if (usedSeconds(app, usage) < limit * 60L) return none
        val reason = if (whole) BlockReason.TOTAL_LIMIT else BlockReason.DAILY_LIMIT
        return BlockVerdict(reason, appName, packageName)
    }

    fun appConfig(settings: AppSettings, appName: String): App {
        return when (appName) {
            "Instagram" -> settings.instagram
            "YouTube" -> settings.youtube
            "TikTok" -> settings.tiktok
            "Facebook" -> settings.facebook
            "X" -> settings.x
            else -> App(name = appName, blocked = false, blockedStart = 0, blockedEnd = 1439, blockedTimer = 0, features = emptyList())
        }
    }

    fun featureSeconds(usage: DailyUsage, appName: String): Long {
        return when (appName) {
            "Instagram" -> usage.instagramSeconds
            "YouTube" -> usage.youtubeSeconds
            "TikTok" -> usage.tiktokSeconds
            "Facebook" -> usage.facebookSeconds
            "X" -> usage.xTotalSeconds
            else -> 0L
        }
    }

    fun totalSeconds(usage: DailyUsage, appName: String): Long {
        return when (appName) {
            "Instagram" -> usage.instagramTotalSeconds
            "YouTube" -> usage.youtubeTotalSeconds
            "TikTok" -> usage.tiktokTotalSeconds
            "Facebook" -> usage.facebookTotalSeconds
            "X" -> usage.xTotalSeconds
            else -> 0L
        }
    }
}
