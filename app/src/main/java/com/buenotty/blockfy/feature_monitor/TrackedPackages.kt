package com.buenotty.blockfy.feature_monitor

object TrackedPackages {
    const val INSTAGRAM = "com.instagram.android"
    const val YOUTUBE = "com.google.android.youtube"
    const val TIKTOK = "com.zhiliaoapp.musically"
    const val FACEBOOK = "com.facebook.katana"
    const val X = "com.twitter.android"

    val ALL: Map<String, String> = mapOf(
        INSTAGRAM to "Instagram",
        YOUTUBE to "YouTube",
        TIKTOK to "TikTok",
        FACEBOOK to "Facebook",
        X to "X"
    )

    fun displayName(packageName: String): String? = ALL[packageName]

    fun isTracked(packageName: String): Boolean = ALL.containsKey(packageName)
}
