package com.buenotty.blockfy.feature_games

import kotlin.random.Random

/*
 * The rules of the short games, kept apart from the screens so they can be tested without a
 * phone. Every game is finite on purpose: it ends, says something kind, and lets go of you.
 */

// ---- Memory ---------------------------------------------------------------------------------

/** One round of "find the pairs". [faces] holds a face number per card; each face appears twice. */
data class MemoryState(
    val faces: List<Int>,
    val revealed: Set<Int> = emptySet(),
    val matched: Set<Int> = emptySet(),
    /** Two cards that did not match, waiting to be turned back; flipping is locked until then. */
    val pendingMismatch: Pair<Int, Int>? = null,
    val moves: Int = 0
) {
    val won: Boolean get() = faces.isNotEmpty() && matched.size == faces.size
}

object Memory {
    fun deal(pairs: Int, seed: Long): MemoryState =
        MemoryState(faces = (0 until pairs).flatMap { listOf(it, it) }.shuffled(Random(seed)))

    fun flip(state: MemoryState, index: Int): MemoryState {
        if (index !in state.faces.indices) return state
        if (state.pendingMismatch != null || index in state.matched || index in state.revealed) return state
        val shown = state.revealed + index
        if (shown.size < 2) return state.copy(revealed = shown)
        val (a, b) = shown.toList()
        return if (state.faces[a] == state.faces[b]) {
            state.copy(revealed = emptySet(), matched = state.matched + shown, moves = state.moves + 1)
        } else {
            state.copy(revealed = shown, pendingMismatch = a to b, moves = state.moves + 1)
        }
    }

    /** Turns a wrong pair back over. */
    fun clearMismatch(state: MemoryState): MemoryState =
        if (state.pendingMismatch == null) state else state.copy(revealed = emptySet(), pendingMismatch = null)
}

// ---- Hold on ---------------------------------------------------------------------------------

/** "Hold on": keep a finger down for [goalMillis]. Letting go starts over. */
data class HoldState(val goalMillis: Long, val heldMillis: Long = 0L, val lastMillis: Long = NO_FRAME) {
    val fraction: Float get() = (heldMillis.toFloat() / goalMillis).coerceIn(0f, 1f)
    val won: Boolean get() = heldMillis >= goalMillis

    companion object {
        const val NO_FRAME = -1L
    }
}

object Hold {
    /** A frame that arrives late (the app was paused) must not count as a long hold. */
    const val MAX_FRAME_MILLIS = 500L

    fun step(state: HoldState, nowMillis: Long, pressed: Boolean): HoldState {
        if (state.won) return state
        if (!pressed) return state.copy(heldMillis = 0L, lastMillis = HoldState.NO_FRAME)
        val delta = if (state.lastMillis == HoldState.NO_FRAME) 0L else (nowMillis - state.lastMillis).coerceIn(0L, MAX_FRAME_MILLIS)
        return state.copy(heldMillis = state.heldMillis + delta, lastMillis = nowMillis)
    }
}

// ---- Quiz -------------------------------------------------------------------------------------

/** A short quiz: [count] distinct questions out of [total], in a seeded order. */
data class QuizState(
    val order: List<Int>,
    val index: Int = 0,
    val score: Int = 0,
    /** The option picked for the current question, or null while it is still open. */
    val picked: Int? = null
) {
    val finished: Boolean get() = index >= order.size
    val current: Int get() = order[index]
}

object Quiz {
    fun start(total: Int, count: Int, seed: Long): QuizState =
        QuizState(order = (0 until total).shuffled(Random(seed)).take(count.coerceAtMost(total)))

    fun answer(state: QuizState, option: Int, correct: IntArray): QuizState {
        if (state.finished || state.picked != null) return state
        val hit = correct[state.current] == option
        return state.copy(picked = option, score = state.score + if (hit) 1 else 0)
    }

    fun next(state: QuizState): QuizState = if (state.picked == null) state else state.copy(index = state.index + 1, picked = null)
}

// ---- Breathing --------------------------------------------------------------------------------

enum class BreathKind { IN, HOLD, OUT }

/** Where a breath is at: its step, how far through the step (0..1), and the circle's size (0..1). */
data class BreathStep(val kind: BreathKind, val fraction: Float, val size: Float)

/** In for four seconds, hold for four, out for six, again and again. */
object Breath {
    const val IN_MILLIS = 4_000L
    const val HOLD_MILLIS = 4_000L
    const val OUT_MILLIS = 6_000L
    const val CYCLE_MILLIS = IN_MILLIS + HOLD_MILLIS + OUT_MILLIS
    private const val SMALLEST = 0.15f

    fun at(elapsedMillis: Long): BreathStep {
        val t = ((elapsedMillis % CYCLE_MILLIS) + CYCLE_MILLIS) % CYCLE_MILLIS
        return when {
            t < IN_MILLIS -> {
                val f = t.toFloat() / IN_MILLIS
                BreathStep(BreathKind.IN, f, SMALLEST + (1f - SMALLEST) * f)
            }
            t < IN_MILLIS + HOLD_MILLIS -> BreathStep(BreathKind.HOLD, (t - IN_MILLIS).toFloat() / HOLD_MILLIS, 1f)
            else -> {
                val f = (t - IN_MILLIS - HOLD_MILLIS).toFloat() / OUT_MILLIS
                BreathStep(BreathKind.OUT, f, 1f - (1f - SMALLEST) * f)
            }
        }
    }
}
