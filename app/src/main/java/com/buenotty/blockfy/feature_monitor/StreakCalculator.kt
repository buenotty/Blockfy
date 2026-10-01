package com.buenotty.blockfy.feature_monitor

import com.buenotty.blockfy.datastore.AppSettings
import com.buenotty.blockfy.datastore.DailyUsage
import com.buenotty.blockfy.datastore.DayRecord
import java.time.LocalDate

enum class DayState { CLEAN, BROKEN, EMPTY }

data class StreakInfo(
    /** Consecutive clean days ending today, counting today while it is still clean. */
    val current: Int,
    val best: Int,
    val todayClean: Boolean,
    /** Days of streak thrown away today by loosening or switching something off; 0 if today is intact. */
    val lostDays: Int,
    /** Oldest first, ending with today. */
    val lastDays: List<DayState>
)

/**
 * A clean day is one where protection was on and the user never switched anything off or
 * loosened it (higher limit, shorter schedule, shield off). Days with no record count as not clean.
 */
object StreakCalculator {

    fun compute(
        history: List<DayRecord>,
        today: DailyUsage,
        settings: AppSettings,
        todayDate: LocalDate = LocalDate.now(),
        window: Int = 7
    ): StreakInfo {
        val byDate = history.associateBy { it.date }
        val todayClean = !today.loosened && (today.hadProtection || BlockPolicy.hasAnyProtection(settings))

        var run = 0
        var cursor = todayDate.minusDays(1)
        while (byDate[cursor.toString()]?.clean == true) {
            run++
            cursor = cursor.minusDays(1)
        }
        // Breaking today throws the whole streak away, not just today.
        val current = if (today.loosened) 0 else run + if (todayClean) 1 else 0
        val lostDays = if (today.loosened) run else 0

        var best = 0
        var streak = 0
        var previous: LocalDate? = null
        for (record in history.sortedBy { it.date }) {
            val date = LocalDate.parse(record.date)
            streak = if (record.clean) {
                if (previous != null && previous.plusDays(1) == date && streak > 0) streak + 1 else 1
            } else {
                0
            }
            previous = date
            if (streak > best) best = streak
        }
        best = maxOf(best, current, run)

        val lastDays = (window - 1 downTo 0).map { back ->
            val date = todayDate.minusDays(back.toLong())
            if (back == 0) {
                when {
                    today.loosened -> DayState.BROKEN
                    todayClean -> DayState.CLEAN
                    else -> DayState.EMPTY
                }
            } else {
                when (byDate[date.toString()]?.clean) {
                    true -> DayState.CLEAN
                    false -> DayState.BROKEN
                    null -> DayState.EMPTY
                }
            }
        }
        return StreakInfo(current = current, best = best, todayClean = todayClean, lostDays = lostDays, lastDays = lastDays)
    }
}
