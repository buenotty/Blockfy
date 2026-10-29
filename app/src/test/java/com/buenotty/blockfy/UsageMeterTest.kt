package com.buenotty.blockfy

import com.buenotty.blockfy.feature_monitor.UsageMeter
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageMeterTest {

    @Test
    fun `sub-second remainders are not lost between credits`() {
        val meter = UsageMeter()
        var now = 1_000L
        meter.credit("Instagram", shorts = true, now = now)
        // 100 events 1.9 s apart: 190 s of wall clock. Resetting the clock to "now" on each
        // credit would count only 100 s.
        repeat(100) {
            now += 1_900L
            meter.credit("Instagram", shorts = true, now = now)
        }
        assertEquals(190L, meter.pendingTotal["Instagram"])
        assertEquals(190L, meter.pendingShorts["Instagram"])
    }

    @Test
    fun `a long silent gap is capped instead of credited in full`() {
        val meter = UsageMeter()
        meter.credit("YouTube", shorts = false, now = 0L)
        meter.credit("YouTube", shorts = false, now = 60_000L)
        assertEquals(UsageMeter.MAX_CREDIT_SECONDS, meter.pendingTotal["YouTube"])
        assertEquals(null, meter.pendingShorts["YouTube"])
    }

    @Test
    fun `switching app starts a new session without crediting the gap`() {
        val meter = UsageMeter()
        meter.credit("Instagram", shorts = false, now = 0L)
        meter.credit("YouTube", shorts = false, now = 3_000L)
        assertEquals(true, meter.pendingTotal.isEmpty())
    }

    @Test
    fun `drain empties the pending maps`() {
        val meter = UsageMeter()
        meter.credit("X", shorts = false, now = 0L)
        meter.credit("X", shorts = false, now = 2_000L)
        val (total, shorts) = meter.drain()
        assertEquals(mapOf("X" to 2L), total)
        assertEquals(true, shorts.isEmpty())
        assertEquals(true, meter.pendingTotal.isEmpty())
    }
}
