package com.buenotty.blockfy

import com.buenotty.blockfy.feature_monitor.ForegroundEvent
import com.buenotty.blockfy.feature_monitor.UsageStatsMath
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageStatsMathTest {

    private fun resumed(cls: String, t: Long, pkg: String = "p") = ForegroundEvent(pkg, cls, UsageStatsMath.RESUMED, t)
    private fun paused(cls: String, t: Long, pkg: String = "p") = ForegroundEvent(pkg, cls, UsageStatsMath.PAUSED, t)

    @Test
    fun `adds up each resume to pause span`() {
        val events = listOf(resumed("A", 0), paused("A", 60_000), resumed("A", 100_000), paused("A", 130_000))
        assertEquals(90L, UsageStatsMath.foregroundSeconds(events, "p", now = 200_000))
    }

    @Test
    fun `an app still in front counts up to now`() {
        assertEquals(50L, UsageStatsMath.foregroundSeconds(listOf(resumed("A", 10_000)), "p", now = 60_000))
    }

    @Test
    fun `moving between two screens of the same app does not count the overlap twice`() {
        val events = listOf(resumed("A", 0), resumed("B", 20_000), paused("A", 21_000), paused("B", 60_000))
        assertEquals(60L, UsageStatsMath.foregroundSeconds(events, "p", now = 100_000))
    }

    @Test
    fun `other apps are ignored`() {
        val events = listOf(resumed("A", 0, "other"), paused("A", 60_000, "other"), resumed("A", 0), paused("A", 5_000))
        assertEquals(5L, UsageStatsMath.foregroundSeconds(events, "p", now = 100_000))
    }

    @Test
    fun `a pause with no matching resume is ignored`() {
        assertEquals(0L, UsageStatsMath.foregroundSeconds(listOf(paused("A", 5_000)), "p", now = 10_000))
    }
}
