package com.buenotty.blockfy

import com.buenotty.blockfy.datastore.AppSettings
import com.buenotty.blockfy.datastore.DailyUsage
import com.buenotty.blockfy.feature_progress.DayJudge
import com.buenotty.blockfy.feature_progress.DayOutcome
import com.buenotty.blockfy.feature_progress.Goals
import com.buenotty.blockfy.feature_progress.Journey
import com.buenotty.blockfy.feature_progress.JourneyEngine
import com.buenotty.blockfy.feature_progress.Medals
import com.buenotty.blockfy.feature_progress.Ranks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class JourneyTest {

    // ---- ranks ----

    @Test
    fun `zero xp is a common person and the first clean day makes a recruit`() {
        assertEquals(0, Ranks.of(0).level)
        val first = Ranks.of(100)
        assertEquals(1, first.level)
        assertEquals(0, first.rankIndex)
        assertEquals(0, first.sub)
    }

    @Test
    fun `levels climb through the ranks and then prestige starts over from recruit`() {
        val general3 = Ranks.of(Ranks.xpForLevel(30))
        assertEquals(30, general3.level)
        assertEquals(9, general3.rankIndex)
        assertEquals(2, general3.sub)
        assertEquals(0, general3.prestige)

        val next = Ranks.of(Ranks.xpForLevel(31))
        assertEquals(31, next.level)
        assertEquals(1, next.prestige)
        assertEquals(0, next.rankIndex)
        assertEquals(0, next.sub)
    }

    @Test
    fun `thresholds only grow and prestige costs more than the cycle before`() {
        for (l in 1..200) assertTrue(Ranks.xpForLevel(l) > Ranks.xpForLevel(l - 1))
        val firstCycle = Ranks.xpForLevel(30)
        val secondCycle = Ranks.xpForLevel(60) - Ranks.xpForLevel(30)
        assertTrue(secondCycle > firstCycle)
    }

    @Test
    fun `progress inside a level is a fraction between zero and one`() {
        val half = Ranks.of((Ranks.xpForLevel(4) + Ranks.xpForLevel(5)) / 2)
        assertEquals(4, half.level)
        assertEquals(0.5, half.fraction, 0.02)
    }

    // ---- goals ----

    @Test
    fun `goals pick the next target and the percent towards it`() {
        val a = Goals.next(0.3)
        assertEquals(1, a.days)
        assertEquals(0.3, a.fraction, 0.001)
        assertEquals(7, Goals.next(3.5).days)
        assertEquals(30, Goals.next(20.0).days)
        assertEquals(730, Goals.next(400.0).days)
    }

    // ---- day judge ----

    private fun settings(limitMin: Int = 60): AppSettings {
        val base = AppSettings()
        return base.copy(instagram = base.instagram.copy(blocked = true, wholeApp = true, appTotalDailyLimitMinutes = limitMin, shortsRuleOn = false))
    }

    private fun usage(instaSeconds: Long, loosened: Boolean = false) =
        DailyUsage(date = "2026-10-29", instagramTotalSeconds = instaSeconds, loosened = loosened)

    @Test
    fun `using less than the limit is clean, a few seconds over is still clean`() {
        assertEquals(DayOutcome.CLEAN, DayJudge.judge(usage(30 * 60), settings(), protectedDay = true))
        assertEquals(DayOutcome.CLEAN, DayJudge.judge(usage(60 * 60 + 40), settings(), protectedDay = true))
    }

    @Test
    fun `a moderate excess is a slip and a big one is a break`() {
        assertEquals(DayOutcome.SLIP, DayJudge.judge(usage(60 * 60 + 8 * 60), settings(), protectedDay = true))
        // 2 hours against a 1 hour limit, as in a real cheat.
        assertEquals(DayOutcome.BREAK, DayJudge.judge(usage(2 * 3600), settings(), protectedDay = true))
    }

    @Test
    fun `loosening a rule breaks the day and an unprotected day is nothing`() {
        assertEquals(DayOutcome.BREAK, DayJudge.judge(usage(0, loosened = true), settings(), protectedDay = true))
        assertEquals(DayOutcome.NONE, DayJudge.judge(usage(0), settings(), protectedDay = false))
    }

    @Test
    fun `a fully blocked app tolerates a few minutes only`() {
        val s = settings(limitMin = 0)
        assertEquals(DayOutcome.CLEAN, DayJudge.judge(usage(30), s, protectedDay = true))
        assertEquals(DayOutcome.BREAK, DayJudge.judge(usage(20 * 60), s, protectedDay = true))
    }

    // ---- engine ----

    private val d1 = LocalDate.of(2026, 10, 1)

    @Test
    fun `a clean day gives xp and extends the run, more xp the longer the run`() {
        val one = JourneyEngine.creditDay(Journey(), d1, DayOutcome.CLEAN, usageShare = 0.0, blocks = 0, savedSeconds = 0)
        assertEquals(1, one.runDays)
        assertEquals(150L, one.xp)
        val later = JourneyEngine.creditDay(Journey(runDays = 10, lastCredited = "2026-09-30"), d1, DayOutcome.CLEAN, 0.0, 0, 0)
        assertTrue(later.xp > one.xp)
    }

    @Test
    fun `a break resets only the run, never xp or rank`() {
        val before = Journey(xp = 5_000, runDays = 12, bestRun = 12, lastCredited = "2026-09-30")
        val after = JourneyEngine.creditDay(before, d1, DayOutcome.BREAK, 1.0, 0, 0)
        assertEquals(0, after.runDays)
        assertEquals(12, after.bestRun)
        assertEquals(5_000L, after.xp)
        assertEquals(1, after.breakDays)
        assertTrue(after.lastWasBreak)
    }

    @Test
    fun `a clean day after a break is a comeback`() {
        val broke = Journey(lastCredited = "2026-09-30", lastWasBreak = true)
        val back = JourneyEngine.creditDay(broke, d1, DayOutcome.CLEAN, 0.0, 0, 0)
        assertEquals(1, back.comebacks)
        assertFalse(back.lastWasBreak)
    }

    @Test
    fun `a slip is saved by a shield twice a month and breaks the third time`() {
        var j = Journey()
        var day = d1
        val outcomes = mutableListOf<DayOutcome>()
        repeat(3) {
            val outcome = JourneyEngine.resolve(j, day, DayOutcome.SLIP)
            outcomes += outcome
            j = JourneyEngine.creditDay(j, day, outcome, 1.0, 0, 0)
            day = day.plusDays(1)
        }
        assertEquals(listOf(DayOutcome.SLIP, DayOutcome.SLIP, DayOutcome.BREAK), outcomes)
        // A new month brings the shields back.
        assertEquals(DayOutcome.SLIP, JourneyEngine.resolve(j, LocalDate.of(2026, 11, 2), DayOutcome.SLIP))
    }

    @Test
    fun `crediting the same day twice changes nothing and a gap ends the run`() {
        val once = JourneyEngine.creditDay(Journey(), d1, DayOutcome.CLEAN, 0.0, 0, 0)
        assertEquals(once, JourneyEngine.creditDay(once, d1, DayOutcome.CLEAN, 0.0, 0, 0))
        val gap = JourneyEngine.creditDay(once, d1.plusDays(3), DayOutcome.CLEAN, 0.0, 0, 0)
        assertEquals(1, gap.runDays)
    }

    // ---- medals ----

    @Test
    fun `run medals unlock at their length and are never taken back`() {
        val seven = Journey(runDays = 7, bestRun = 7)
        val ids = Medals.unlock(seven, liveRun = 7, today = d1).medals.keys
        assertTrue("run_1" in ids && "run_3" in ids && "run_7" in ids)
        assertFalse("run_14" in ids)
        val broke = Medals.unlock(seven, 7, d1).copy(runDays = 0)
        assertTrue("run_7" in Medals.unlock(broke, 0, d1.plusDays(1)).medals)
    }

    @Test
    fun `behaviour medals use lifetime counters`() {
        val j = Journey(blocks = 120, comebacks = 1, savedSeconds = 40_000, slipDays = 1)
        val ids = Medals.unlock(j, 0, d1).medals.keys
        assertTrue(setOf("resisted_10", "resisted_100", "comeback", "saved_10h", "shield_used").all { it in ids })
        assertFalse("resisted_500" in ids)
    }

    @Test
    fun `every medal has a unique id`() {
        assertEquals(Medals.all.size, Medals.all.map { it.id }.toSet().size)
    }
}
