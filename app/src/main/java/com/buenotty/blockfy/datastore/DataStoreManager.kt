package com.buenotty.blockfy.datastore

import android.content.Context
import androidx.datastore.dataStore
import com.buenotty.blockfy.feature_monitor.BlockPolicy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.appSettingsStore by dataStore("app_settings.json", AppSettingsSerializer)
val Context.dailyUsageStore by dataStore("daily_usage.json", DailyUsageSerializer)
val Context.historyStore by dataStore("history.json", HistorySerializer)

private const val MAX_HISTORY_DAYS = 90

class DataStoreManager(private val context: Context) {

    val appSettingsFlow = context.appSettingsStore.data
    val historyFlow = context.historyStore.data
    val dailyUsageFlow = context.dailyUsageStore.data.map { current ->
        val today = getTodayDateString()
        if (current.date == today) current else DailyUsage(date = today)
    }

    /**
     * Read-modify-write inside DataStore, so two quick changes can never overwrite each other.
     * Also feeds the clean-day streak: loosening anything marks today as not clean, and having any
     * protection on marks today as protected.
     */
    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        var before: AppSettings? = null
        var after: AppSettings? = null
        context.appSettingsStore.updateData { current ->
            before = current
            transform(current).also { after = it }
        }
        val old = before ?: return
        val new = after ?: return
        if (BlockPolicy.isSettingsLoosening(old, new)) markLoosened()
        if (BlockPolicy.hasAnyProtection(new)) updateToday { it.copy(hadProtection = true) }
    }

    /** Called when the user switches off a protection from outside the app, such as Accessibility. */
    suspend fun markLoosened() = updateToday { it.copy(loosened = true) }

    private fun getTodayDateString(): String {
        return java.time.LocalDate.now().toString()
    }

    suspend fun ensureTodayUsage(): DailyUsage {
        var result = DailyUsage()
        updateToday { it.also { usage -> result = usage } }
        return result
    }

    /** Applies [transform] to today's usage, first archiving yesterday if the day just rolled over. */
    private suspend fun updateToday(transform: (DailyUsage) -> DailyUsage) {
        val today = getTodayDateString()
        var finished: DailyUsage? = null
        context.dailyUsageStore.updateData { current ->
            if (current.date == today) {
                transform(current)
            } else {
                if (current.date.isNotBlank()) finished = current
                transform(DailyUsage(date = today))
            }
        }
        finished?.let { archive(it) }
    }

    private suspend fun archive(day: DailyUsage) {
        val settings = context.appSettingsStore.data.first()
        val protectedDay = day.hadProtection || BlockPolicy.hasAnyProtection(settings)
        val record = DayRecord(
            date = day.date,
            blocks = day.blockedAttemptsToday,
            savedSeconds = day.savedSeconds,
            clean = protectedDay && !day.loosened
        )
        context.historyStore.updateData { history ->
            History((history.days.filterNot { it.date == record.date } + record).sortedBy { it.date }.takeLast(MAX_HISTORY_DAYS))
        }
    }

    suspend fun addUsage(appName: String, seconds: Long) = updateToday { it.plusShorts(appName, seconds) }

    suspend fun addTotalAppUsage(appName: String, seconds: Long) = updateToday { it.plusTotal(appName, seconds) }

    suspend fun recordBlockedDistraction(estimatedSecondsSaved: Long = 300L) = updateToday {
        it.copy(
            blockedAttemptsToday = it.blockedAttemptsToday + 1,
            savedSeconds = it.savedSeconds + estimatedSecondsSaved
        )
    }

    suspend fun resetUsage(appName: String) = updateToday { it.resetApp(appName) }
}
