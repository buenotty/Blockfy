package com.buenotty.blockfy.feature_accessibility.overlay

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeBankTest {

    @Test
    fun idsAreUnique() {
        val ids = ChallengeBank.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun everyChallengeOffersTwoDifferentAnswers() {
        ChallengeBank.all.forEach {
            assertNotEquals("${it.id}: answers must differ", it.a.label, it.b.label)
        }
    }

    @Test
    fun artIsAllOrNothing() {
        ChallengeBank.all.forEach {
            assertEquals("${it.id}: both answers need art, or neither", it.a.art == null, it.b.art == null)
        }
    }

    @Test
    fun nextNeverRepeatsTheLastChallenge() {
        val random = Random(42)
        ChallengeBank.all.forEach { last ->
            repeat(50) {
                assertNotEquals(last.id, ChallengeBank.next(last.id, random).id)
            }
        }
    }

    @Test
    fun nextWorksWithNoHistory() {
        assertTrue(ChallengeBank.next(null, Random(1)) in ChallengeBank.all)
    }
}
