package com.buenotty.blockfy.feature_preferences.repository.models

import kotlinx.serialization.Serializable

/**
 * One app has a master switch and up to two independent rules:
 *
 *  - the whole-app rule ([wholeApp]), which limits or blocks all use of the app;
 *  - the Reels/Shorts rule ([shortsRuleOn]), which limits or blocks only short videos.
 *
 * Each rule is either "block it" (limit 0) or "allow N minutes a day". Hours and weekdays are
 * global and live in AppSettings. TikTok and X have no short-video screen, so for them the
 * whole-app rule is always the rule.
 */
@Serializable
data class App(
    val name: String,
    /** Master switch. When false the app is never blocked, whatever the rules say. */
    val blocked: Boolean,
    /** Unused: hours moved to AppSettings. Kept so older saved settings still load. */
    val blockedStart: Int,
    val blockedEnd: Int,
    val blockedTimer: Int,
    val features: List<Feature>,
    /** Daily minutes allowed for Reels/Shorts. 0 means blocked. */
    val dailyLimitMinutes: Int = 0,
    /** Daily minutes allowed for the whole app. 0 means blocked. */
    val appTotalDailyLimitMinutes: Int = 0,
    /** Unused: weekdays moved to AppSettings. */
    val blockedWeekdays: Int = 127,
    /** The whole-app rule is on. TikTok and X always behave as if it were. */
    val wholeApp: Boolean = false,
    /** The Reels/Shorts rule is on. Ignored for TikTok and X. */
    val shortsRuleOn: Boolean = true
)
