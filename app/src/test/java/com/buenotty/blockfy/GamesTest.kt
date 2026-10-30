package com.buenotty.blockfy

import com.buenotty.blockfy.feature_games.Breath
import com.buenotty.blockfy.feature_games.BreathKind
import com.buenotty.blockfy.feature_games.Hold
import com.buenotty.blockfy.feature_games.HoldState
import com.buenotty.blockfy.feature_games.Memory
import com.buenotty.blockfy.feature_games.MemoryState
import com.buenotty.blockfy.feature_games.Quiz
import com.buenotty.blockfy.feature_progress.Journey
import com.buenotty.blockfy.feature_progress.JourneyEngine
import com.buenotty.blockfy.feature_progress.Medals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GamesTest {

    // ---- memory ----

    @Test
    fun `a deal has every face exactly twice, shuffled by the seed`() {
        val a = Memory.deal(pairs = 6, seed = 1L)
        assertEquals(12, a.faces.size)
        assertEquals((0 until 6).toList(), a.faces.sorted().distinct())
        assertTrue(a.faces.groupBy { it }.values.all { it.size == 2 })
        assertEquals(a.faces, Memory.deal(6, 1L).faces)
        assertTrue(a.faces != Memory.deal(6, 2L).faces)
    }

    private fun pairOf(state: MemoryState, face: Int) = state.faces.indices.filter { state.faces[it] == face }

    @Test
    fun `flipping two equal cards matches them`() {
        var s = Memory.deal(3, 5L)
        val (a, b) = pairOf(s, s.faces[0])
        s = Memory.flip(s, a)
        assertEquals(setOf(a), s.revealed)
        s = Memory.flip(s, b)
        assertEquals(setOf(a, b), s.matched)
        assertNull(s.pendingMismatch)
        assertEquals(1, s.moves)
    }

    @Test
    fun `two different cards wait to be hidden and block other flips meanwhile`() {
        var s = Memory.deal(3, 5L)
        val a = 0
        val b = s.faces.indices.first { s.faces[it] != s.faces[a] }
        s = Memory.flip(s, a)
        s = Memory.flip(s, b)
        assertEquals(a to b, s.pendingMismatch)
        val other = s.faces.indices.first { it != a && it != b }
        assertEquals(s, Memory.flip(s, other))
        s = Memory.clearMismatch(s)
        assertTrue(s.revealed.isEmpty() && s.pendingMismatch == null)
    }

    @Test
    fun `flipping a revealed card does nothing and the game is won when all match`() {
        val start = Memory.deal(2, 9L)
        var s = Memory.flip(start, 0)
        assertEquals(s, Memory.flip(s, 0))
        s = start
        for (face in 0 until 2) {
            val (x, y) = pairOf(start, face)
            s = Memory.flip(Memory.flip(s, x), y)
        }
        assertTrue(s.won)
        assertEquals(s, Memory.flip(s, 0))
    }

    // ---- hold ----

    @Test
    fun `holding long enough wins and letting go starts over`() {
        var s = HoldState(goalMillis = 1_000L)
        s = Hold.step(s, nowMillis = 0L, pressed = true)
        s = Hold.step(s, 100L, true)
        s = Hold.step(s, 200L, true)
        assertEquals(0.2f, s.fraction, 0.001f)
        s = Hold.step(s, 300L, false)
        assertEquals(0L, s.heldMillis)
        var now = 400L
        s = Hold.step(s, now, true)
        repeat(12) { now += 100L; s = Hold.step(s, now, true) }
        assertEquals(1f, s.fraction, 0.001f)
        assertTrue(s.won)
    }

    @Test
    fun `a long gap between frames never counts as hold time`() {
        var s = Hold.step(HoldState(goalMillis = 10_000L), 0L, true)
        s = Hold.step(s, 5_000L, true)
        assertTrue(s.heldMillis <= Hold.MAX_FRAME_MILLIS)
    }

    // ---- quiz ----

    @Test
    fun `quiz picks the asked number of distinct questions and scores answers`() {
        var q = Quiz.start(total = 8, count = 5, seed = 3L)
        assertEquals(5, q.order.size)
        assertEquals(5, q.order.toSet().size)
        assertTrue(q.order.all { it in 0 until 8 })
        val correct = intArrayOf(0, 1, 2, 0, 1, 2, 0, 1)
        repeat(5) { i ->
            q = Quiz.answer(q, option = correct[q.order[i]], correct = correct)
            assertEquals(i + 1, q.score)
            q = Quiz.next(q)
        }
        assertTrue(q.finished)
        assertEquals(5, q.score)
    }

    @Test
    fun `a wrong answer scores nothing and answering twice is ignored`() {
        val correct = IntArray(8) { 0 }
        var q = Quiz.start(8, 5, 1L)
        q = Quiz.answer(q, 1, correct)
        assertEquals(0, q.score)
        assertEquals(q, Quiz.answer(q, 0, correct))
    }

    // ---- breathing ----

    @Test
    fun `breathing cycles in, hold, out and repeats`() {
        assertEquals(BreathKind.IN, Breath.at(0L).kind)
        assertEquals(0.5f, Breath.at(Breath.IN_MILLIS / 2).fraction, 0.01f)
        assertEquals(BreathKind.HOLD, Breath.at(Breath.IN_MILLIS + 10L).kind)
        assertEquals(BreathKind.OUT, Breath.at(Breath.IN_MILLIS + Breath.HOLD_MILLIS + 10L).kind)
        assertEquals(BreathKind.IN, Breath.at(Breath.CYCLE_MILLIS + 10L).kind)
        assertEquals(1f, Breath.at(Breath.IN_MILLIS + 10L).size, 0.01f)
        assertTrue(Breath.at(Breath.CYCLE_MILLIS - 10L).size < 0.2f)
    }

    // ---- xp for games ----

    @Test
    fun `each game gives xp up to three a day and counts for the medal`() {
        val d = LocalDate.of(2026, 10, 30)
        var j = Journey()
        repeat(3) { j = JourneyEngine.awardGame(j, d) }
        assertEquals(3 * JourneyEngine.GAME_XP, j.xp)
        assertEquals(3, j.gamesPlayed)
        val capped = JourneyEngine.awardGame(j, d)
        assertEquals(j.xp, capped.xp)
        assertEquals(4, capped.gamesPlayed)
        val tomorrow = JourneyEngine.awardGame(capped, d.plusDays(1))
        assertEquals(capped.xp + JourneyEngine.GAME_XP, tomorrow.xp)
        assertFalse("games_10" in Medals.unlock(tomorrow, 0, d).medals)
        assertTrue("games_10" in Medals.unlock(tomorrow.copy(gamesPlayed = 10), 0, d).medals)
    }
}
