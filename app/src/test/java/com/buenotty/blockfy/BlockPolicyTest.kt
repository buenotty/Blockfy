package com.buenotty.blockfy

import com.buenotty.blockfy.datastore.AppSettings
import com.buenotty.blockfy.datastore.DailyUsage
import com.buenotty.blockfy.feature_monitor.BlockPolicy
import com.buenotty.blockfy.feature_monitor.BlockReason
import com.buenotty.blockfy.feature_monitor.TrackedPackages
import com.buenotty.blockfy.feature_preferences.repository.models.App
import com.buenotty.blockfy.fixtures.BankPackages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class BlockPolicyTest {

    private val utc = TimeZone.getTimeZone("UTC")

    /** 2026-01-05 is a Monday. Day 0 = Monday, 6 = Sunday. */
    private fun at(dayOffsetFromMonday: Int, hour: Int, minute: Int = 0): Long {
        val calendar = Calendar.getInstance(utc)
        calendar.clear()
        calendar.set(2026, Calendar.JANUARY, 5 + dayOffsetFromMonday, hour, minute)
        return calendar.timeInMillis
    }

    private fun settings(transform: (AppSettings) -> AppSettings) = transform(AppSettings())

    private fun evaluate(
        pkg: String,
        s: AppSettings,
        usage: DailyUsage = DailyUsage(),
        now: Long = at(0, 12),
        shortsVisible: Boolean = true
    ) = BlockPolicy.evaluate(pkg, s, usage, now, shortsVisible, utc)

    @Test
    fun masterSwitchOffMeansNothingIsEverBlocked() {
        val s = settings {
            it.copy(
                instagram = it.instagram.copy(blocked = false, dailyLimitMinutes = 1),
                tiktok = it.tiktok.copy(blocked = false, appTotalDailyLimitMinutes = 1)
            )
        }
        val heavyUsage = DailyUsage(instagramSeconds = 9_999, tiktokTotalSeconds = 9_999)
        assertFalse(evaluate(TrackedPackages.INSTAGRAM, s, heavyUsage).shouldBlock)
        assertFalse(evaluate(TrackedPackages.TIKTOK, s, heavyUsage).shouldBlock)
    }

    @Test
    fun shortsScopeBlocksOnlyWhileShortsAreOnScreen() {
        val s = settings { it.copy(instagram = it.instagram.copy(blocked = true)) }
        assertEquals(BlockReason.SCHEDULE, evaluate(TrackedPackages.INSTAGRAM, s, shortsVisible = true).reason)
        assertEquals(BlockReason.NONE, evaluate(TrackedPackages.INSTAGRAM, s, shortsVisible = false).reason)
    }

    @Test
    fun wholeAppScopeBlocksEvenWhenNoShortsAreVisible() {
        val s = settings { it.copy(instagram = it.instagram.copy(blocked = true, wholeApp = true)) }
        assertEquals(BlockReason.SCHEDULE, evaluate(TrackedPackages.INSTAGRAM, s, shortsVisible = false).reason)
    }

    @Test
    fun tiktokAndXAreAlwaysWholeApp() {
        val s = settings {
            it.copy(
                tiktok = it.tiktok.copy(blocked = true),
                x = it.x.copy(blocked = true)
            )
        }
        assertEquals(BlockReason.SCHEDULE, evaluate(TrackedPackages.TIKTOK, s, shortsVisible = false).reason)
        assertEquals(BlockReason.SCHEDULE, evaluate(TrackedPackages.X, s, shortsVisible = false).reason)
    }

    @Test
    fun shortsLimitUsesShortsSecondsAndBlocksAtTheLimit() {
        val s = settings { it.copy(youtube = it.youtube.copy(blocked = true, dailyLimitMinutes = 10)) }
        val under = evaluate(TrackedPackages.YOUTUBE, s, DailyUsage(youtubeSeconds = 9 * 60 + 59))
        val over = evaluate(TrackedPackages.YOUTUBE, s, DailyUsage(youtubeSeconds = 10 * 60))
        assertFalse(under.shouldBlock)
        assertEquals(BlockReason.DAILY_LIMIT, over.reason)
    }

    @Test
    fun wholeAppLimitUsesTotalSecondsNotShortsSeconds() {
        val s = settings {
            it.copy(instagram = it.instagram.copy(blocked = true, wholeApp = true, appTotalDailyLimitMinutes = 20))
        }
        val shortsOnly = evaluate(TrackedPackages.INSTAGRAM, s, DailyUsage(instagramSeconds = 3_000, instagramTotalSeconds = 60))
        val total = evaluate(TrackedPackages.INSTAGRAM, s, DailyUsage(instagramTotalSeconds = 20 * 60))
        assertFalse(shortsOnly.shouldBlock)
        assertEquals(BlockReason.TOTAL_LIMIT, total.reason)
    }

    @Test
    fun outsideTheScheduleNothingIsBlockedEvenOverTheLimit() {
        val s = settings {
            it.copy(
                tiktok = it.tiktok.copy(
                    blocked = true,
                    blockedStart = 22 * 60,
                    blockedEnd = 23 * 60,
                    appTotalDailyLimitMinutes = 1
                )
            )
        }
        val usage = DailyUsage(tiktokTotalSeconds = 5_000)
        assertFalse(evaluate(TrackedPackages.TIKTOK, s, usage, now = at(0, 12)).shouldBlock)
        assertTrue(evaluate(TrackedPackages.TIKTOK, s, usage, now = at(0, 22, 30)).shouldBlock)
    }

    @Test
    fun weekdayMaskSelectsTheRightDays() {
        val mondayOnly = AppSettings().instagram.copy(blocked = true, blockedWeekdays = 1 shl 1)
        assertTrue(BlockPolicy.isScheduleActive(mondayOnly, at(0, 12), utc))
        assertFalse(BlockPolicy.isScheduleActive(mondayOnly, at(1, 12), utc))
        assertTrue(BlockPolicy.isActiveWeekday(127, at(3, 12), utc))
    }

    @Test
    fun overnightWindowBelongsToTheDayItStarts() {
        val fridayNight = App(
            name = "Instagram", blocked = true, blockedStart = 22 * 60, blockedEnd = 6 * 60,
            blockedTimer = 0, features = emptyList(), blockedWeekdays = 1 shl 5
        )
        // Friday 23:00 and Saturday 02:00 are both part of Friday's window.
        assertTrue(BlockPolicy.isScheduleActive(fridayNight, at(4, 23), utc))
        assertTrue(BlockPolicy.isScheduleActive(fridayNight, at(5, 2), utc))
        // Saturday 23:00 starts Saturday's window, which is not selected.
        assertFalse(BlockPolicy.isScheduleActive(fridayNight, at(5, 23), utc))
        // Friday 02:00 is the tail of Thursday's window.
        assertFalse(BlockPolicy.isScheduleActive(fridayNight, at(4, 2), utc))
        assertFalse(BlockPolicy.isScheduleActive(fridayNight, at(4, 12), utc))
    }

    @Test
    fun intervalHelperWrapsMidnight() {
        assertTrue(BlockPolicy.isWithinInterval(22 * 60, 6 * 60, 23 * 60))
        assertTrue(BlockPolicy.isWithinInterval(22 * 60, 6 * 60, 30))
        assertFalse(BlockPolicy.isWithinInterval(22 * 60, 6 * 60, 12 * 60))
    }

    @Test
    fun bankPackagesNeverProduceAVerdict() {
        val s = settings { it.copy(instagram = it.instagram.copy(blocked = true, wholeApp = true)) }
        BankPackages.ALL.forEach { bank ->
            assertEquals(BlockReason.NONE, evaluate(bank, s).reason)
        }
    }

    @Test
    fun usageHelpersNeverCountXTwiceAndResetCleanly() {
        val usage = DailyUsage().plusTotal("X", 30).plusShorts("X", 30)
        assertEquals(30L, usage.xTotalSeconds)
        val instagram = DailyUsage().plusTotal("Instagram", 10).plusShorts("Instagram", 4)
        assertEquals(10L, instagram.instagramTotalSeconds)
        assertEquals(4L, instagram.instagramSeconds)
        assertEquals(0L, instagram.resetApp("Instagram").instagramTotalSeconds)
    }

    @Test
    fun strictModeLockExpiresAtMidnight() {
        val s = AppSettings(strictModeEnabled = true, strictModeType = "MIDNIGHT", strictModeLockedUntilEpoch = 1_000L)
        assertTrue(s.isStrictLocked(nowMillis = 999L))
        assertFalse(s.isStrictLocked(nowMillis = 1_000L))
        assertFalse(AppSettings().isStrictLocked())
    }

    private fun app(
        start: Int = 0, end: Int = 1439, days: Int = 127, limit: Int = 0,
        whole: Boolean = false, blocked: Boolean = true
    ) = App(
        name = "Instagram", blocked = blocked, blockedStart = start, blockedEnd = end,
        blockedTimer = 0, features = emptyList(), dailyLimitMinutes = if (whole) 0 else limit,
        appTotalDailyLimitMinutes = if (whole) limit else 0, blockedWeekdays = days, wholeApp = whole
    )

    @Test
    fun turningOffOrRaisingTheLimitIsLoosening() {
        assertTrue(BlockPolicy.isLoosening(app(), app(blocked = false)))
        assertTrue(BlockPolicy.isLoosening(app(limit = 30), app(limit = 45)))
        assertTrue(BlockPolicy.isLoosening(app(limit = 0), app(limit = 30)))
        assertTrue(BlockPolicy.isLoosening(app(whole = true), app(whole = false)))
    }

    @Test
    fun makingABlockStricterIsNeverLoosening() {
        assertFalse(BlockPolicy.isLoosening(app(limit = 30), app(limit = 15)))
        assertFalse(BlockPolicy.isLoosening(app(limit = 30), app(limit = 0)))
        assertFalse(BlockPolicy.isLoosening(app(whole = false), app(whole = true)))
        assertFalse(BlockPolicy.isLoosening(app(blocked = false), app(blocked = true)))
        assertFalse(BlockPolicy.isLoosening(app(start = 9 * 60, end = 18 * 60), app()))
        assertFalse(BlockPolicy.isLoosening(app(days = 1 shl 1), app(days = 127)))
    }

    @Test
    fun shrinkingOrShiftingTheScheduleIsLoosening() {
        assertTrue(BlockPolicy.isLoosening(app(), app(start = 9 * 60, end = 18 * 60)))
        assertTrue(BlockPolicy.isLoosening(app(), app(days = 0b0111110)))
        // Same length, different hours: the old hours are no longer covered.
        assertTrue(BlockPolicy.isLoosening(app(start = 22 * 60, end = 6 * 60), app(start = 23 * 60, end = 7 * 60)))
        assertFalse(BlockPolicy.isLoosening(app(start = 22 * 60, end = 6 * 60), app(start = 21 * 60, end = 7 * 60)))
    }
}
