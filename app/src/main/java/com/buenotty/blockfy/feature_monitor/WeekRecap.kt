package com.buenotty.blockfy.feature_monitor

import com.buenotty.blockfy.datastore.DailyUsage
import com.buenotty.blockfy.datastore.DayRecord
import java.time.LocalDate

/** The last seven days (today included) in three numbers, worked out from the history already on the phone. */
data class WeekRecap(val cleanDays: Int, val blocks: Int, val savedMinutes: Int) {

    val isEmpty: Boolean get() = cleanDays == 0 && blocks == 0 && savedMinutes == 0

    companion object {
        fun of(
            history: List<DayRecord>,
            today: DailyUsage,
            todayClean: Boolean,
            todayDate: LocalDate = LocalDate.now()
        ): WeekRecap {
            val first = todayDate.minusDays(6).toString()
            val last = todayDate.toString()
            // ISO dates sort as text, so a string comparison is a date comparison.
            val week = history.filter { it.date >= first && it.date < last }
            return WeekRecap(
                cleanDays = week.count { it.clean } + if (todayClean) 1 else 0,
                blocks = week.sumOf { it.blocks } + today.blockedAttemptsToday,
                savedMinutes = ((week.sumOf { it.savedSeconds } + today.savedSeconds) / 60L).toInt()
            )
        }
    }
}
