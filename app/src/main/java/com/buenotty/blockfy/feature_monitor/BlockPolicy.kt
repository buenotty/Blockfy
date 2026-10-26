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
    val packageName: String,
    /** True when the whole app is blocked, false when only Reels/Shorts are. */
    val wholeApp: Boolean = false
) {
    val shouldBlock: Boolean get() = reason != BlockReason.NONE
}

/**
 * The rules, in one place:
 *
 *  1. The app's master switch must be on.
 *  2. The moment must be inside the global schedule (hours and weekdays, shared by every app).
 *  3. The whole-app rule, if on, either blocks the app (limit 0) or allows N minutes a day of
 *     total use.
 *  4. The Reels/Shorts rule, if on and a short-video screen is showing, either blocks it
 *     (limit 0) or allows N minutes a day of short-video use.
 *
 * The two rules are independent, so "one hour of Instagram, but only 15 minutes of Reels" is
 * simply a whole-app limit of 60 plus a Reels limit of 15.
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
     * Whether a schedule covers [minute] of weekday [dayIndex] (0 = Sunday). A window that
     * crosses midnight (22:00-06:00) belongs to the weekday it starts on, so the 02:00 part of a
     * Friday night window still counts as Friday.
     */
    fun scheduleCovers(start: Int, end: Int, weekdays: Int, dayIndex: Int, minute: Int): Boolean {
        if (!isWithinInterval(start, end, minute)) return false
        val crossesMidnight = start > end
        val windowDay = if (crossesMidnight && minute <= end) (dayIndex + 6) % 7 else dayIndex
        return weekdays and (1 shl windowDay) != 0
    }

    fun isScheduleActive(
        settings: AppSettings,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Boolean {
        val calendar = Calendar.getInstance(timeZone)
        calendar.timeInMillis = nowMillis
        val minute = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val day = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        return scheduleCovers(settings.scheduleStart, settings.scheduleEnd, settings.scheduleWeekdays, day, minute)
    }

    /** True when [new] stops covering any minute of the week that [old] covered. */
    fun isScheduleLoosening(old: AppSettings, new: AppSettings): Boolean {
        for (day in 0 until 7) {
            for (minute in 0 until 1440) {
                val before = scheduleCovers(old.scheduleStart, old.scheduleEnd, old.scheduleWeekdays, day, minute)
                val after = scheduleCovers(new.scheduleStart, new.scheduleEnd, new.scheduleWeekdays, day, minute)
                if (before && !after) return true
            }
        }
        return false
    }

    fun isWholeAppOnly(appName: String): Boolean = appName == "TikTok" || appName == "X"

    /** TikTok and X can only be limited as a whole, so for them this rule is always on. */
    fun wholeRuleOn(app: App): Boolean = isWholeAppOnly(app.name) || app.wholeApp

    fun shortsRuleOn(app: App): Boolean = !isWholeAppOnly(app.name) && app.shortsRuleOn

    private fun limitLoosened(oldLimit: Int, newLimit: Int): Boolean = when {
        oldLimit <= 0 -> newLimit > 0
        else -> newLimit > oldLimit
    }

    /**
     * True when [new] lets the user use the app in some situation where [old] would have blocked
     * it: the master switch goes off, a rule is dropped, or a limit grows. Hours and weekdays are
     * global and checked with [isScheduleLoosening]. Strict mode forbids all of these, and the
     * editor asks for a pause before accepting them.
     */
    fun isLoosening(old: App, new: App): Boolean {
        if (!old.blocked) return false
        if (!new.blocked) return true
        if (wholeRuleOn(old)) {
            if (!wholeRuleOn(new)) return true
            if (limitLoosened(old.appTotalDailyLimitMinutes, new.appTotalDailyLimitMinutes)) return true
        }
        if (shortsRuleOn(old)) {
            if (!shortsRuleOn(new)) return true
            if (limitLoosened(old.dailyLimitMinutes, new.dailyLimitMinutes)) return true
        }
        return false
    }

    /** True when at least one app rule or the adult shield is on. */
    fun hasAnyProtection(settings: AppSettings): Boolean {
        val apps = listOf(settings.instagram, settings.youtube, settings.tiktok, settings.facebook, settings.x)
        return settings.adultContentBlockerEnabled ||
            apps.any { it.blocked && (wholeRuleOn(it) || shortsRuleOn(it)) }
    }

    /** True when [new] circumvents [old] in any way: a looser app rule, a narrower schedule or the shield off. */
    fun isSettingsLoosening(old: AppSettings, new: AppSettings): Boolean {
        val pairs = listOf(
            old.instagram to new.instagram, old.youtube to new.youtube, old.tiktok to new.tiktok,
            old.facebook to new.facebook, old.x to new.x
        )
        return pairs.any { (before, after) -> isLoosening(before, after) } ||
            isScheduleLoosening(old, new) ||
            (old.adultContentBlockerEnabled && !new.adultContentBlockerEnabled)
    }

    /**
     * @param shortsVisible whether a Reels/Shorts screen is on display right now. It only
     * matters for the Reels/Shorts rule; the whole-app rule ignores it.
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
        if (!isScheduleActive(settings, nowMillis, timeZone)) return none

        if (wholeRuleOn(app)) {
            val limit = app.appTotalDailyLimitMinutes
            if (limit <= 0) return BlockVerdict(BlockReason.SCHEDULE, appName, packageName, wholeApp = true)
            if (totalSeconds(usage, appName) >= limit * 60L) {
                return BlockVerdict(BlockReason.TOTAL_LIMIT, appName, packageName, wholeApp = true)
            }
        }
        if (shortsRuleOn(app) && shortsVisible) {
            val limit = app.dailyLimitMinutes
            if (limit <= 0) return BlockVerdict(BlockReason.SCHEDULE, appName, packageName)
            if (featureSeconds(usage, appName) >= limit * 60L) {
                return BlockVerdict(BlockReason.DAILY_LIMIT, appName, packageName)
            }
        }
        return none
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

    /** Seconds spent on Reels/Shorts today. */
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

    /** Seconds spent anywhere in the app today. */
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
