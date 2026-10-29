package com.buenotty.blockfy.feature_monitor

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.time.LocalDate
import java.time.ZoneId

/** One foreground/background event of one screen of an app, as Android reports it. */
data class ForegroundEvent(val packageName: String, val className: String, val type: Int, val timeMillis: Long)

/** The arithmetic apart from Android, so it can be tested. */
object UsageStatsMath {
    // Same values as UsageEvents.Event.ACTIVITY_RESUMED / ACTIVITY_PAUSED (and the older
    // MOVE_TO_FOREGROUND / MOVE_TO_BACKGROUND), so it works on every supported Android.
    const val RESUMED = 1
    const val PAUSED = 2

    /**
     * Seconds the app was in front: the time with at least one of its screens resumed. Moving from
     * one screen to another overlaps for a moment, which must not count twice.
     */
    fun foregroundSeconds(events: List<ForegroundEvent>, packageName: String, now: Long): Long {
        val open = mutableSetOf<String>()
        var since = 0L
        var millis = 0L
        for (e in events.filter { it.packageName == packageName }.sortedBy { it.timeMillis }) {
            when (e.type) {
                RESUMED -> {
                    if (open.isEmpty()) since = e.timeMillis
                    open += e.className
                }
                PAUSED -> if (open.remove(e.className) && open.isEmpty()) millis += e.timeMillis - since
            }
        }
        if (open.isNotEmpty()) millis += now - since
        return millis / 1000L
    }
}

/**
 * The phone's own count of time in each app: the same number Digital Wellbeing shows. It needs
 * the "Usage access" special permission, which the user grants in Settings. Blockfy reads only the
 * time per app, on the phone, and never sends it anywhere.
 */
object UsageStatsReader {

    fun hasAccess(context: Context): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Seconds [packageName] was in front since local midnight, or null without access. */
    fun secondsToday(context: Context, packageName: String, now: Long = System.currentTimeMillis()): Long? {
        if (!hasAccess(context)) return null
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val raw = manager.queryEvents(start, now)
        val events = mutableListOf<ForegroundEvent>()
        val event = UsageEvents.Event()
        while (raw.hasNextEvent()) {
            raw.getNextEvent(event)
            if (event.packageName == packageName) {
                events += ForegroundEvent(event.packageName, event.className.orEmpty(), event.eventType, event.timeStamp)
            }
        }
        return UsageStatsMath.foregroundSeconds(events, packageName, now)
    }
}
