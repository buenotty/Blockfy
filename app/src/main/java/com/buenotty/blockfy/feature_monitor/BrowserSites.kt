package com.buenotty.blockfy.feature_monitor

/**
 * Which tracked app a web address belongs to, so time on the Instagram or YouTube website counts
 * like time in the app and cannot be used to go around a limit. It only ever sees the browser's
 * address bar text, which the adult shield already reads.
 */
object BrowserSites {
    private val SITES = mapOf(
        "Instagram" to listOf("instagram.com"),
        "YouTube" to listOf("youtube.com", "youtu.be"),
        "Facebook" to listOf("facebook.com", "fb.com", "fb.watch"),
        "X" to listOf("x.com", "twitter.com"),
        "TikTok" to listOf("tiktok.com"),
        "Telegram" to listOf("web.telegram.org", "t.me", "telegram.me")
    )
    private val HOST = Regex("""(?:[a-z][a-z0-9+.-]*://)?(?:[^/@\s]*@)?([a-z0-9][a-z0-9.-]*\.[a-z]{2,})""")

    /** The app for the host at the start of [addressBarText], or null. A lookalike or a host in the path never matches. */
    fun appFor(addressBarText: String?): String? {
        val text = addressBarText?.trim()?.lowercase().orEmpty()
        val host = HOST.find(text)?.takeIf { it.range.first == 0 }?.groupValues?.get(1) ?: return null
        return SITES.entries.firstOrNull { (_, domains) -> domains.any { host == it || host.endsWith(".$it") } }?.key
    }
}
