package com.buenotty.blockfy.feature_preferences.ui

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.buenotty.blockfy.R
import com.buenotty.blockfy.feature_monitor.BlockPolicy
import com.buenotty.blockfy.feature_preferences.OverviewViewModel
import com.buenotty.blockfy.feature_preferences.repository.models.App
import com.buenotty.blockfy.feature_preferences.ui.composables.AccessibilityServiceDialog
import com.buenotty.blockfy.feature_preferences.ui.composables.AppBlockCard
import com.buenotty.blockfy.feature_preferences.ui.composables.DisableAdultContentDialog
import com.buenotty.blockfy.feature_preferences.ui.composables.DisableBlockerDialog
import com.buenotty.blockfy.feature_preferences.ui.composables.ProtectionStatusCard
import com.buenotty.blockfy.feature_preferences.ui.composables.SectionTitle
import com.buenotty.blockfy.feature_preferences.ui.composables.StrictModeDialog
import com.buenotty.blockfy.feature_preferences.ui.composables.TodaySummaryCard
import com.buenotty.blockfy.feature_preferences.ui.composables.ToggleCard
import com.buenotty.blockfy.feature_preferences.ui.composables.isAccessibilityGranted
import org.koin.androidx.compose.koinViewModel

@Composable
fun BlocksScreen(
    onEditApp: (String) -> Unit,
    viewModel: OverviewViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val settings by viewModel.appSettings.collectAsState()
    val usage by viewModel.dailyUsage.collectAsState()

    var isAccessibilityGranted by remember { mutableStateOf(context.isAccessibilityGranted()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isAccessibilityGranted = context.isAccessibilityGranted()
    }

    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var showStrictDialog by remember { mutableStateOf(false) }
    var showDisableAdultDialog by remember { mutableStateOf(false) }
    var appPendingDisable by remember { mutableStateOf<String?>(null) }

    val strictLocked = settings.isStrictLocked()

    fun toggleApp(app: App, wantsOn: Boolean) {
        when {
            wantsOn && !isAccessibilityGranted -> showAccessibilityDialog = true
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
        ProtectionStatusCard(
            isGranted = isAccessibilityGranted,
            onEnable = { showAccessibilityDialog = true },
            onManage = {
                runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            }
        )

        TodaySummaryCard(blocks = usage.blockedAttemptsToday, savedSeconds = usage.savedSeconds)

        SectionTitle(stringResource(R.string.section_apps))
        listOf(settings.instagram, settings.youtube, settings.tiktok, settings.facebook, settings.x).forEach { app ->
            AppBlockCard(
                app = app,
                usedSeconds = BlockPolicy.usedSeconds(app, usage),
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
                    wantsOn && !isAccessibilityGranted -> showAccessibilityDialog = true
                    wantsOn -> viewModel.setAdultContentBlocker(true)
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

    if (showAccessibilityDialog) {
        AccessibilityServiceDialog(onDismissRequest = { showAccessibilityDialog = false })
    }
    if (showStrictDialog) {
        StrictModeDialog(onDismiss = { showStrictDialog = false })
    }
    appPendingDisable?.let { name ->
        DisableBlockerDialog(
            onDismissRequest = { appPendingDisable = null },
            onConfirmation = {
                viewModel.setAppBlocked(name, false)
                appPendingDisable = null
            }
        )
    }
    if (showDisableAdultDialog) {
        DisableAdultContentDialog(
            onDismissRequest = { showDisableAdultDialog = false },
            onConfirmDisable = {
                viewModel.setAdultContentBlocker(false)
                showDisableAdultDialog = false
            }
        )
    }
}
