package com.buenotty.blockfy.feature_preferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buenotty.blockfy.datastore.AppSettings
import com.buenotty.blockfy.datastore.DailyUsage
import com.buenotty.blockfy.datastore.DataStoreManager
import com.buenotty.blockfy.feature_preferences.repository.models.App
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class OverviewViewModel(private val dataStoreManager: DataStoreManager) : ViewModel() {

    val appSettings: StateFlow<AppSettings> =
        dataStoreManager.appSettingsFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = AppSettings()
            )

    /** False until DataStore has delivered the stored settings, so screens do not flash defaults. */
    val isLoaded: StateFlow<Boolean> =
        dataStoreManager.appSettingsFlow
            .map { true }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = false
            )

    val dailyUsage: StateFlow<DailyUsage> =
        dataStoreManager.dailyUsageFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = DailyUsage()
            )

    init {
        viewModelScope.launch {
            dataStoreManager.ensureTodayUsage()
        }
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { dataStoreManager.updateSettings(transform) }
    }

    fun updateApp(app: App) = update { settings ->
        when (app.name) {
            settings.instagram.name -> settings.copy(instagram = app)
            settings.youtube.name -> settings.copy(youtube = app)
            settings.tiktok.name -> settings.copy(tiktok = app)
            settings.facebook.name -> settings.copy(facebook = app)
            settings.x.name -> settings.copy(x = app)
            else -> settings
        }
    }

    fun setAppBlocked(appName: String, blocked: Boolean) = update { settings ->
        fun App.toggled() = if (name == appName) copy(blocked = blocked) else this
        settings.copy(
            instagram = settings.instagram.toggled(),
            youtube = settings.youtube.toggled(),
            tiktok = settings.tiktok.toggled(),
            facebook = settings.facebook.toggled(),
            x = settings.x.toggled()
        )
    }

    fun setAdultContentBlocker(enabled: Boolean) = update { it.copy(adultContentBlockerEnabled = enabled) }

    fun setProvocationMode(enabled: Boolean) = update { it.copy(provocationModeEnabled = enabled) }

    fun setStrictMode(enabled: Boolean, type: String = "MIDNIGHT") = update { settings ->
        val lockedUntil = if (enabled && type == "MIDNIGHT") {
            LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000L
        } else {
            0L
        }
        settings.copy(
            strictModeEnabled = enabled,
            strictModeType = type,
            strictModeLockedUntilEpoch = lockedUntil
        )
    }

    fun setSchedule(start: Int, end: Int, weekdays: Int) = update {
        it.copy(scheduleStart = start, scheduleEnd = end, scheduleWeekdays = weekdays)
    }

    fun setOnboardingDone() = update { it.copy(onboardingDone = true) }

    fun resetDailyUsage(appName: String) {
        viewModelScope.launch { dataStoreManager.resetUsage(appName) }
    }

    fun isStrictLocked(): Boolean = appSettings.value.isStrictLocked()
}
