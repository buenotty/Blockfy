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

    /** 2026-01-05 is a Monday. Offset 0 = Monday ... 6 = Sunday. */
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

    private fun app(
        blocked: Boolean = true,
        whole: Boolean = false,
        wholeLimit: Int = 0,
        shorts: Boolean = true,
        shortsLimit: Int = 0
    ) = App(
        name = "Instagram", blocked = blocked, blockedStart = 0, blockedEnd = 1439, blockedTimer = 0,
        features = emptyList(), dailyLimitMinutes = shortsLimit, appTotalDailyLimitMinutes = wholeLimit,
        wholeApp = whole, shortsRuleOn = shorts
    )

    // ---- the two independent rules --------------------------------------------------------

    @Test
    fun masterSwitchOffMeansNothingIsEverBlocked() {
        val s = settings {
            it.copy(
                instagram = it.instagram.copy(blocked = false, dailyLimitMinutes = 1, wholeApp = true, appTotalDailyLimitMinutes = 1),
                tiktok = it.tiktok.copy(blocked = false, appTotalDailyLimitMinutes = 1)
            )
        }
        val heavyUsage = DailyUsage(instagramSeconds = 9_999, instagramTotalSeconds = 9_999, tiktokTotalSeconds = 9_999)
        assertFalse(evaluate(TrackedPackages.INSTAGRAM, s, heavyUsage).shouldBlock)
        assertFalse(evaluate(TrackedPackages.TIKTOK, s, heavyUsage).shouldBlock)
    }

    @Test
    fun shortsRuleBlocksOnlyWhileShortsAreOnScreen() {
        val s = settings { it.copy(instagram = it.instagram.copy(blocked = true)) }
        val onScreen = evaluate(TrackedPackages.INSTAGRAM, s, shortsVisible = true)
        assertEquals(BlockReason.SCHEDULE, onScreen.reason)
        assertFalse(onScreen.wholeApp)
        assertEquals(BlockReason.NONE, evaluate(TrackedPackages.INSTAGRAM, s, shortsVisible = false).reason)
    }

    @Test
    fun wholeAppRuleBlocksEvenWithoutShorts() {
        val s = settings { it.copy(instagram = it.instagram.copy(blocked = true, wholeApp = true, shortsRuleOn = false)) }
        val verdict = evaluate(TrackedPackages.INSTAGRAM, s, shortsVisible = false)
        assertEquals(BlockReason.SCHEDULE, verdict.reason)
        assertTrue(verdict.wholeApp)
    }

    @Test
    fun oneHourOfInstagramButOnlyFifteenMinutesOfReels() {
        val s = settings {
            it.copy(
                instagram = it.instagram.copy(
                    blocked = true, wholeApp = true, appTotalDailyLimitMinutes = 60,
                    shortsRuleOn = true, dailyLimitMinutes = 15
                )
            )
        }
        // 20 minutes in the app, 10 of them in Reels: nothing is blocked yet.
        val early = DailyUsage(instagramTotalSeconds = 20 * 60, instagramSeconds = 10 * 60)
        assertFalse(evaluate(TrackedPackages.INSTAGRAM, s, early, shortsVisible = true).shouldBlock)

        // 15 minutes of Reels: Reels are blocked, the rest of the app is still free.
        val reelsDone = DailyUsage(instagramTotalSeconds = 30 * 60, instagramSeconds = 15 * 60)
        val onReels = evaluate(TrackedPackages.INSTAGRAM, s, reelsDone, shortsVisible = true)
        assertEquals(BlockReason.DAILY_LIMIT, onReels.reason)
        assertFalse(onReels.wholeApp)
        assertFalse(evaluate(TrackedPackages.INSTAGRAM, s, reelsDone, shortsVisible = false).shouldBlock)

        // One hour in the app: everything is blocked, Reels on screen or not.
        val appDone = DailyUsage(instagramTotalSeconds = 60 * 60, instagramSeconds = 5 * 60)
        val whole = evaluate(TrackedPackages.INSTAGRAM, s, appDone, shortsVisible = false)
        assertEquals(BlockReason.TOTAL_LIMIT, whole.reason)
        assertTrue(whole.wholeApp)
    }

    @Test
    fun wholeAppLimitUsesTotalSecondsNotShortsSeconds() {
        val s = settings {
            it.copy(instagram = it.instagram.copy(blocked = true, wholeApp = true, appTotalDailyLimitMinutes = 20, shortsRuleOn = false))
        }
        val shortsOnly = evaluate(TrackedPackages.INSTAGRAM, s, DailyUsage(instagramSeconds = 3_000, instagramTotalSeconds = 60))
        val total = evaluate(TrackedPackages.INSTAGRAM, s, DailyUsage(instagramTotalSeconds = 20 * 60))
        assertFalse(shortsOnly.shouldBlock)
        assertEquals(BlockReason.TOTAL_LIMIT, total.reason)
    }

    @Test
    fun shortsLimitBlocksAtTheLimit() {
        val s = settings { it.copy(youtube = it.youtube.copy(blocked = true, dailyLimitMinutes = 10)) }
        assertFalse(evaluate(TrackedPackages.YOUTUBE, s, DailyUsage(youtubeSeconds = 9 * 60 + 59)).shouldBlock)
        assertEquals(BlockReason.DAILY_LIMIT, evaluate(TrackedPackages.YOUTUBE, s, DailyUsage(youtubeSeconds = 10 * 60)).reason)
    }

    @Test
    fun tiktokAndXAreAlwaysWholeApp() {
        val s = settings { it.copy(tiktok = it.tiktok.copy(blocked = true), x = it.x.copy(blocked = true)) }
        assertTrue(evaluate(TrackedPackages.TIKTOK, s, shortsVisible = false).wholeApp)
        assertEquals(BlockReason.SCHEDULE, evaluate(TrackedPackages.X, s, shortsVisible = false).reason)
    }

    @Test
    fun bankPackagesNeverProduceAVerdict() {
        val s = settings { it.copy(instagram = it.instagram.copy(blocked = true, wholeApp = true)) }
        BankPackages.ALL.forEach { bank ->
            assertEquals(BlockReason.NONE, evaluate(bank, s).reason)
        }
    }

    // ---- global schedule -------------------------------------------------------------------

    @Test
    fun outsideTheGlobalScheduleNothingIsBlockedEvenOverTheLimit() {
        val s = settings {
            it.copy(
                scheduleStart = 22 * 60, scheduleEnd = 23 * 60,
                tiktok = it.tiktok.copy(blocked = true, appTotalDailyLimitMinutes = 1)
            )
        }
        val usage = DailyUsage(tiktokTotalSeconds = 5_000)
        assertFalse(evaluate(TrackedPackages.TIKTOK, s, usage, now = at(0, 12)).shouldBlock)
        assertTrue(evaluate(TrackedPackages.TIKTOK, s, usage, now = at(0, 22, 30)).shouldBlock)
    }

    @Test
    fun globalWeekdaysSelectTheRightDays() {
        val s = settings { it.copy(scheduleWeekdays = 1 shl 1, instagram = it.instagram.copy(blocked = true)) }
        assertTrue(evaluate(TrackedPackages.INSTAGRAM, s, now = at(0, 12)).shouldBlock)
        assertFalse(evaluate(TrackedPackages.INSTAGRAM, s, now = at(1, 12)).shouldBlock)
        assertTrue(BlockPolicy.isActiveWeekday(127, at(3, 12), utc))
    }

    @Test
    fun overnightWindowBelongsToTheDayItStarts() {
        val fridayNight = AppSettings(scheduleStart = 22 * 60, scheduleEnd = 6 * 60, scheduleWeekdays = 1 shl 5)
        assertTrue(BlockPolicy.isScheduleActive(fridayNight, at(4, 23), utc))
        assertTrue(BlockPolicy.isScheduleActive(fridayNight, at(5, 2), utc))
        assertFalse(BlockPolicy.isScheduleActive(fridayNight, at(5, 23), utc))
        assertFalse(BlockPolicy.isScheduleActive(fridayNight, at(4, 2), utc))
        assertFalse(BlockPolicy.isScheduleActive(fridayNight, at(4, 12), utc))
    }

    @Test
    fun intervalHelperWrapsMidnight() {
        assertTrue(BlockPolicy.isWithinInterval(22 * 60, 6 * 60, 23 * 60))
        assertTrue(BlockPolicy.isWithinInterval(22 * 60, 6 * 60, 30))
        assertFalse(BlockPolicy.isWithinInterval(22 * 60, 6 * 60, 12 * 60))
    }

    // ---- loosening -------------------------------------------------------------------------

    @Test
    fun turningOffDroppingARuleOrRaisingALimitIsLoosening() {
        assertTrue(BlockPolicy.isLoosening(app(), app(blocked = false)))
        assertTrue(BlockPolicy.isLoosening(app(shorts = true), app(shorts = false)))
        assertTrue(BlockPolicy.isLoosening(app(whole = true), app(whole = false)))
        assertTrue(BlockPolicy.isLoosening(app(shortsLimit = 15), app(shortsLimit = 30)))
        assertTrue(BlockPolicy.isLoosening(app(shortsLimit = 0), app(shortsLimit = 15)))
        assertTrue(BlockPolicy.isLoosening(app(whole = true, wholeLimit = 60), app(whole = true, wholeLimit = 90)))
    }

    @Test
    fun makingABlockStricterIsNeverLoosening() {
        assertFalse(BlockPolicy.isLoosening(app(shortsLimit = 30), app(shortsLimit = 15)))
        assertFalse(BlockPolicy.isLoosening(app(shortsLimit = 30), app(shortsLimit = 0)))
        assertFalse(BlockPolicy.isLoosening(app(whole = false), app(whole = true)))
        assertFalse(BlockPolicy.isLoosening(app(shorts = false), app(shorts = true)))
        assertFalse(BlockPolicy.isLoosening(app(blocked = false), app(blocked = true)))
    }

    @Test
    fun narrowingOrShiftingTheScheduleIsLoosening() {
        val all = AppSettings()
        assertTrue(BlockPolicy.isScheduleLoosening(all, all.copy(scheduleStart = 9 * 60, scheduleEnd = 18 * 60)))
        assertTrue(BlockPolicy.isScheduleLoosening(all, all.copy(scheduleWeekdays = 0b0111110)))
        val night = all.copy(scheduleStart = 22 * 60, scheduleEnd = 6 * 60)
        assertTrue(BlockPolicy.isScheduleLoosening(night, night.copy(scheduleStart = 23 * 60, scheduleEnd = 7 * 60)))
        assertFalse(BlockPolicy.isScheduleLoosening(night, night.copy(scheduleStart = 21 * 60, scheduleEnd = 7 * 60)))
        assertFalse(BlockPolicy.isScheduleLoosening(AppSettings(scheduleStart = 9 * 60, scheduleEnd = 18 * 60), all))
    }

    // ---- usage and strict mode --------------------------------------------------------------

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
}
