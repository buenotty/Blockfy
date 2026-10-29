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

    @Test
    fun `resetting the session after a break starts from zero`() {
        val m = UsageMeter()
        m.credit("Instagram", false, 0L)
        m.credit("Instagram", false, 4_000L)
        m.resetSession()
        assertEquals(0L, m.sessionSeconds("Instagram"))
    }
}
