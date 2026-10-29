package com.buenotty.blockfy.feature_monitor

/** What the meter handed over in one [UsageMeter.drain]: seconds in the app, seconds in Reels/Shorts, new opens. */
data class Drained(
    val total: Map<String, Long>,
    val shorts: Map<String, Long>,
    val opens: Map<String, Int>
)

/**
 * Turns "the user is in app X right now" samples into whole seconds of use. The wall clock is the
 * only truth: each credit advances the mark by exactly the seconds it granted, so the
 * sub-second remainder carries to the next sample instead of being dropped.
 *
 * It also tells sessions apart: leaving an app for less than [SESSION_GAP_MILLIS] and coming back
 * is the same session (so a quick glance elsewhere cannot dodge a session limit); a longer
 * absence is a new session and counts as one more open.
 */
class UsageMeter {

    val pendingTotal = mutableMapOf<String, Long>()
    val pendingShorts = mutableMapOf<String, Long>()
    val pendingOpens = mutableMapOf<String, Int>()

    private var app: String? = null
    private var markElapsed = 0L

    private var sessionApp: String? = null
    private var sessionSecs = 0L
    private var leftAt = 0L

    /** Seconds of the running session of [appName], or 0 when that app has none. */
    fun sessionSeconds(appName: String): Long = if (sessionApp == appName) sessionSecs else 0L

    /** Starts the session over, for example once the break after a session limit begins. */
    fun resetSession() {
        sessionSecs = 0L
    }

    /** Forget the running clock (the user left, the screen went off). Pending seconds stay. */
    fun stop(now: Long) {
        if (app != null) leftAt = now
        app = null
        markElapsed = 0L
    }

    fun credit(appName: String, shorts: Boolean, now: Long) {
        if (app != appName) {
            val sameSession = sessionApp == appName && now - leftAt <= SESSION_GAP_MILLIS
            if (!sameSession) {
                sessionApp = appName
                sessionSecs = 0L
                pendingOpens.merge(appName, 1, Int::plus)
            }
            app = appName
            markElapsed = now
            return
        }
        val seconds = (now - markElapsed) / 1000L
        if (seconds <= 0L) return
        // A gap longer than the cap means we were not watching (sleep, killed service): do not
        // invent time, just restart the clock.
        markElapsed = if (seconds > MAX_CREDIT_SECONDS) now else markElapsed + seconds * 1000L
        val credited = seconds.coerceAtMost(MAX_CREDIT_SECONDS)
        pendingTotal.merge(appName, credited, Long::plus)
        if (shorts) pendingShorts.merge(appName, credited, Long::plus)
        sessionSecs += credited
    }

    /** Hands over everything credited so far and clears it. */
    fun drain(): Drained {
        val result = Drained(pendingTotal.toMap(), pendingShorts.toMap(), pendingOpens.toMap())
        pendingTotal.clear()
        pendingShorts.clear()
        pendingOpens.clear()
        return result
    }

    companion object {
        const val MAX_CREDIT_SECONDS = 5L
        const val SESSION_GAP_MILLIS = 60_000L
    }
}
