package com.buenotty.blockfy.feature_accessibility.overlay

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.buenotty.blockfy.R
import kotlin.random.Random

/** One answer card. [art] is optional: without it the card is just a big button. */
data class ChallengeOption(@StringRes val label: Int, @DrawableRes val art: Int? = null)

/**
 * A harmless trick question shown when a block fires. Both answers are wrong on purpose:
 * the point is a laugh and a pause, not a score. The result text is shared by both options.
 */
data class Challenge(
    val id: String,
    @StringRes val question: Int,
    val a: ChallengeOption,
    val b: ChallengeOption,
    @StringRes val resultTitle: Int,
    @StringRes val resultBody: Int
)

object ChallengeBank {

    val all: List<Challenge> = listOf(
        Challenge(
            id = "bull",
            question = R.string.challenge_bull_q,
            a = ChallengeOption(R.string.challenge_bull_a, R.drawable.challenge_bull_mechanical),
            b = ChallengeOption(R.string.challenge_bull_b, R.drawable.challenge_bull_mechanic),
            resultTitle = R.string.challenge_bull_title,
            resultBody = R.string.challenge_bull_body
        ),
        Challenge(
            id = "one_more",
            question = R.string.challenge_more_q,
            a = ChallengeOption(R.string.challenge_more_a),
            b = ChallengeOption(R.string.challenge_more_b),
            resultTitle = R.string.challenge_more_title,
            resultBody = R.string.challenge_more_body
        ),
        Challenge(
            id = "worker",
            question = R.string.challenge_worker_q,
            a = ChallengeOption(R.string.challenge_worker_a),
            b = ChallengeOption(R.string.challenge_worker_b),
            resultTitle = R.string.challenge_worker_title,
            resultBody = R.string.challenge_worker_body
        ),
        Challenge(
            id = "algorithm",
            question = R.string.challenge_algo_q,
            a = ChallengeOption(R.string.challenge_algo_a),
            b = ChallengeOption(R.string.challenge_algo_b),
            resultTitle = R.string.challenge_algo_title,
            resultBody = R.string.challenge_algo_body
        ),
        Challenge(
            id = "leave_time",
            question = R.string.challenge_leave_q,
            a = ChallengeOption(R.string.challenge_leave_a),
            b = ChallengeOption(R.string.challenge_leave_b),
            resultTitle = R.string.challenge_leave_title,
            resultBody = R.string.challenge_leave_body
        ),
        Challenge(
            id = "minute",
            question = R.string.challenge_minute_q,
            a = ChallengeOption(R.string.challenge_minute_a),
            b = ChallengeOption(R.string.challenge_minute_b),
            resultTitle = R.string.challenge_minute_title,
            resultBody = R.string.challenge_minute_body
        )
    )

    /** Never the same challenge twice in a row, so the joke stays fresh. */
    fun next(lastId: String?, random: Random = Random.Default): Challenge {
        val pool = all.filter { it.id != lastId }.ifEmpty { all }
        return pool[random.nextInt(pool.size)]
    }
}
