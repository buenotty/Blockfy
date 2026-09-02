package com.robingebert.blokky.datastore

import kotlinx.serialization.Serializable

@Serializable
data class DailyUsage(
    val date: String = "",
    val instagramSeconds: Long = 0L,
    val youtubeSeconds: Long = 0L,
    val tiktokSeconds: Long = 0L
)
