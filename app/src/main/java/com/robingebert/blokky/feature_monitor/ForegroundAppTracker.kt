package com.robingebert.blokky.feature_monitor

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

object ForegroundAppTracker {

    fun currentPackage(context: Context, now: Long = System.currentTimeMillis()): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val fromEvents = fromEvents(usm, now)
        if (!fromEvents.isNullOrBlank()) return fromEvents
        return fromStats(usm, now)
    }

    @Suppress("DEPRECATION")
    private fun fromEvents(usm: UsageStatsManager, now: Long): String? {
        val events = usm.queryEvents(now - 8_000L, now) ?: return null
        val event = UsageEvents.Event()
        var lastPkg: String? = null
        var lastTime = 0L
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val isResume = event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            if (isResume && event.timeStamp >= lastTime) {
                lastTime = event.timeStamp
                lastPkg = event.packageName
            }
        }
        return lastPkg
    }

    private fun fromStats(usm: UsageStatsManager, now: Long): String? {
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 60_000L, now) ?: return null
        return stats.maxByOrNull { it.lastTimeUsed }?.packageName
    }
}
