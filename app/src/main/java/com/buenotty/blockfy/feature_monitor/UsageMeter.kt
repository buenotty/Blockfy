package com.buenotty.blockfy.feature_monitor

/**
 * Turns "the user is in app X right now" samples into whole seconds of use. The wall clock is the
 * only truth: each credit advances the mark by exactly the seconds it granted, so the
 * sub-second remainder carries to the next sample instead of being dropped.
 */
class UsageMeter {

    val pendingTotal = mutableMapOf<String, Long>()
    val pendingShorts = mutableMapOf<String, Long>()

    private var app: String? = null
    private var markElapsed = 0L

    /** Forget the running session (the user left, the screen went off). Pending seconds stay. */
    fun stop() {
        app = null
        markElapsed = 0L
    }

    fun credit(appName: String, shorts: Boolean, now: Long) {
        if (app != appName) {
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
    }

    /** Hands over everything credited so far and clears it. */
    fun drain(): Pair<Map<String, Long>, Map<String, Long>> {
        val result = pendingTotal.toMap() to pendingShorts.toMap()
        pendingTotal.clear()
        pendingShorts.clear()
        return result
    }

    companion object {
        const val MAX_CREDIT_SECONDS = 5L
    }
}
