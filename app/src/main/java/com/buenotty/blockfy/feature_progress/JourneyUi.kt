package com.buenotty.blockfy.feature_progress

import com.buenotty.blockfy.datastore.AppSettings
import com.buenotty.blockfy.datastore.DailyUsage
import com.buenotty.blockfy.feature_monitor.BlockPolicy
import java.time.LocalDate
import java.time.LocalDateTime

/** Everything the Journey screen and the home card show, worked out from the saved journey plus today's numbers. */
data class JourneyUi(
    val journey: Journey = Journey(),
    val rank: RankState = Ranks.of(0),
    /** How today stands: still clean, forgiven by a shield, already broken, or unprotected. */
    val todayOutcome: DayOutcome = DayOutcome.NONE,
    /** Clean days in the run counting today while it is still alive. */
    val liveRun: Int = 0,
    val goal: Goal = Goals.next(0.0),
    val shieldsLeft: Int = JourneyEngine.SHIELDS_PER_MONTH
) {
    val todayAlive: Boolean get() = todayOutcome == DayOutcome.CLEAN || todayOutcome == DayOutcome.SLIP

    companion object {
        fun of(
            journey: Journey,
            usage: DailyUsage,
            settings: AppSettings,
            now: LocalDateTime = LocalDateTime.now()
        ): JourneyUi {
            val protectedNow = usage.hadProtection || BlockPolicy.hasAnyProtection(settings)
            val raw = DayJudge.judge(usage, settings, protectedNow)
            val outcome = JourneyEngine.resolve(journey, now.toLocalDate(), raw)
            val alive = outcome == DayOutcome.CLEAN || outcome == DayOutcome.SLIP
            val partOfToday = now.toLocalTime().toSecondOfDay() / 86_400.0
            // Days not credited up to yesterday mean the run already ended, even if the stored count has not been reset yet.
            val stale = journey.lastCredited.isNotEmpty() && LocalDate.parse(journey.lastCredited).isBefore(now.toLocalDate().minusDays(1))
            val base = if (stale) 0 else journey.runDays
            return JourneyUi(
                journey = journey,
                rank = Ranks.of(journey.xp),
                todayOutcome = outcome,
                liveRun = if (alive) base + 1 else 0,
                goal = Goals.next(if (alive) base + partOfToday else 0.0),
                shieldsLeft = JourneyEngine.shieldsLeft(journey, now.toLocalDate())
            )
        }
    }
}
