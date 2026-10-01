package com.buenotty.blockfy.feature_preferences.repository.models

import kotlinx.serialization.Serializable

@Serializable
data class App(
    val name: String,
    /** Master switch. When false the app is never blocked, whatever the other fields say. */
    val blocked: Boolean,
    val blockedStart: Int,
    val blockedEnd: Int,
    val blockedTimer: Int,
    val features: List<Feature>,
    /** Daily minutes allowed for Reels/Shorts. 0 means blocked for the whole schedule. */
    val dailyLimitMinutes: Int = 0,
    /** Daily minutes allowed for the whole app. 0 means blocked for the whole schedule. */
    val appTotalDailyLimitMinutes: Int = 0,
    /** Bits 0-6 are Sunday-Saturday. 127 means every day. */
    val blockedWeekdays: Int = 127,
    /** Block the whole app instead of only Reels/Shorts. TikTok and X are always whole-app. */
    val wholeApp: Boolean = false
)
