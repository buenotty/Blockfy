package com.buenotty.blockfy.feature_accessibility

import android.content.Context
import com.buenotty.blockfy.R
import kotlin.random.Random

object MindfulnessProvocationEngine {

    fun getRandomReelsQuote(context: Context): String {
        val quotes = context.resources.getStringArray(R.array.mindfulness_reels_quotes)
        return quotes[Random.nextInt(quotes.size)]
    }
}
