package com.buenotty.blockfy

import com.buenotty.blockfy.datastore.AppSettings
import com.buenotty.blockfy.datastore.DailyUsage
import com.buenotty.blockfy.datastore.DayRecord
import com.buenotty.blockfy.feature_accessibility.AdultContentDetector
import com.buenotty.blockfy.feature_monitor.BlockPolicy
import com.buenotty.blockfy.feature_monitor.DayState
import com.buenotty.blockfy.feature_monitor.ListeningScope
import com.buenotty.blockfy.feature_monitor.StreakCalculator
import com.buenotty.blockfy.feature_monitor.TrackedPackages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Calendar
import java.util.TimeZone

class StreakAndScopeTest {

    private val today = LocalDate.of(2026, 1, 15)
    private val protectedSettings = AppSettings().let { it.copy(instagram = it.instagram.copy(blocked = true)) }

    private fun day(back: Int, clean: Boolean) = DayRecord(today.minusDays(back.toLong()).toString(), clean = clean)

    private fun streak(history: List<DayRecord>, usage: DailyUsage = DailyUsage(date = today.toString()), settings: AppSettings = protectedSettings) =
        StreakCalculator.compute(history, usage, settings, today)

    // ---- clean-day streak ------------------------------------------------------------------

    @Test
    fun consecutiveCleanDaysAreCountedAndTodayAddsOne() {
        val info = streak(listOf(day(3, true), day(2, true), day(1, true)))
        assertEquals(4, info.current)
        assertTrue(info.todayClean)
    }

    @Test
    fun aBrokenDayEndsTheStreak() {
        val info = streak(listOf(day(4, true), day(3, true), day(2, false), day(1, true)))
        assertEquals(2, info.current) // yesterday + today
        assertEquals(2, info.best)
    }

    @Test
    fun aMissingDayEndsTheStreak() {
        val info = streak(listOf(day(5, true), day(4, true), day(3, true), day(1, true)))
        assertEquals(2, info.current)
        assertEquals(3, info.best)
    }

    @Test
    fun looseningTodayThrowsTheStreakAway() {
        val usage = DailyUsage(date = today.toString(), loosened = true)
        val info = streak(listOf(day(2, true), day(1, true)), usage)
        // The two clean days are thrown away, and the screen says how many were lost.
        assertEquals(0, info.current)
        assertEquals(2, info.lostDays)
        assertEquals(2, info.best)
        assertFalse(info.todayClean)
        assertEquals(DayState.BROKEN, info.lastDays.last())
    }

    @Test
    fun withoutAnyProtectionTodayIsNotCounted() {
        val info = streak(emptyList(), settings = AppSettings())
        assertEquals(0, info.current)
        assertFalse(info.todayClean)
        assertEquals(DayState.EMPTY, info.lastDays.last())
    }

    @Test
    fun lastSevenDaysAreReportedOldestFirst() {
        val info = streak(listOf(day(3, false), day(2, true), day(1, true)))
        assertEquals(
            listOf(DayState.EMPTY, DayState.EMPTY, DayState.EMPTY, DayState.BROKEN, DayState.CLEAN, DayState.CLEAN, DayState.CLEAN),
            info.lastDays
        )
    }

    @Test
    fun anySettingsCircumventionCountsAsLoosening() {
        val on = protectedSettings
        assertTrue(BlockPolicy.isSettingsLoosening(on, on.copy(instagram = on.instagram.copy(blocked = false))))
        assertTrue(BlockPolicy.isSettingsLoosening(on, on.copy(scheduleStart = 9 * 60, scheduleEnd = 18 * 60)))
        assertTrue(
            BlockPolicy.isSettingsLoosening(
                on.copy(adultContentBlockerEnabled = true),
                on.copy(adultContentBlockerEnabled = false)
            )
        )
        assertFalse(BlockPolicy.isSettingsLoosening(on, on.copy(adultContentBlockerEnabled = true)))
        assertFalse(BlockPolicy.isSettingsLoosening(AppSettings(), on))
    }

    @Test
    fun protectionMeansABlockRuleOrTheShieldIsOn() {
        assertFalse(BlockPolicy.hasAnyProtection(AppSettings()))
        assertTrue(BlockPolicy.hasAnyProtection(protectedSettings))
        assertTrue(BlockPolicy.hasAnyProtection(AppSettings(adultContentBlockerEnabled = true)))
        val noRule = AppSettings().let { it.copy(instagram = it.instagram.copy(blocked = true, shortsRuleOn = false)) }
        assertFalse(BlockPolicy.hasAnyProtection(noRule))
    }

    // ---- listening scope (battery) -----------------------------------------------------------

    private val utc = TimeZone.getTimeZone("UTC")
    private fun at(hour: Int): Long {
        val c = Calendar.getInstance(utc)
        c.clear()
        c.set(2026, Calendar.JANUARY, 5, hour, 0)
        return c.timeInMillis
    }

    @Test
    fun nothingActiveMeansListeningToNothing() {
        assertEquals(setOf(ListeningScope.NOTHING), ListeningScope.packages(AppSettings(), at(12), utc))
    }

    @Test
    fun onlyAppsWithAnActiveRuleAreListenedTo() {
        val s = AppSettings().let {
            it.copy(
                instagram = it.instagram.copy(blocked = true),
                youtube = it.youtube.copy(blocked = false),
                tiktok = it.tiktok.copy(blocked = true)
            )
        }
        assertEquals(setOf(TrackedPackages.INSTAGRAM, TrackedPackages.TIKTOK), ListeningScope.packages(s, at(12), utc))
    }

    @Test
    fun outsideTheScheduleNoAppIsListenedTo() {
        val s = protectedSettings.copy(scheduleStart = 22 * 60, scheduleEnd = 23 * 60)
        assertEquals(setOf(ListeningScope.NOTHING), ListeningScope.packages(s, at(12), utc))
        assertTrue(TrackedPackages.INSTAGRAM in ListeningScope.packages(s, at(22), utc))
    }

    @Test
    fun browsersAreListenedToOnlyWhileTheShieldIsOn() {
        val on = AppSettings(adultContentBlockerEnabled = true)
        assertEquals(AdultContentDetector.BROWSER_PACKAGES, ListeningScope.packages(on, at(12), utc))
        assertFalse(ListeningScope.packages(AppSettings(), at(12), utc).any { it in AdultContentDetector.BROWSER_PACKAGES })
    }
}
