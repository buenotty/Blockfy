package com.buenotty.blockfy.datastore

import android.content.Context
import androidx.datastore.dataStore
import android.util.Log
import com.buenotty.blockfy.feature_monitor.BlockPolicy
import com.buenotty.blockfy.feature_progress.DayJudge
import com.buenotty.blockfy.feature_progress.DayOutcome
import com.buenotty.blockfy.feature_progress.Journey
import com.buenotty.blockfy.feature_progress.JourneyEngine
import com.buenotty.blockfy.feature_progress.JourneySerializer
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.appSettingsStore by dataStore("app_settings.json", AppSettingsSerializer)
val Context.dailyUsageStore by dataStore("daily_usage.json", DailyUsageSerializer)
val Context.historyStore by dataStore("history.json", HistorySerializer)
val Context.journeyStore by dataStore("journey.json", JourneySerializer)

private const val MAX_HISTORY_DAYS = 90
private const val MAX_GAP_DAYS = 30

/** Blocked attempts credited per day, so reopening a blocked app again and again cannot farm medals. */
private const val MAX_BLOCKS_PER_DAY = 30

class DataStoreManager(private val context: Context) {

    val appSettingsFlow = context.appSettingsStore.data
    val historyFlow = context.historyStore.data
    val journeyFlow = context.journeyStore.data
    val dailyUsageFlow = context.dailyUsageStore.data.map { current ->
        val today = getTodayDateString()
        if (current.date == today) current else DailyUsage(date = today)
    }

