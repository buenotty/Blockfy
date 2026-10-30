package com.buenotty.blockfy.feature_progress

import com.buenotty.blockfy.datastore.AppSettings
import com.buenotty.blockfy.datastore.DailyUsage
import com.buenotty.blockfy.feature_monitor.BlockPolicy
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
            return JourneyUi(
                journey = journey,
                rank = Ranks.of(journey.xp),
                todayOutcome = outcome,
                liveRun = if (alive) journey.runDays + 1 else 0,
                goal = Goals.next(if (alive) journey.runDays + partOfToday else 0.0),
                shieldsLeft = JourneyEngine.shieldsLeft(journey, now.toLocalDate())
            )
        }
    }
}
