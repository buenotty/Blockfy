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
    val telegramTotalSeconds: Long = 0L,
    val facebookSeconds: Long = 0L,
    val facebookTotalSeconds: Long = 0L,
    val savedSeconds: Long = 0L,
    val blockedAttemptsToday: Int = 0,
    /** The user turned a block off or loosened it today, so today cannot count as a clean day. */
    val loosened: Boolean = false,
    /** Some block or the adult shield was on at some point today. */
    val hadProtection: Boolean = false,
    /** Times each app was opened today (a new session after a real absence). */
    val opens: Map<String, Int> = emptyMap(),
    /** Epoch millis until which each app stays blocked by a session-limit break. */
    val breakUntil: Map<String, Long> = emptyMap(),
    /** Extra seconds the user was granted per app today, usable in the app or in the browser. */
    val bonusApp: Map<String, Long> = emptyMap(),
    /** Extra seconds granted per app that only count for its website in a browser. */
    val bonusBrowser: Map<String, Long> = emptyMap(),
    /** Exceptions ("I need it anyway") granted today. */
    val exceptions: Int = 0
) {
    /** A negotiated exception: [seconds] more for [appName], in the app or only on its website. */
    fun withException(appName: String, seconds: Long, browser: Boolean): DailyUsage =
        if (browser) {
            copy(bonusBrowser = bonusBrowser + (appName to (bonusBrowser[appName] ?: 0L) + seconds), exceptions = exceptions + 1)
        } else {
            copy(bonusApp = bonusApp + (appName to (bonusApp[appName] ?: 0L) + seconds), exceptions = exceptions + 1)
        }

    /** Seconds granted on top of the limit for [appName]; the browser-only share counts only for [viaBrowser]. */
    fun bonusSeconds(appName: String, viaBrowser: Boolean): Long =
        (bonusApp[appName] ?: 0L) + if (viaBrowser) (bonusBrowser[appName] ?: 0L) else 0L

    fun plusOpens(appName: String, count: Int): DailyUsage =
        copy(opens = opens + (appName to (opens[appName] ?: 0) + count))

    fun withBreak(appName: String, untilMillis: Long): DailyUsage =
        copy(breakUntil = breakUntil + (appName to untilMillis))

    /** Adds time spent anywhere in the app. */
    fun plusTotal(appName: String, seconds: Long): DailyUsage = when (appName) {
        "Instagram" -> copy(instagramTotalSeconds = instagramTotalSeconds + seconds)
        "YouTube" -> copy(youtubeTotalSeconds = youtubeTotalSeconds + seconds)
        "TikTok" -> copy(tiktokTotalSeconds = tiktokTotalSeconds + seconds)
        "Facebook" -> copy(facebookTotalSeconds = facebookTotalSeconds + seconds)
        "X" -> copy(xTotalSeconds = xTotalSeconds + seconds)
        "Telegram" -> copy(telegramTotalSeconds = telegramTotalSeconds + seconds)
        else -> this
    }

    /** Adds time spent on Reels/Shorts only. X and Telegram have no such screen, so it is never counted twice. */
    fun plusShorts(appName: String, seconds: Long): DailyUsage = when (appName) {
        "Instagram" -> copy(instagramSeconds = instagramSeconds + seconds)
        "YouTube" -> copy(youtubeSeconds = youtubeSeconds + seconds)
        "TikTok" -> copy(tiktokSeconds = tiktokSeconds + seconds)
        "Facebook" -> copy(facebookSeconds = facebookSeconds + seconds)
        else -> this
    }

    fun resetApp(appName: String): DailyUsage =
        resetSeconds(appName).copy(
            opens = opens - appName, breakUntil = breakUntil - appName,
            bonusApp = bonusApp - appName, bonusBrowser = bonusBrowser - appName
        )

    private fun resetSeconds(appName: String): DailyUsage = when (appName) {
        "Instagram" -> copy(instagramSeconds = 0L, instagramTotalSeconds = 0L)
        "YouTube" -> copy(youtubeSeconds = 0L, youtubeTotalSeconds = 0L)
        "TikTok" -> copy(tiktokSeconds = 0L, tiktokTotalSeconds = 0L)
        "Facebook" -> copy(facebookSeconds = 0L, facebookTotalSeconds = 0L)
        "X" -> copy(xTotalSeconds = 0L)
        "Telegram" -> copy(telegramTotalSeconds = 0L)
        else -> this
    }
}