    /**
     * Read-modify-write inside DataStore, so two quick changes can never overwrite each other.
     * Also feeds the clean-day streak: loosening anything marks today as not clean, and having any
     * protection on marks today as protected.
     */
    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        // If the day just rolled over, close yesterday with the settings it ran under, not with these.
        updateToday { it }
        var before: AppSettings? = null
        var after: AppSettings? = null
        context.appSettingsStore.updateData { current ->
            before = current
            // Sites added to the adult list and the apps the user admitted stay for good, whatever the transform does.
            transform(current).let {
                it.copy(
                    customAdultSites = (current.customAdultSites + it.customAdultSites).distinct(),
                    customAdultKeywords = (current.customAdultKeywords + it.customAdultKeywords).distinct(),
                    seenAdultIn = (current.seenAdultIn + it.seenAdultIn).distinct(),
                    checkInDone = current.checkInDone || it.checkInDone,
                    // A running focus session cannot be shortened, only extended.
                    focusUntilEpoch = maxOf(current.focusUntilEpoch, it.focusUntilEpoch)
                )
            }.also { after = it }
        }
        val old = before ?: return
        val new = after ?: return
        if (BlockPolicy.breaksCleanDay(old, new)) markLoosened()
        if (BlockPolicy.hasAnyProtection(new)) updateToday { it.copy(hadProtection = true) }
    }

    /** Called when the user switches off a protection from outside the app, such as Accessibility. */
    suspend fun markLoosened() = updateToday { it.copy(loosened = true) }

    private fun getTodayDateString(): String {
        return java.time.LocalDate.now().toString()
    }

    suspend fun ensureTodayUsage(): DailyUsage {
        var result = DailyUsage()
        updateToday { it.also { usage -> result = usage } }
        return result
    }

    /** Applies [transform] to today's usage, first archiving yesterday if the day just rolled over. */
    private suspend fun updateToday(transform: (DailyUsage) -> DailyUsage) {
        val today = getTodayDateString()
        var finished: DailyUsage? = null
        context.dailyUsageStore.updateData { current ->
            if (current.date == today) {
                transform(current)
            } else {
                if (current.date.isNotBlank()) finished = current
                transform(DailyUsage(date = today))
            }
        }
        finished?.let { archive(it) }
    }

    private suspend fun archive(day: DailyUsage) {
        // A failure here must never take the service down or hold the next day back.
        try {
            archiveDay(day)
        } catch (e: Exception) {
            Log.e("DataStoreManager", "Could not archive ${day.date}", e)
        }
    }

    private suspend fun archiveDay(day: DailyUsage) {
        // Replay old history first, so a phone that already has some is never left starting from zero.
        bootstrapJourney()
        val settings = context.appSettingsStore.data.first()
        val protectedDay = day.hadProtection || BlockPolicy.hasAnyProtection(settings)
        val date = LocalDate.parse(day.date)
        // A clock set back must not book today or the future, which would lock out the real days after it.
        if (!date.isBefore(LocalDate.now())) return
        val raw = DayJudge.judge(day, settings, protectedDay)
        var outcome = DayOutcome.NONE
        val skipped = mutableListOf<DayRecord>()
        // The same step judges the day, spends a shield if one is needed, and books the XP, so
        // the history and the journey can never disagree.
        context.journeyStore.updateData { start ->
            skipped.clear()
            var journey = start
            // Days nobody wrote anything on (the user kept away from the apps and never opened
            // Blockfy) were clean if protection is on; leaving them out would cut the run of the
            // people who behave best.
            if (journey.lastCredited.isNotEmpty()) {
                var gap = LocalDate.parse(journey.lastCredited).plusDays(1)
                var counted = 0
                while (gap.isBefore(date) && counted < MAX_GAP_DAYS) {
                    val quiet = if (BlockPolicy.hasAnyProtection(settings)) DayOutcome.CLEAN else DayOutcome.NONE
                    journey = JourneyEngine.creditDay(journey, gap, quiet, 0.0, 0, 0L)
                    skipped += DayRecord(date = gap.toString(), clean = quiet == DayOutcome.CLEAN)
                    gap = gap.plusDays(1)
                    counted++
                }
            }
            outcome = JourneyEngine.resolve(journey, date, raw)
            JourneyEngine.creditDay(
                journey, date, outcome, DayJudge.usageShare(day, settings),
                day.blockedAttemptsToday, day.savedSeconds
            )
        }
        val record = DayRecord(
            date = day.date,
            blocks = day.blockedAttemptsToday,
            savedSeconds = day.savedSeconds,
            clean = outcome == DayOutcome.CLEAN || outcome == DayOutcome.SLIP,
            slip = outcome == DayOutcome.SLIP
        )
        context.historyStore.updateData { history ->
            val keep = history.days.filterNot { d -> d.date == record.date || skipped.any { it.date == d.date } }
            History((keep + skipped + record).sortedBy { it.date }.takeLast(MAX_HISTORY_DAYS))
        }
    }

    suspend fun updateJourney(transform: (Journey) -> Journey) {
        context.journeyStore.updateData(transform)
    }

    /** First run of the journey on a phone that already has history: replay it once so nobody starts from zero. */
    suspend fun bootstrapJourney() {
        val history = context.historyStore.data.first().days
        context.journeyStore.updateData { journey ->
            if (journey.lastCredited.isNotEmpty() || history.isEmpty()) {
                journey
            } else {
                history.sortedBy { it.date }.fold(journey.copy(enlisted = true)) { acc, record ->
                    val outcome = when {
                        record.slip -> DayOutcome.SLIP
                        record.clean -> DayOutcome.CLEAN
                        else -> DayOutcome.NONE
                    }
                    JourneyEngine.creditDay(acc, LocalDate.parse(record.date), outcome, 1.0, record.blocks, record.savedSeconds)
                }
            }
        }
    }

    suspend fun addUsage(appName: String, seconds: Long) = updateToday { it.plusShorts(appName, seconds) }

    suspend fun addTotalAppUsage(appName: String, seconds: Long) = updateToday { it.plusTotal(appName, seconds) }

    suspend fun grantException(appName: String, seconds: Long, browser: Boolean) =
        updateToday { it.withException(appName, seconds, browser) }

    suspend fun addOpens(appName: String, count: Int) = updateToday { it.plusOpens(appName, count) }

    suspend fun startBreak(appName: String, untilMillis: Long) = updateToday { it.withBreak(appName, untilMillis) }

    suspend fun recordBlockedDistraction(estimatedSecondsSaved: Long = 300L) = updateToday {
        if (it.blockedAttemptsToday >= MAX_BLOCKS_PER_DAY) it else it.copy(
            blockedAttemptsToday = it.blockedAttemptsToday + 1,
            savedSeconds = it.savedSeconds + estimatedSecondsSaved
        )
    }

    /** Wiping counters that had time in them is cheating the day; wiping an empty one is harmless. */
    suspend fun resetUsage(appName: String) = updateToday {
        val hadUse = BlockPolicy.totalSeconds(it, appName) > 0L || BlockPolicy.featureSeconds(it, appName) > 0L
        it.resetApp(appName).let { reset -> if (hadUse) reset.copy(loosened = true) else reset }
    }
}
