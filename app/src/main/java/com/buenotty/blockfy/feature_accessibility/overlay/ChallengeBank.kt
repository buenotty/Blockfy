package com.buenotty.blockfy.feature_accessibility.overlay

import android.content.res.Resources
import com.buenotty.blockfy.R
import kotlin.random.Random

/** One answer card. [art] is optional: without it the card is just a big button. */
data class ChallengeOption(val label: String, val art: Int? = null)

/**
 * A harmless trick question shown when a block fires. Both answers are wrong on purpose:
 * the point is a laugh and a pause, not a score. The result text is shared by both options.
 */
data class Challenge(
    val question: String,
    val a: ChallengeOption,
    val b: ChallengeOption,
    val resultTitle: String,
    val resultBody: String
)

object ChallengeBank {

    /** Index 0 is the signature bull photo challenge, the rest come from the string arrays. */
    fun load(res: Resources): List<Challenge> {
        val q = res.getStringArray(R.array.challenge_q)
        val a = res.getStringArray(R.array.challenge_a)
        val b = res.getStringArray(R.array.challenge_b)
        val title = res.getStringArray(R.array.challenge_title)
        val body = res.getStringArray(R.array.challenge_body)
        val count = minOf(q.size, a.size, b.size, title.size, body.size)
        val bull = Challenge(
            question = res.getString(R.string.challenge_bull_q),
            a = ChallengeOption(res.getString(R.string.challenge_bull_a), R.drawable.challenge_bull_mechanical),
            b = ChallengeOption(res.getString(R.string.challenge_bull_b), R.drawable.challenge_bull_mechanic),
            resultTitle = res.getString(R.string.challenge_bull_title),
            resultBody = res.getString(R.string.challenge_bull_body)
        )
        return listOf(bull) + List(count) {
            Challenge(q[it], ChallengeOption(a[it]), ChallengeOption(b[it]), title[it], body[it])
        }
    }

    /** A random index that is never the same as [last], so the joke stays fresh. */
    fun pick(size: Int, last: Int?, random: Random = Random.Default): Int {
        if (size <= 1) return 0
        var i = random.nextInt(size - 1)
        if (last != null && i >= last) i++
        return i
    }
}
