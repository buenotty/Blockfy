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

object BlockPolicy {

    fun currentMinuteOfDay(nowMillis: Long = System.currentTimeMillis(), timeZone: TimeZone = TimeZone.getDefault()): Int {
        val offset = timeZone.getOffset(nowMillis)
        return (((nowMillis + offset) / 60000) % 1440).toInt()
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

    fun evaluate(
        packageName: String,
        settings: AppSettings,
        usage: DailyUsage,
        minuteOfDay: Int
    ): BlockVerdict {
        val appName = TrackedPackages.displayName(packageName)
            ?: return BlockVerdict(BlockReason.NONE, "", packageName)

        val appConfig = appConfig(settings, appName)
        val usedTotalSeconds = totalSeconds(usage, appName)

        if (!isActiveWeekday(appConfig.blockedWeekdays)) {
            return BlockVerdict(BlockReason.NONE, appName, packageName)
        }

        if (appConfig.appTotalDailyLimitMinutes > 0 &&
            usedTotalSeconds >= appConfig.appTotalDailyLimitMinutes * 60L
        ) {
            return BlockVerdict(BlockReason.TOTAL_LIMIT, appName, packageName)
        }

        return BlockVerdict(BlockReason.NONE, appName, packageName)
    }

    fun evaluateShorts(
        appName: String,
        settings: AppSettings,
        usage: DailyUsage,
        minuteOfDay: Int
    ): BlockVerdict {
        val packageName = TrackedPackages.ALL.entries.firstOrNull { it.value == appName }?.key ?: return BlockVerdict(BlockReason.NONE, appName, "")
        val appConfig = appConfig(settings, appName)
        if (!isActiveWeekday(appConfig.blockedWeekdays)) {
            return BlockVerdict(BlockReason.NONE, appName, packageName)
        }
        if (!appConfig.blocked || !isWithinInterval(appConfig.blockedStart, appConfig.blockedEnd, minuteOfDay)) {
            return BlockVerdict(BlockReason.NONE, appName, packageName)
        }
        if (appConfig.dailyLimitMinutes <= 0) {
            return BlockVerdict(BlockReason.SCHEDULE, appName, packageName)
        }
        if (featureSeconds(usage, appName) >= appConfig.dailyLimitMinutes * 60L) {
            return BlockVerdict(BlockReason.DAILY_LIMIT, appName, packageName)
        }
        return BlockVerdict(BlockReason.NONE, appName, packageName)
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
