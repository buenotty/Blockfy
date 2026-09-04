package com.robingebert.blokky.datastore

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
)
