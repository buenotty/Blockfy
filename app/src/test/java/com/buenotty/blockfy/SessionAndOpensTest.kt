package com.buenotty.blockfy

import com.buenotty.blockfy.datastore.AppSettings
import com.buenotty.blockfy.datastore.DailyUsage
import com.buenotty.blockfy.feature_monitor.BlockPolicy
import com.buenotty.blockfy.feature_monitor.BlockReason
import com.buenotty.blockfy.feature_monitor.TrackedPackages
import com.buenotty.blockfy.feature_monitor.UsageMeter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionAndOpensTest {

    private fun settings(session: Int = 0, brk: Int = 0, opens: Int = 0): AppSettings {
        val base = AppSettings()
        return base.copy(
            instagram = base.instagram.copy(
                blocked = true, shortsRuleOn = false,
                sessionLimitMinutes = session, sessionBreakMinutes = brk, opensDailyLimit = opens
            )
        )
    }

    private fun verdict(s: AppSettings, usage: DailyUsage = DailyUsage(), session: Long = 0, now: Long = 1_000_000L) =
        BlockPolicy.evaluate(TrackedPackages.INSTAGRAM, s, usage, nowMillis = now, shortsVisible = false, sessionSeconds = session)

    @Test
    fun `no extra rule means no extra block`() {
        assertEquals(BlockReason.NONE, verdict(settings(), session = 9_999).reason)
    }

    @Test
    fun `session limit blocks once the session is long enough`() {
        val s = settings(session = 10, brk = 15)
        assertEquals(BlockReason.NONE, verdict(s, session = 599).reason)
        val v = verdict(s, session = 600)
        assertEquals(BlockReason.SESSION_LIMIT, v.reason)
        assertTrue(v.wholeApp)
    }

    @Test
    fun `a running break blocks the app until it ends`() {
        val s = settings(session = 10, brk = 15)
        val usage = DailyUsage().withBreak("Instagram", 2_000_000L)
        assertEquals(BlockReason.BREAK, verdict(s, usage, now = 1_999_999L).reason)
        assertEquals(BlockReason.NONE, verdict(s, usage, now = 2_000_000L).reason)
    }

    @Test
    fun `opens limit blocks the open after the last allowed one`() {
        val s = settings(opens = 3)
        assertEquals(BlockReason.NONE, verdict(s, DailyUsage().plusOpens("Instagram", 3)).reason)
        assertEquals(BlockReason.OPENS_LIMIT, verdict(s, DailyUsage().plusOpens("Instagram", 4)).reason)
    }

    @Test
    fun `loosening an extra rule counts as loosening`() {
        val old = settings(session = 10, brk = 15, opens = 3).instagram
        assertTrue(BlockPolicy.isLoosening(old, old.copy(sessionLimitMinutes = 0)))
        assertTrue(BlockPolicy.isLoosening(old, old.copy(sessionLimitMinutes = 20)))
        assertTrue(BlockPolicy.isLoosening(old, old.copy(sessionBreakMinutes = 5)))
        assertTrue(BlockPolicy.isLoosening(old, old.copy(opensDailyLimit = 0)))
        assertTrue(BlockPolicy.isLoosening(old, old.copy(opensDailyLimit = 9)))
        assertFalse(BlockPolicy.isLoosening(old, old.copy(sessionLimitMinutes = 5, opensDailyLimit = 2)))
    }

    @Test
    fun `an app with only extra rules still counts as protected and listened to`() {
        val app = settings(opens = 3).instagram
        assertTrue(BlockPolicy.anyRuleOn(app))
        assertFalse(BlockPolicy.anyRuleOn(app.copy(opensDailyLimit = 0, wholeApp = false, shortsRuleOn = false)))
    }

    @Test
    fun `meter counts an open per new session and keeps a short absence in the same session`() {
        val m = UsageMeter()
        m.credit("Instagram", false, 0L)
        m.credit("Instagram", false, 3_000L)
        assertEquals(3L, m.sessionSeconds("Instagram"))
        m.stop(3_000L)
        // Back after 20 s: same session, not a new open.
        m.credit("Instagram", false, 23_000L)
        m.credit("Instagram", false, 25_000L)
        assertEquals(5L, m.sessionSeconds("Instagram"))
        assertEquals(1, m.drain().opens["Instagram"])
        m.stop(25_000L)
        // Back after 2 min: new session, second open.
        m.credit("Instagram", false, 145_000L)
        assertEquals(0L, m.sessionSeconds("Instagram"))
        assertEquals(1, m.drain().opens["Instagram"])
    }

    @Test
    fun `focus mode blocks every tracked app, even one with no rule, until it ends`() {
        val s = AppSettings().copy(focusUntilEpoch = 2_000_000L)
        val on = BlockPolicy.evaluate(TrackedPackages.TIKTOK, s, DailyUsage(), nowMillis = 1_000_000L)
        assertEquals(BlockReason.FOCUS, on.reason)
        assertTrue(on.wholeApp)
        val off = BlockPolicy.evaluate(TrackedPackages.TIKTOK, s, DailyUsage(), nowMillis = 2_000_000L)
        assertEquals(BlockReason.NONE, off.reason)
        assertTrue(BlockPolicy.isFocusActive(s, 1_999_999L))
        assertFalse(BlockPolicy.isFocusActive(s, 2_000_000L))
    }

    @Test
    fun `week recap sums the last seven days including today`() {
        val today = java.time.LocalDate.of(2026, 10, 29)
        val days = listOf(
            com.buenotty.blockfy.datastore.DayRecord("2026-10-22", blocks = 9, savedSeconds = 9_000, clean = true), // outside the window
            com.buenotty.blockfy.datastore.DayRecord("2026-10-23", blocks = 2, savedSeconds = 600, clean = true),
            com.buenotty.blockfy.datastore.DayRecord("2026-10-27", blocks = 3, savedSeconds = 900, clean = false),
            com.buenotty.blockfy.datastore.DayRecord("2026-10-28", blocks = 1, savedSeconds = 300, clean = true)
        )
        val usage = DailyUsage(date = "2026-10-29", blockedAttemptsToday = 4, savedSeconds = 1_200)
        val recap = com.buenotty.blockfy.feature_monitor.WeekRecap.of(days, usage, todayClean = true, todayDate = today)
        assertEquals(3, recap.cleanDays)
        assertEquals(10, recap.blocks)
        assertEquals(50, recap.savedMinutes)
    }

    // ---- exceptions ("I need it anyway") ----

    private fun limited(minutes: Int): AppSettings {
        val base = AppSettings()
        return base.copy(instagram = base.instagram.copy(blocked = true, wholeApp = true, appTotalDailyLimitMinutes = minutes, shortsRuleOn = false))
    }

    private fun verdictFor(s: AppSettings, usage: DailyUsage, viaBrowser: Boolean = false) =
        BlockPolicy.evaluate(
            TrackedPackages.INSTAGRAM, s, usage, nowMillis = 1_000_000L, shortsVisible = false, viaBrowser = viaBrowser
        )

    @Test
    fun `an app exception adds minutes to the limit`() {
        val usage = DailyUsage(instagramTotalSeconds = 60 * 60L)
        assertEquals(BlockReason.TOTAL_LIMIT, verdictFor(limited(60), usage).reason)
        val granted = usage.withException("Instagram", 300L, browser = false)
        assertEquals(BlockReason.NONE, verdictFor(limited(60), granted).reason)
        assertEquals(BlockReason.TOTAL_LIMIT, verdictFor(limited(60), granted.copy(instagramTotalSeconds = 65 * 60L)).reason)
        assertEquals(1, granted.exceptions)
    }

    @Test
    fun `a browser exception frees the site but keeps the app blocked`() {
        val usage = DailyUsage(instagramTotalSeconds = 60 * 60L).withException("Instagram", 600L, browser = true)
        assertEquals(BlockReason.NONE, verdictFor(limited(60), usage, viaBrowser = true).reason)
        assertEquals(BlockReason.TOTAL_LIMIT, verdictFor(limited(60), usage, viaBrowser = false).reason)
    }

    @Test
    fun `an exception also lets a fully blocked app run for the granted minutes only`() {
        val blocked = limited(0)
        assertEquals(BlockReason.SCHEDULE, verdictFor(blocked, DailyUsage()).reason)
        val granted = DailyUsage().withException("Instagram", 300L, browser = false)
        assertEquals(BlockReason.NONE, verdictFor(blocked, granted).reason)
        assertEquals(BlockReason.TOTAL_LIMIT, verdictFor(blocked, granted.copy(instagramTotalSeconds = 301L)).reason)
    }

    @Test
    fun `granted minutes are not cheating and an exception day earns no under-limit bonus`() {
        val s = limited(60)
        val used = DailyUsage(instagramTotalSeconds = 64 * 60L).withException("Instagram", 300L, browser = false)
        assertEquals(com.buenotty.blockfy.feature_progress.DayOutcome.CLEAN,
            com.buenotty.blockfy.feature_progress.DayJudge.judge(used, s, protectedDay = true))
        assertTrue(com.buenotty.blockfy.feature_progress.DayJudge.usageShare(used, s) >= 1.0)
    }

    @Test
    fun `browser sites map to their app by host and subdomain`() {
        val m = com.buenotty.blockfy.feature_monitor.BrowserSites::appFor
        assertEquals("Instagram", m("https://www.instagram.com/reels/abc"))
        assertEquals("Instagram", m("instagram.com"))
        assertEquals("YouTube", m("m.youtube.com/shorts/xyz"))
        assertEquals("YouTube", m("youtu.be/abc"))
        assertEquals("X", m("x.com/home"))
        assertEquals("Telegram", m("web.telegram.org/k/"))
        assertEquals(null, m("google.com/search?q=instagram.com"))
        assertEquals(null, m("notinstagram.com"))
        assertEquals(null, m("instagram.com.evil.org"))
        assertEquals(null, m(""))
    }

    // ---- fixes from the bug sweep ----

    private val utc = java.util.TimeZone.getTimeZone("UTC")
    private fun at(day: Int, hour: Int, minute: Int = 0) =
        java.time.LocalDateTime.of(2026, 10, day, hour, minute).toInstant(java.time.ZoneOffset.UTC).toEpochMilli()

    @Test
    fun `the schedule window start is midnight for all day and the opening time otherwise`() {
        val allDay = AppSettings()
        assertEquals(at(30, 0), BlockPolicy.scheduleWindowStartMillis(allDay, at(30, 14), utc))
        val nine = AppSettings(scheduleStart = 9 * 60, scheduleEnd = 17 * 60)
        assertEquals(at(30, 9), BlockPolicy.scheduleWindowStartMillis(nine, at(30, 14), utc))
        val night = AppSettings(scheduleStart = 22 * 60, scheduleEnd = 6 * 60)
        assertEquals(at(29, 22), BlockPolicy.scheduleWindowStartMillis(night, at(30, 2), utc))
        assertEquals(at(30, 22), BlockPolicy.scheduleWindowStartMillis(night, at(30, 23), utc))
    }

    @Test
    fun `a run that was not credited yesterday does not show as a run today`() {
        val journey = com.buenotty.blockfy.feature_progress.Journey(runDays = 6, bestRun = 6, lastCredited = "2026-10-24", enlisted = true)
        val usage = DailyUsage(date = "2026-10-30", hadProtection = true)
        val now = java.time.LocalDateTime.of(2026, 10, 30, 12, 0)
        assertEquals(1, com.buenotty.blockfy.feature_progress.JourneyUi.of(journey, usage, AppSettings(adultContentBlockerEnabled = true), now).liveRun)
        val fresh = journey.copy(lastCredited = "2026-10-29")
        assertEquals(7, com.buenotty.blockfy.feature_progress.JourneyUi.of(fresh, usage, AppSettings(adultContentBlockerEnabled = true), now).liveRun)
    }

    @Test
    fun `run medals come only from finished days`() {
        val today = java.time.LocalDate.of(2026, 10, 30)
        val medals = com.buenotty.blockfy.feature_progress.Medals.unlock(com.buenotty.blockfy.feature_progress.Journey(runDays = 6), liveRun = 7, today = today).medals
        assertTrue("run_3" in medals)
        assertFalse("run_7" in medals)
    }

    @Test
    fun `hopping between two apps keeps each session going`() {
        val m = UsageMeter()
        m.credit("Instagram", false, 0L)
        m.credit("Instagram", false, 4_000L)
        m.credit("YouTube", false, 5_000L)
        m.credit("YouTube", false, 8_000L)
        // Back to Instagram 3 s after leaving: the same session, no new open.
        m.credit("Instagram", false, 8_000L)
        m.credit("Instagram", false, 10_000L)
        assertEquals(6L, m.sessionSeconds("Instagram"))
        assertEquals(1, m.drain().opens["Instagram"])
    }

    @Test
    fun `resetting the session after a break starts from zero`() {
        val m = UsageMeter()
        m.credit("Instagram", false, 0L)
        m.credit("Instagram", false, 4_000L)
        m.resetSession()
        assertEquals(0L, m.sessionSeconds("Instagram"))
    }
}
