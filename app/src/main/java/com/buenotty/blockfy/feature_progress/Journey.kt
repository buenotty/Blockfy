package com.buenotty.blockfy.feature_progress

import androidx.datastore.core.Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToLong

/**
 * Everything that is kept for good about a user's progress. Unlike the 90-day history it never
 * rolls over, so a year-long run and a high rank survive. All of it stays on the phone.
 */
@Serializable
data class Journey(
    val xp: Long = 0L,
    /** Consecutive credited clean days up to yesterday. Today is added on screen while it is still clean. */
    val runDays: Int = 0,
    val bestRun: Int = 0,
    val cleanDays: Int = 0,
    val slipDays: Int = 0,
    val breakDays: Int = 0,
    val comebacks: Int = 0,
    val blocks: Int = 0,
    val savedSeconds: Long = 0L,
    /** ISO date of the last day credited; each day is credited once. */
    val lastCredited: String = "",
    val lastWasBreak: Boolean = false,
    /** Medal id to the date it was earned. Never removed. */
    val medals: Map<String, String> = emptyMap(),
    /** Medals whose unlock the user already saw celebrated. */
    val seenMedals: List<String> = emptyList(),
    /** Highest level whose promotion the user already saw. */
    val seenLevel: Int = 0,
    /** "2026-10" to the number of shields spent that month. */
    val slipMonths: Map<String, Int> = emptyMap(),
    /** The first protection was turned on, which gave the first level straight away. */
    val enlisted: Boolean = false,
    /** Short games finished, ever. */
    val gamesPlayed: Int = 0,
    /** The day [gamesToday] counts for, so the daily cap resets by itself. */
    val gameDay: String = "",
    /** Games finished on [gameDay] that gave XP. */
    val gamesToday: Int = 0
)

object JourneyEngine {
    const val SHIELDS_PER_MONTH = 2

    /** Turning on the first protection is the first step, so it already makes a recruit. */
    const val ENLIST_XP = 100L

    /** A finished game is worth a little, and only the first few each day, so it never becomes the new scroll. */
    const val GAME_XP = 15L
    const val GAMES_PER_DAY = 3

    fun awardGame(j: Journey, date: LocalDate): Journey {
        val today = date.toString()
        val used = if (j.gameDay == today) j.gamesToday else 0
        val pays = used < GAMES_PER_DAY
        return j.copy(
            xp = j.xp + if (pays) GAME_XP else 0L,
            gamesPlayed = j.gamesPlayed + 1,
            gameDay = today,
            gamesToday = if (pays) used + 1 else used
        )
    }

    private const val CLEAN_XP = 100
    private const val WELL_UNDER_BONUS = 50
    private const val UNDER_BONUS = 20
    private const val SLIP_XP = 40
    private const val RESISTED_XP = 5
    private const val RESISTED_CAP = 10
    private const val RUN_BONUS_PER_DAY = 0.02
    private const val RUN_BONUS_CAP_DAYS = 25

    private fun monthKey(date: LocalDate) = YearMonth.from(date).toString()

    fun shieldsLeft(j: Journey, date: LocalDate): Int =
        (SHIELDS_PER_MONTH - (j.slipMonths[monthKey(date)] ?: 0)).coerceAtLeast(0)

    /** A slip is forgiven while the month still has shields; after that it counts as a break. */
    fun resolve(j: Journey, date: LocalDate, raw: DayOutcome): DayOutcome =
        if (raw == DayOutcome.SLIP && shieldsLeft(j, date) == 0) DayOutcome.BREAK else raw

    /**
     * Books one finished day. Safe to call twice for the same day. [outcome] must already be
     * resolved. A missing stretch of days (the phone was off or the app never ran) ends the run
     * without counting as a break.
     */
    fun creditDay(
        journey: Journey,
        date: LocalDate,
        outcome: DayOutcome,
        usageShare: Double,
        blocks: Int,
        savedSeconds: Long
    ): Journey {
        if (journey.lastCredited.isNotEmpty() && date.toString() <= journey.lastCredited) return journey
        var j = journey
        if (j.lastCredited.isNotEmpty() && date.isAfter(LocalDate.parse(j.lastCredited).plusDays(1))) j = j.copy(runDays = 0)

        val multiplier = 1.0 + minOf(j.runDays, RUN_BONUS_CAP_DAYS) * RUN_BONUS_PER_DAY
        j = when (outcome) {
            DayOutcome.CLEAN -> {
                val bonus = when {
                    usageShare <= 0.5 -> WELL_UNDER_BONUS
                    usageShare <= 0.8 -> UNDER_BONUS
                    else -> 0
                }
                val xp = ((CLEAN_XP + bonus + minOf(blocks, RESISTED_CAP) * RESISTED_XP) * multiplier).roundToLong()
                j.copy(
                    xp = j.xp + xp,
                    runDays = j.runDays + 1,
                    cleanDays = j.cleanDays + 1,
                    comebacks = j.comebacks + if (j.lastWasBreak) 1 else 0,
                    lastWasBreak = false
                )
            }
            DayOutcome.SLIP -> j.copy(
                xp = j.xp + (SLIP_XP * multiplier).roundToLong(),
                runDays = j.runDays + 1,
                slipDays = j.slipDays + 1,
                slipMonths = j.slipMonths + (monthKey(date) to (j.slipMonths[monthKey(date)] ?: 0) + 1)
            )
            DayOutcome.BREAK -> j.copy(runDays = 0, breakDays = j.breakDays + 1, lastWasBreak = true)
            DayOutcome.NONE -> j.copy(runDays = 0)
        }
        return j.copy(
            bestRun = maxOf(j.bestRun, j.runDays),
            blocks = j.blocks + blocks,
            savedSeconds = j.savedSeconds + savedSeconds,
            lastCredited = date.toString()
        )
    }
}

/** The next clean-time goal and how far along it the current run is. */
data class Goal(val days: Int, val fraction: Double)

object Goals {
    private val LADDER = listOf(1, 3, 7, 14, 30, 60, 90, 180, 365)
    private const val YEAR = 365

    /** [runDays] may be fractional so "34% of one day" moves through the day. */
    fun next(runDays: Double): Goal {
        val days = LADDER.firstOrNull { it > runDays } ?: ((runDays / YEAR).toInt() + 1) * YEAR
        return Goal(days, (runDays / days).coerceIn(0.0, 1.0))
    }
}

@Suppress("BlockingMethodInNonBlockingContext")
object JourneySerializer : Serializer<Journey> {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; encodeDefaults = true }

    override val defaultValue: Journey get() = Journey()

    override suspend fun readFrom(input: InputStream): Journey = try {
        json.decodeFromString(Journey.serializer(), input.readBytes().decodeToString())
    } catch (e: Exception) {
        defaultValue
    }

    override suspend fun writeTo(t: Journey, output: OutputStream) {
        output.write(json.encodeToString(Journey.serializer(), t).encodeToByteArray())
    }
}
