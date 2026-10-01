package com.buenotty.blockfy.datastore

import kotlinx.serialization.Serializable

@Serializable
data class DailyUsage(
    val date: String = "",
    val instagramSeconds: Long = 0L,
    val instagramTotalSeconds: Long = 0L,
    val youtubeSeconds: Long = 0L,
    val youtubeTotalSeconds: Long = 0L,
    val tiktokSeconds: Long = 0L,
    val tiktokTotalSeconds: Long = 0L,
    val xTotalSeconds: Long = 0L,
    val facebookSeconds: Long = 0L,
    val facebookTotalSeconds: Long = 0L,
    val savedSeconds: Long = 0L,
    val blockedAttemptsToday: Int = 0
) {
    /** Adds time spent anywhere in the app. */
    fun plusTotal(appName: String, seconds: Long): DailyUsage = when (appName) {
        "Instagram" -> copy(instagramTotalSeconds = instagramTotalSeconds + seconds)
        "YouTube" -> copy(youtubeTotalSeconds = youtubeTotalSeconds + seconds)
        "TikTok" -> copy(tiktokTotalSeconds = tiktokTotalSeconds + seconds)
        "Facebook" -> copy(facebookTotalSeconds = facebookTotalSeconds + seconds)
        "X" -> copy(xTotalSeconds = xTotalSeconds + seconds)
        else -> this
    }

    /** Adds time spent on Reels/Shorts only. X has no such screen, so it is never counted twice. */
    fun plusShorts(appName: String, seconds: Long): DailyUsage = when (appName) {
        "Instagram" -> copy(instagramSeconds = instagramSeconds + seconds)
        "YouTube" -> copy(youtubeSeconds = youtubeSeconds + seconds)
        "TikTok" -> copy(tiktokSeconds = tiktokSeconds + seconds)
        "Facebook" -> copy(facebookSeconds = facebookSeconds + seconds)
        else -> this
    }

    fun resetApp(appName: String): DailyUsage = when (appName) {
        "Instagram" -> copy(instagramSeconds = 0L, instagramTotalSeconds = 0L)
        "YouTube" -> copy(youtubeSeconds = 0L, youtubeTotalSeconds = 0L)
        "TikTok" -> copy(tiktokSeconds = 0L, tiktokTotalSeconds = 0L)
        "Facebook" -> copy(facebookSeconds = 0L, facebookTotalSeconds = 0L)
        "X" -> copy(xTotalSeconds = 0L)
        else -> this
    }
}
