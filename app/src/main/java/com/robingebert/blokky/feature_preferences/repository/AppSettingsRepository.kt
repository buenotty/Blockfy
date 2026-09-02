package com.robingebert.blokky.feature_preferences.repository

import android.content.Context
import com.robingebert.blokky.datastore.AppSettings
import com.robingebert.blokky.datastore.DataStoreManager
import com.robingebert.blokky.feature_preferences.repository.models.App
import com.robingebert.blokky.feature_preferences.repository.models.Feature
import com.robingebert.blokky.worker.FeatureToggleWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SettingsRepository(
    private val dataStore: DataStoreManager,
    private val context: Context
) {

    val appSettings: Flow<AppSettings>
        get() = dataStore.appSettingsFlow

    suspend fun updateApp(
        appName: String,
        update: (old: App) -> App
    ) {
        val current = dataStore.appSettingsFlow.first()
        val modified = when (appName) {
            "Instagram" -> current.copy(instagram = update(current.instagram))
            "YouTube"   -> current.copy(youtube   = update(current.youtube))
            "TikTok"    -> current.copy(tiktok    = update(current.tiktok))
            else        -> current
        }
        dataStore.update(modified)
    }

    suspend fun updateFeature(
        appName: String,
        featureName: String,
        update: (old: Feature) -> Feature
    ) {
        val current = dataStore.appSettingsFlow.first()
        val newApps = when (appName) {
            "Instagram" -> {
                val updatedFeatures = current.instagram.features.map { feature ->
                    if (feature.name == featureName) update(feature) else feature
                }
                current.copy(instagram = current.instagram.copy(features = updatedFeatures))
            }
            "YouTube" -> {
                val updatedFeatures = current.youtube.features.map { feature ->
                    if (feature.name == featureName) update(feature) else feature
                }
                current.copy(youtube = current.youtube.copy(features = updatedFeatures))
            }
            else -> current
        }
        dataStore.update(newApps)
    }

    fun applyTemporaryException(
        appName: String,
        featureName: String,
        durationMinutes: Int
    ) {
        FeatureToggleWorker.scheduleTemporaryException(
            context,
            appName,
            featureName,
            durationMinutes
        )
    }
}
