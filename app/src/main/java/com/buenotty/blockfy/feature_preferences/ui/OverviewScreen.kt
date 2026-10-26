package com.buenotty.blockfy.feature_preferences.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.buenotty.blockfy.R
import com.buenotty.blockfy.feature_preferences.OverviewViewModel
import com.buenotty.blockfy.feature_preferences.repository.models.App
import com.buenotty.blockfy.feature_preferences.ui.composables.AppBlockCard
import com.buenotty.blockfy.feature_preferences.ui.composables.DailyMotivationCard
import com.buenotty.blockfy.feature_preferences.ui.composables.DisableAdultContentDialog
import com.buenotty.blockfy.feature_preferences.ui.composables.DisableBlockerDialog
import com.buenotty.blockfy.feature_preferences.ui.composables.ScheduleSummaryCard
import com.buenotty.blockfy.feature_preferences.ui.composables.SectionTitle
import com.buenotty.blockfy.feature_preferences.ui.composables.StreakHero
import com.buenotty.blockfy.feature_preferences.ui.composables.StrictModeDialog
import com.buenotty.blockfy.feature_preferences.ui.composables.TodaySummaryCard
import com.buenotty.blockfy.feature_preferences.ui.composables.ToggleCard
import com.buenotty.blockfy.feature_preferences.ui.composables.daysLabel
import com.buenotty.blockfy.feature_preferences.ui.composables.hoursLabel
import org.koin.androidx.compose.koinViewModel

@Composable
fun BlocksScreen(
    onEditApp: (String) -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenConcepts: () -> Unit,
    viewModel: OverviewViewModel = koinViewModel()
) {
    val settings by viewModel.appSettings.collectAsState()
    val usage by viewModel.dailyUsage.collectAsState()
    val streak by viewModel.streak.collectAsState()

    var showStrictDialog by remember { mutableStateOf(false) }
    var showDisableAdultDialog by remember { mutableStateOf(false) }
    var appPendingDisable by remember { mutableStateOf<String?>(null) }

    val strictLocked = settings.isStrictLocked()
    val motivations = stringArrayResource(R.array.motivation_daily)
    val motivation = remember(motivations) {
        motivations[(java.time.LocalDate.now().toEpochDay() % motivations.size).toInt()]
    }

    fun toggleApp(app: App, wantsOn: Boolean) {
        when {
            wantsOn -> viewModel.setAppBlocked(app.name, true)
            strictLocked -> showStrictDialog = true
            else -> appPendingDisable = app.name
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StreakHero(info = streak, why = settings.myWhy, onWriteWhy = onOpenConcepts)

        DailyMotivationCard(text = motivation)

        TodaySummaryCard(blocks = usage.blockedAttemptsToday, savedSeconds = usage.savedSeconds)

        ScheduleSummaryCard(
            summary = "${daysLabel(settings.scheduleWeekdays)} · ${hoursLabel(settings.scheduleStart, settings.scheduleEnd)}",
            onOpen = onOpenSchedule
        )

        SectionTitle(stringResource(R.string.section_apps))
        listOf(settings.instagram, settings.youtube, settings.tiktok, settings.facebook, settings.x).forEach { app ->
            AppBlockCard(
                app = app,
                usage = usage,
                onToggle = { toggleApp(app, it) },
                onOpen = { onEditApp(app.name) }
            )
        }

        SectionTitle(stringResource(R.string.section_protection))
        ToggleCard(
            icon = Icons.Rounded.Shield,
            title = stringResource(R.string.adult_blocker_title),
            summary = stringResource(R.string.adult_blocker_desc),
            checked = settings.adultContentBlockerEnabled,
            onCheckedChange = { wantsOn ->
                when {
                    wantsOn -> viewModel.setAdultContentBlocker(true)
                    strictLocked -> showStrictDialog = true
                    else -> showDisableAdultDialog = true
                }
            }
        )

        SectionTitle(stringResource(R.string.section_focus))
        ToggleCard(
            icon = Icons.Rounded.Lock,
            title = stringResource(R.string.strict_mode_title),
            summary = stringResource(R.string.strict_mode_desc),
            checked = strictLocked,
            onCheckedChange = { wantsOn ->
                when {
                    wantsOn -> viewModel.setStrictMode(true, "MIDNIGHT")
                    strictLocked -> showStrictDialog = true
                    else -> viewModel.setStrictMode(false)
                }
            }
        )

        Spacer(Modifier.height(8.dp))
    }

    if (showStrictDialog) {
        StrictModeDialog(onDismiss = { showStrictDialog = false })
    }
    appPendingDisable?.let { name ->
        DisableBlockerDialog(
            streakDays = streak.current,
            why = settings.myWhy,
            onDismissRequest = { appPendingDisable = null },
            onConfirmation = {
                viewModel.setAppBlocked(name, false)
                appPendingDisable = null
            }
        )
    }
    if (showDisableAdultDialog) {
        DisableAdultContentDialog(
            streakDays = streak.current,
            why = settings.myWhy,
            onDismissRequest = { showDisableAdultDialog = false },
            onConfirmDisable = {
                viewModel.setAdultContentBlocker(false)
                showDisableAdultDialog = false
            }
        )
    }
}
