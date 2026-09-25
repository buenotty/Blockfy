package com.buenotty.blockfy.feature_preferences.repository.models

import kotlinx.serialization.Serializable

@Serializable
data class App(
    val name: String,
    val blocked: Boolean,
    val blockedStart: Int,
    val blockedEnd: Int,
    val blockedTimer: Int,
    val features: List<Feature>,
    val dailyLimitMinutes: Int = 0,
    val appTotalDailyLimitMinutes: Int = 0,
    /** Bits 0–6 are Sunday–Saturday. 127 means every day. */
    val blockedWeekdays: Int = 127
)
