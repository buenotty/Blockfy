package com.buenotty.blockfy.datastore

import com.buenotty.blockfy.feature_preferences.repository.models.App
import com.buenotty.blockfy.feature_preferences.repository.models.Feature
import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val instagram: App = App(
        name = "Instagram",
        blocked = false,
        blockedStart = 0,
        blockedEnd = 1439,
        blockedTimer = 0,
        features = listOf(
            Feature(
                name = "Reels",
                enabled = true,
                startTime = 0,
                endTime = 1439,
            ),
            Feature(
                name = "Stories",
                enabled = false,
                startTime = 0,
                endTime = 1439,
            ),
            Feature(
                name = "Search",
                enabled = false,
                startTime = 0,
                endTime = 1439,
            ),
            Feature(
                name = "Comments",
                enabled = false,
                startTime = 0,
                endTime = 1439,
            )
        )
    ),
    val youtube: App = App(
        name = "YouTube",
        blocked = false,
        blockedStart = 0,
        blockedEnd = 1439,
        blockedTimer = 0,
        features = listOf(
            Feature(
                name = "Shorts",
                enabled = true,
                startTime = 0,
                endTime = 1439,
            ),
        )
    ),
    val tiktok: App = App(
        name = "TikTok",
        blocked = false,
        blockedStart = 0,
        blockedEnd = 1439,
        blockedTimer = 0,
        features = emptyList()
    ),
    val facebook: App = App(
        name = "Facebook",
        blocked = false,
        blockedStart = 0,
        blockedEnd = 1439,
        blockedTimer = 0,
        features = listOf(
            Feature(
                name = "Reels",
                enabled = true,
                startTime = 0,
                endTime = 1439,
            )
        )
    ),
    val x: App = App(
        name = "X",
        blocked = false,
        blockedStart = 0,
        blockedEnd = 1439,
        blockedTimer = 0,
        features = emptyList()
    ),
    val provocationModeEnabled: Boolean = true,
    val strictModeEnabled: Boolean = false,
    val strictModeType: String = "MIDNIGHT",
    val strictModeLockedUntilEpoch: Long = 0L,
    val adultContentBlockerEnabled: Boolean = false,
    /** Hours and weekdays when the rules are enforced. Shared by every app. */
    val scheduleStart: Int = 0,
    val scheduleEnd: Int = 1439,
    /** Bits 0-6 are Sunday-Saturday. 127 means every day. */
    val scheduleWeekdays: Int = 127,
    /** The first-launch notice about what the app is and is not has been accepted. */
    val onboardingDone: Boolean = false
) {
    /** True while strict mode forbids loosening any block. A midnight lock expires by itself. */
    fun isStrictLocked(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!strictModeEnabled) return false
        return if (strictModeType == "MIDNIGHT") nowMillis < strictModeLockedUntilEpoch else true
    }
}
