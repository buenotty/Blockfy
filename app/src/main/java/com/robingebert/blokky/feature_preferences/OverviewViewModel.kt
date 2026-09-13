package com.robingebert.blokky.feature_preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robingebert.blokky.datastore.AppSettings
import com.robingebert.blokky.datastore.DailyUsage
import com.robingebert.blokky.datastore.DataStoreManager
import com.robingebert.blokky.feature_preferences.repository.models.App
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class OverviewViewModel(private val dataStoreManager: DataStoreManager) : ViewModel() {
    val appSettings: StateFlow<AppSettings> =
        dataStoreManager.appSettingsFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = AppSettings()
            )

    val dailyUsage: StateFlow<DailyUsage> =
        dataStoreManager.dailyUsageFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = DailyUsage()
            )

    fun updateInstagram(app: App) {
        viewModelScope.launch {
            dataStoreManager.update(appSettings.value.copy(instagram = app))
        }
    }

    fun updateTikTok(app: App) {
        viewModelScope.launch {
            dataStoreManager.update(appSettings.value.copy(tiktok = app))
        }
    }

    fun updateYoutube(app: App) {
        viewModelScope.launch {
            dataStoreManager.update(appSettings.value.copy(youtube = app))
        }
    }

    fun updateFacebook(app: App) {
        viewModelScope.launch {
            dataStoreManager.update(appSettings.value.copy(facebook = app))
        }
    }

    fun updateX(app: App) {
        viewModelScope.launch {
            dataStoreManager.update(appSettings.value.copy(x = app))
        }
    }

    fun setProvocationMode(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.update(appSettings.value.copy(provocationModeEnabled = enabled))
        }
    }

    fun setStrictMode(enabled: Boolean, type: String = "MIDNIGHT") {
        viewModelScope.launch {
            val lockedUntil = if (enabled && type == "MIDNIGHT") {
                // Calculate next midnight epoch
                val tomorrowMidnight = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000L
                tomorrowMidnight
            } else 0L

            dataStoreManager.update(
                appSettings.value.copy(
                    strictModeEnabled = enabled,
                    strictModeType = type,
                    strictModeLockedUntilEpoch = lockedUntil
                )
            )
        }
    }

    fun resetDailyUsage(appName: String) {
        viewModelScope.launch {
            dataStoreManager.resetUsage(appName)
        }
    }

    fun isStrictLocked(): Boolean {
        val settings = appSettings.value
        if (!settings.strictModeEnabled) return false
        if (settings.strictModeType == "MIDNIGHT") {
            val now = System.currentTimeMillis()
            return now < settings.strictModeLockedUntilEpoch
        }
        return true
    }
}