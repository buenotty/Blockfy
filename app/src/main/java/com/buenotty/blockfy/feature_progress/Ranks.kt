package com.buenotty.blockfy.feature_progress

import kotlin.math.pow
import kotlin.math.roundToLong

/** Where a total of XP sits on the ladder: a level, its rank and sub-rank, the prestige cycle and the progress to the next level. */
data class RankState(
    /** 0 is "common person"; 1 and up are real levels, with no ceiling. */
    val level: Int,
    /** How many full ladders were climbed. 0 on the first one. */
    val prestige: Int,
    /** 0..9 (Recruit to General), or -1 for a common person. */
    val rankIndex: Int,
    /** 0..2, shown as I, II, III. */
    val sub: Int,
    val xpInto: Long,
    val xpNeeded: Long
) {
    val fraction: Double get() = if (xpNeeded <= 0L) 0.0 else (xpInto.toDouble() / xpNeeded).coerceIn(0.0, 1.0)
}

/**
 * The ladder: ten ranks of three levels, Recruit to General, then prestige starts it over from
 * Recruit with a star, each cycle costing a bit more than the one before, so there is always a
 * next level. The first clean day already makes a recruit; General III takes about half a year.
 */
object Ranks {
    const val LEVELS_PER_CYCLE = 30
    const val RANK_COUNT = 10

    private const val BASE_XP = 100.0
    private const val CURVE = 1.6
    private const val PRESTIGE_COST_STEP = 0.35

    /** XP needed to be exactly at [level] (0 for level 0). */
    fun xpForLevel(level: Int): Long {
        var total = 0L
        for (l in 1..level) total += costOf(l)
        return total
    }

    private fun costOf(level: Int): Long {
        val prestige = (level - 1) / LEVELS_PER_CYCLE
        val n = (level - 1) % LEVELS_PER_CYCLE + 1
        val step = n.toDouble().pow(CURVE) - (n - 1).toDouble().pow(CURVE)
        return (BASE_XP * step * (1.0 + PRESTIGE_COST_STEP * prestige)).roundToLong().coerceAtLeast(1L)
    }

    fun of(xp: Long): RankState {
        var level = 0
        var floor = 0L
        while (true) {
            val next = floor + costOf(level + 1)
            if (next > xp) break
            floor = next
            level++
        }
        val needed = costOf(level + 1)
        if (level == 0) return RankState(0, 0, -1, 0, xp - floor, needed)
        val prestige = (level - 1) / LEVELS_PER_CYCLE
        val n = (level - 1) % LEVELS_PER_CYCLE + 1
        return RankState(level, prestige, (n - 1) / 3, (n - 1) % 3, xp - floor, needed)
    }
}
