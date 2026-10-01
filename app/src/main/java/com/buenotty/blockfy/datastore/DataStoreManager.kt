package com.buenotty.blockfy.datastore

import android.content.Context
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.map

val Context.appSettingsStore by dataStore("app_settings.json", AppSettingsSerializer)
val Context.dailyUsageStore by dataStore("daily_usage.json", DailyUsageSerializer)

class DataStoreManager(private val context: Context) {

    val appSettingsFlow = context.appSettingsStore.data
    val dailyUsageFlow = context.dailyUsageStore.data.map { current ->
        val today = getTodayDateString()
        if (current.date == today) current else DailyUsage(date = today)
    }

    /** Read-modify-write inside DataStore, so two quick changes can never overwrite each other. */
    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.appSettingsStore.updateData(transform)
    }

    private fun getTodayDateString(): String {
        return java.time.LocalDate.now().toString()
    }

    suspend fun ensureTodayUsage(): DailyUsage {
        val today = getTodayDateString()
        return context.dailyUsageStore.updateData { current ->
            if (current.date == today) current else DailyUsage(date = today)
        }
    }

    private suspend fun updateToday(transform: (DailyUsage) -> DailyUsage) {
        val today = getTodayDateString()
        context.dailyUsageStore.updateData { current ->
            transform(if (current.date == today) current else DailyUsage(date = today))
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
