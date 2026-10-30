package com.buenotty.blockfy.feature_progress

import com.buenotty.blockfy.datastore.AppSettings
import com.buenotty.blockfy.datastore.DailyUsage
import com.buenotty.blockfy.feature_monitor.BlockPolicy

/** How a finished day counts. */
enum class DayOutcome {
    /** Protected, and every limit was kept. */
    CLEAN,

    /** Went over a limit by a little. A shield can save the day. */
    SLIP,

    /** Cheated: a big excess, or a rule was loosened. The run starts over. */
    BREAK,

    /** No protection was on, so nothing was earned and nothing broken. */
    NONE
}

/**
 * Reads the day's numbers against the user's own limits. With "Usage access" the totals are the
 * phone's own, so going around the blocker still shows up: two hours on a one-hour limit is a break.
 */
object DayJudge {
    /** Seconds over a limit that are just the block taking a moment to land. */
    private const val GRACE_SECONDS = 90L
    private const val MIN_TOLERANCE_SECONDS = 10 * 60L
    private const val TOLERANCE_SHARE = 0.2
    private const val BLOCKED_TOLERANCE_SECONDS = 180L

    private data class Rule(val usedSeconds: Long, val limitMinutes: Int, val grantedSeconds: Long)

    /** Minutes the user negotiated today are allowed, not cheated. */
    private fun granted(usage: DailyUsage, appName: String) = usage.bonusSeconds(appName, viaBrowser = true)

    private fun rules(usage: DailyUsage, settings: AppSettings): List<Rule> = buildList {
        for (app in settings.apps) {
            if (!app.blocked) continue
            if (BlockPolicy.wholeRuleOn(app)) add(Rule(BlockPolicy.totalSeconds(usage, app.name), app.appTotalDailyLimitMinutes, granted(usage, app.name)))
            if (BlockPolicy.shortsRuleOn(app)) add(Rule(BlockPolicy.featureSeconds(usage, app.name), app.dailyLimitMinutes, granted(usage, app.name)))
        }
    }

    fun judge(usage: DailyUsage, settings: AppSettings, protectedDay: Boolean): DayOutcome {
        if (!protectedDay) return DayOutcome.NONE
        if (usage.loosened) return DayOutcome.BREAK
        var worst = DayOutcome.CLEAN
        for (rule in rules(usage, settings)) {
            val allowed = rule.limitMinutes * 60L + rule.grantedSeconds
            val excess = rule.usedSeconds - allowed
            val tolerance = if (rule.limitMinutes <= 0) {
                BLOCKED_TOLERANCE_SECONDS
            } else {
                maxOf(MIN_TOLERANCE_SECONDS, (allowed * TOLERANCE_SHARE).toLong())
            }
            when {
                excess <= GRACE_SECONDS -> Unit
                excess <= tolerance -> if (worst == DayOutcome.CLEAN) worst = DayOutcome.SLIP
                else -> return DayOutcome.BREAK
            }
        }
        return worst
    }

    /** The most of any limit used today, from 0 (nothing) to 1 or more (at or over). Rewards finishing well under. */
    fun usageShare(usage: DailyUsage, settings: AppSettings): Double {
        // Asking for an exception means the day was not comfortably under the limit.
        if (usage.exceptions > 0) return 1.0
        return rules(usage, settings).maxOfOrNull { rule ->
            when {
                rule.limitMinutes > 0 -> rule.usedSeconds / (rule.limitMinutes * 60.0)
                rule.usedSeconds > 0L -> 1.0
                else -> 0.0
            }
        } ?: 0.0
    }
}
