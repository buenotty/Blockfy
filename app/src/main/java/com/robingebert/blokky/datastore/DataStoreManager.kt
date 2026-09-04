package com.robingebert.blokky.datastore

import android.content.Context
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.first

val Context.appSettingsStore by dataStore("app_settings.json", AppSettingsSerializer)
val Context.featureEnabledStore by dataStore("feature_enabled.json", FeatureEnabledSerializer)
val Context.dailyUsageStore by dataStore("daily_usage.json", DailyUsageSerializer)

class DataStoreManager(private val context: Context) {

    val appSettingsFlow = context.appSettingsStore.data
    val featureEnabledFlow = context.featureEnabledStore.data
    val dailyUsageFlow = context.dailyUsageStore.data

    suspend fun updateAppSettings(settings: AppSettings) {
        context.appSettingsStore.updateData { settings }
    }

    suspend fun update(settings: AppSettings) {
        updateAppSettings(settings)
    }

    private fun getTodayDateString(): String {
        return java.time.LocalDate.now().toString()
    }

    suspend fun getTodayUsage(): DailyUsage {
        val today = getTodayDateString()
        val current = dailyUsageFlow.first()
        return if (current.date == today) {
            current
        } else {
            val resetUsage = DailyUsage(date = today)
            context.dailyUsageStore.updateData { resetUsage }
            resetUsage
        }
    }

    suspend fun addUsage(appName: String, seconds: Long) {
        val today = getTodayDateString()
        context.dailyUsageStore.updateData { current ->
            val base = if (current.date == today) current else DailyUsage(date = today)
            when (appName) {
                "Instagram" -> base.copy(instagramSeconds = base.instagramSeconds + seconds)
                "YouTube" -> base.copy(youtubeSeconds = base.youtubeSeconds + seconds)
                "TikTok" -> base.copy(tiktokSeconds = base.tiktokSeconds + seconds)
                "Facebook" -> base.copy(facebookSeconds = base.facebookSeconds + seconds)
                "X" -> base.copy(xTotalSeconds = base.xTotalSeconds + seconds)
                else -> base
            }
        }
    }

    suspend fun addTotalAppUsage(appName: String, seconds: Long) {
        val today = getTodayDateString()
        context.dailyUsageStore.updateData { current ->
            val base = if (current.date == today) current else DailyUsage(date = today)
            when (appName) {
                "Instagram" -> base.copy(instagramTotalSeconds = base.instagramTotalSeconds + seconds)
                "YouTube" -> base.copy(youtubeTotalSeconds = base.youtubeTotalSeconds + seconds)
                "TikTok" -> base.copy(tiktokTotalSeconds = base.tiktokTotalSeconds + seconds)
                "Facebook" -> base.copy(facebookTotalSeconds = base.facebookTotalSeconds + seconds)
                "X" -> base.copy(xTotalSeconds = base.xTotalSeconds + seconds)
                else -> base
            }
        }
    }

    suspend fun recordBlockedDistraction(estimatedSecondsSaved: Long = 300L) {
        val today = getTodayDateString()
        context.dailyUsageStore.updateData { current ->
            val base = if (current.date == today) current else DailyUsage(date = today)
            base.copy(
                blockedAttemptsToday = base.blockedAttemptsToday + 1,
                savedSeconds = base.savedSeconds + estimatedSecondsSaved
            )
        }
    }

    suspend fun resetUsage(appName: String) {
        val today = getTodayDateString()
        context.dailyUsageStore.updateData { current ->
            val base = if (current.date == today) current else DailyUsage(date = today)
            when (appName) {
                "Instagram" -> base.copy(instagramSeconds = 0L, instagramTotalSeconds = 0L)
                "YouTube" -> base.copy(youtubeSeconds = 0L, youtubeTotalSeconds = 0L)
                "TikTok" -> base.copy(tiktokSeconds = 0L, tiktokTotalSeconds = 0L)
                "Facebook" -> base.copy(facebookSeconds = 0L, facebookTotalSeconds = 0L)
                "X" -> base.copy(xTotalSeconds = 0L)
                else -> base
            }
        }
    }

    suspend fun updateFeatureStatus(appName: String, featureName: String?, enabled: Boolean) {
        context.featureEnabledStore.updateData { prefs ->
            val updatedStatuses = prefs.statuses.filterNot {
                it.appName == appName && it.featureName == featureName
            } + FeatureEnabledStatus(appName, featureName, enabled)

            prefs.copy(statuses = updatedStatuses)
        }
    }

    suspend fun getFeatureEnabledStatus(appName: String, featureName: String?): Boolean {
        return featureEnabledFlow.first().statuses
            .firstOrNull { it.appName == appName && it.featureName == featureName }
            ?.enabled ?: false
    }
}
