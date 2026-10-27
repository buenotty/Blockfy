package com.buenotty.blockfy.feature_monitor

object TrackedPackages {
    const val INSTAGRAM = "com.instagram.android"
    const val YOUTUBE = "com.google.android.youtube"
    const val TIKTOK = "com.zhiliaoapp.musically"
    const val FACEBOOK = "com.facebook.katana"
    const val X = "com.twitter.android"

    /** Official Telegram, its direct-download build, Telegram X and the Plus fork. */
    val TELEGRAM = listOf(
        "org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram", "org.telegram.plus"
    )

    val ALL: Map<String, String> = mapOf(
        INSTAGRAM to "Instagram",
        YOUTUBE to "YouTube",
        TIKTOK to "TikTok",
        FACEBOOK to "Facebook",
        X to "X"
    ) + TELEGRAM.associateWith { "Telegram" }

    fun displayName(packageName: String): String? = ALL[packageName]

    fun isTracked(packageName: String): Boolean = ALL.containsKey(packageName)
}
