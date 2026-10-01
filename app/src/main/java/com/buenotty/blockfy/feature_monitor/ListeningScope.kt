package com.buenotty.blockfy.feature_monitor

import com.buenotty.blockfy.datastore.AppSettings
import com.buenotty.blockfy.feature_accessibility.AdultContentDetector
import java.util.TimeZone

/**
 * Which apps the accessibility service should receive events from right now. Android only wakes
 * the service for the packages listed here, so an app that has no active rule, or that is outside
 * its schedule, costs no battery at all.
 */
object ListeningScope {

    /** A package name that no real app has, used to listen to nothing. An empty filter would mean "everything". */
    const val NOTHING = "com.buenotty.blockfy.listen.nothing"

    fun packages(
        settings: AppSettings,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Set<String> {
        val result = linkedSetOf<String>()
        if (BlockPolicy.isScheduleActive(settings, nowMillis, timeZone)) {
            for ((pkg, name) in TrackedPackages.ALL) {
                val app = BlockPolicy.appConfig(settings, name)
                if (app.blocked && (BlockPolicy.wholeRuleOn(app) || BlockPolicy.shortsRuleOn(app))) result += pkg
            }
        }
        if (settings.adultContentBlockerEnabled) result += AdultContentDetector.BROWSER_PACKAGES
        return if (result.isEmpty()) setOf(NOTHING) else result
    }
}
