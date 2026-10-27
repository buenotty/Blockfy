package com.buenotty.blockfy

import com.buenotty.blockfy.datastore.AppSettings
import com.buenotty.blockfy.feature_monitor.BlockPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstSetupStreakTest {

    @Test
    fun narrowingTheScheduleBeforeAnyBlockIsNotASlip() {
        val before = AppSettings()
        val after = before.copy(scheduleWeekdays = 0b0111110)
        assertFalse(BlockPolicy.hasAnyProtection(before))
        assertFalse(BlockPolicy.breaksCleanDay(before, after))
    }

    @Test
    fun narrowingTheScheduleWithABlockOnIsASlip() {
        val before = AppSettings().let { it.copy(youtube = it.youtube.copy(blocked = true)) }
        val after = before.copy(scheduleWeekdays = 0b0111110)
        if (BlockPolicy.hasAnyProtection(before)) {
            assertTrue(BlockPolicy.breaksCleanDay(before, after))
        }
    }
}
