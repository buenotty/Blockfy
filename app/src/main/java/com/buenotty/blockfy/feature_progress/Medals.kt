package com.buenotty.blockfy.feature_progress

import androidx.annotation.StringRes
import com.buenotty.blockfy.R
import java.time.LocalDate

/** Metal of a medal, from the humble to the rare. */
enum class MedalTier { BRONZE, SILVER, GOLD, PLATINUM, DIAMOND }

/** The picture in the middle of a medal. */
enum class MedalGlyph { SUN, FLAME, SHIELD, BOLT, WALL, PHOENIX, CLOCK, STAR, CROWN }

class MedalDef(
    val id: String,
    val tier: MedalTier,
    val glyph: MedalGlyph,
    @StringRes val title: Int,
    @StringRes val description: Int,
    val earned: (j: Journey, liveRun: Int) -> Boolean
)

/** Every medal, and the rules that unlock them. Medals are earned for good: a later break never takes one back. */
object Medals {

    private fun run(id: String, days: Int, tier: MedalTier, glyph: MedalGlyph, @StringRes title: Int, @StringRes desc: Int) =
        MedalDef(id, tier, glyph, title, desc) { j, live -> maxOf(j.bestRun, j.runDays, live) >= days }

    val all: List<MedalDef> = listOf(
        run("run_1", 1, MedalTier.BRONZE, MedalGlyph.SUN, R.string.medal_run_1_title, R.string.medal_run_1_desc),
        run("run_3", 3, MedalTier.BRONZE, MedalGlyph.FLAME, R.string.medal_run_3_title, R.string.medal_run_3_desc),
        run("run_7", 7, MedalTier.SILVER, MedalGlyph.FLAME, R.string.medal_run_7_title, R.string.medal_run_7_desc),
        run("run_14", 14, MedalTier.SILVER, MedalGlyph.STAR, R.string.medal_run_14_title, R.string.medal_run_14_desc),
        run("run_30", 30, MedalTier.GOLD, MedalGlyph.STAR, R.string.medal_run_30_title, R.string.medal_run_30_desc),
        run("run_90", 90, MedalTier.PLATINUM, MedalGlyph.CROWN, R.string.medal_run_90_title, R.string.medal_run_90_desc),
        run("run_180", 180, MedalTier.DIAMOND, MedalGlyph.CROWN, R.string.medal_run_180_title, R.string.medal_run_180_desc),
        run("run_365", 365, MedalTier.DIAMOND, MedalGlyph.CROWN, R.string.medal_run_365_title, R.string.medal_run_365_desc),
        MedalDef("comeback", MedalTier.BRONZE, MedalGlyph.PHOENIX, R.string.medal_comeback_title, R.string.medal_comeback_desc) { j, _ -> j.comebacks >= 1 },
        MedalDef("comeback_3", MedalTier.SILVER, MedalGlyph.PHOENIX, R.string.medal_comeback_3_title, R.string.medal_comeback_3_desc) { j, _ -> j.comebacks >= 3 },
        MedalDef("resisted_10", MedalTier.BRONZE, MedalGlyph.WALL, R.string.medal_resisted_10_title, R.string.medal_resisted_10_desc) { j, _ -> j.blocks >= 10 },
        MedalDef("resisted_100", MedalTier.SILVER, MedalGlyph.WALL, R.string.medal_resisted_100_title, R.string.medal_resisted_100_desc) { j, _ -> j.blocks >= 100 },
        MedalDef("resisted_500", MedalTier.GOLD, MedalGlyph.WALL, R.string.medal_resisted_500_title, R.string.medal_resisted_500_desc) { j, _ -> j.blocks >= 500 },
        MedalDef("saved_10h", MedalTier.SILVER, MedalGlyph.CLOCK, R.string.medal_saved_10h_title, R.string.medal_saved_10h_desc) { j, _ -> j.savedSeconds >= 10 * 3600L },
        MedalDef("saved_100h", MedalTier.GOLD, MedalGlyph.CLOCK, R.string.medal_saved_100h_title, R.string.medal_saved_100h_desc) { j, _ -> j.savedSeconds >= 100 * 3600L },
        MedalDef("shield_used", MedalTier.BRONZE, MedalGlyph.SHIELD, R.string.medal_shield_title, R.string.medal_shield_desc) { j, _ -> j.slipDays >= 1 },
        MedalDef("prestige_1", MedalTier.PLATINUM, MedalGlyph.BOLT, R.string.medal_prestige_title, R.string.medal_prestige_desc) { j, _ ->
            Ranks.of(j.xp).level > Ranks.LEVELS_PER_CYCLE
        }
    )

    /** Adds every medal that is now earned and not yet owned, stamped with [today]. */
    fun unlock(journey: Journey, liveRun: Int, today: LocalDate): Journey {
        val fresh = all.filter { it.id !in journey.medals && it.earned(journey, liveRun) }
        return if (fresh.isEmpty()) journey else journey.copy(medals = journey.medals + fresh.associate { it.id to today.toString() })
    }
}
