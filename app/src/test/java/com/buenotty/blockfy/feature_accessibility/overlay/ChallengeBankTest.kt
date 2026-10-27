package com.buenotty.blockfy.feature_accessibility.overlay

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeBankTest {

    @Test
    fun pickNeverRepeatsTheLastIndex() {
        val random = Random(42)
        for (size in 2..30) for (last in 0 until size) repeat(20) {
            assertNotEquals(last, ChallengeBank.pick(size, last, random))
        }
    }

    @Test
    fun pickStaysInRangeAndReachesEveryIndex() {
        val random = Random(7)
        val seen = mutableSetOf<Int>()
        repeat(500) {
            val i = ChallengeBank.pick(10, 3, random)
            assertTrue(i in 0 until 10)
            seen += i
        }
        assertEquals((0 until 10).toSet() - 3, seen)
    }

    @Test
    fun pickHandlesTinyBanksAndNoHistory() {
        assertEquals(0, ChallengeBank.pick(1, 0))
        assertEquals(0, ChallengeBank.pick(0, null))
        assertTrue(ChallengeBank.pick(5, null, Random(1)) in 0 until 5)
    }
}
