package com.buenotty.blockfy

import com.buenotty.blockfy.datastore.AppSettings
import com.buenotty.blockfy.datastore.DailyUsage
import com.buenotty.blockfy.feature_monitor.BlockPolicy
import com.buenotty.blockfy.feature_monitor.BlockReason
import com.buenotty.blockfy.feature_monitor.TrackedPackages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInTest {

    @Test
    fun `accepting blocks exactly the named apps as a whole`() {
        val out = BlockPolicy.applyCheckIn(AppSettings(), listOf("X", "Telegram"))
        assertTrue(out.x.blocked && out.telegram.blocked)
        assertFalse(out.instagram.blocked)
        assertEquals(listOf("X", "Telegram"), out.seenAdultIn)
        assertTrue(out.checkInDone)
    }

    @Test
    fun `a blocked app with a whole-app rule at zero minutes blocks the whole app`() {
        val out = BlockPolicy.applyCheckIn(AppSettings(), listOf("Instagram"))
        val verdict = BlockPolicy.evaluate(TrackedPackages.INSTAGRAM, out, DailyUsage(), shortsVisible = false)
        assertEquals(BlockReason.SCHEDULE, verdict.reason)
        assertTrue(verdict.wholeApp)
    }

    @Test
    fun `answering none only marks the check-in done`() {
        val out = BlockPolicy.applyCheckIn(AppSettings(), emptyList())
        assertTrue(out.checkInDone)
        assertTrue(out.seenAdultIn.isEmpty())
        assertFalse(out.apps.any { it.blocked })
    }

    @Test
    fun `unknown apps are ignored and repeats are not duplicated`() {
        val first = BlockPolicy.applyCheckIn(AppSettings(), listOf("Reddit", "X", "X"))
        assertEquals(listOf("X"), first.seenAdultIn)
        val again = BlockPolicy.applyCheckIn(first, listOf("X"))
        assertEquals(listOf("X"), again.seenAdultIn)
    }
}
