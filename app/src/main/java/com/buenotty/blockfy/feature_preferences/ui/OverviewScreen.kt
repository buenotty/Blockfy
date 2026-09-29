package com.buenotty.blockfy.feature_preferences.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.buenotty.blockfy.R
import com.buenotty.blockfy.feature_preferences.OverviewViewModel
import com.buenotty.blockfy.feature_preferences.ui.composables.AccessibilityServiceCard
import com.buenotty.blockfy.feature_preferences.ui.composables.AppBrandIcon
import com.buenotty.blockfy.feature_preferences.ui.composables.DisableAdultContentDialog
import com.buenotty.blockfy.feature_preferences.ui.composables.EditAppBottomSheet
import com.buenotty.blockfy.feature_preferences.ui.composables.PreferenceGroup
import com.buenotty.blockfy.feature_preferences.ui.composables.StrictModeDialog
import com.buenotty.blockfy.feature_preferences.ui.composables.SwitchPreference
import com.buenotty.blockfy.feature_preferences.ui.composables.TintedGlyph
import com.buenotty.blockfy.feature_preferences.ui.composables.TrackedAppIcon
import com.buenotty.blockfy.feature_preferences.ui.composables.isAccessibilityGranted
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(overviewViewModel: OverviewViewModel = koinViewModel()) {
    val context = LocalContext.current
    val appSettings by overviewViewModel.appSettings.collectAsState()
    val dailyUsage by overviewViewModel.dailyUsage.collectAsState()

    var selectedApp by remember { mutableStateOf(appSettings.instagram) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showStrictModeDialog by remember { mutableStateOf(false) }
    var showDisableAdultBlockerDialog by remember { mutableStateOf(false) }
    var isAccessibilityGranted by remember { mutableStateOf(context.isAccessibilityGranted()) }
    var homeTab by remember { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            isAccessibilityGranted = context.isAccessibilityGranted()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = homeTab) {
            Tab(
                selected = homeTab == 0,
                onClick = { homeTab = 0 },
                text = { Text(stringResource(R.string.tab_blocks)) }
            )
            Tab(
                selected = homeTab == 1,
                onClick = { homeTab = 1 },
                text = { Text(stringResource(R.string.tab_concepts)) }
            )
        }
        if (homeTab == 1) {
            ConceptsScreen()
            return@Column
        }
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = spacedBy(20.dp)
    ) {
        if (!isAccessibilityGranted) {
            AccessibilityServiceCard(isAccessibilityGranted)
        }

        PreferenceGroup(title = stringResource(R.string.section_apps)) {
            BlockedAppPreference(
                title = stringResource(R.string.instagram_reels),
                summary = dailyLimitSummary(
                    appSettings.instagram.dailyLimitMinutes,
                    dailyUsage.instagramSeconds,
                    stringResource(R.string.block_instagram_summary)
                ),
                icon = TrackedAppIcon.Instagram,
                value = appSettings.instagram.blocked,
                enabled = isAccessibilityGranted,
                showDivider = true,
                onOpenSettings = {
                    selectedApp = appSettings.instagram
                    showSettingsDialog = true
                }
            ) { overviewViewModel.updateInstagram(appSettings.instagram.copy(blocked = it)) }

            BlockedAppPreference(
                title = stringResource(R.string.youtube_shorts),
                summary = dailyLimitSummary(
                    appSettings.youtube.dailyLimitMinutes,
                    dailyUsage.youtubeSeconds,
                    stringResource(R.string.block_youtube_summary)
                ),
                icon = TrackedAppIcon.YouTube,
                value = appSettings.youtube.blocked,
                enabled = isAccessibilityGranted,
                showDivider = true,
                onOpenSettings = {
                    selectedApp = appSettings.youtube
                    showSettingsDialog = true
                }
            ) { overviewViewModel.updateYoutube(appSettings.youtube.copy(blocked = it)) }

            BlockedAppPreference(
                title = stringResource(R.string.tiktok_app),
                summary = stringResource(R.string.block_tiktok_summary),
                icon = TrackedAppIcon.TikTok,
                value = appSettings.tiktok.blocked,
                enabled = isAccessibilityGranted,
                showDivider = true,
                onOpenSettings = {
                    selectedApp = appSettings.tiktok
                    showSettingsDialog = true
                }
            ) { overviewViewModel.updateTikTok(appSettings.tiktok.copy(blocked = it)) }

            BlockedAppPreference(
                title = stringResource(R.string.facebook_reels),
                summary = dailyLimitSummary(
                    appSettings.facebook.dailyLimitMinutes,
                    dailyUsage.facebookSeconds,
                    stringResource(R.string.block_facebook_summary)
                ),
                icon = TrackedAppIcon.Facebook,
                value = appSettings.facebook.blocked,
                enabled = isAccessibilityGranted,
                showDivider = true,
                onOpenSettings = {
                    selectedApp = appSettings.facebook
                    showSettingsDialog = true
                }
            ) { overviewViewModel.updateFacebook(appSettings.facebook.copy(blocked = it)) }

            BlockedAppPreference(
                title = stringResource(R.string.x_app),
                summary = if (appSettings.x.appTotalDailyLimitMinutes > 0) {
                    stringResource(
                        R.string.daily_usage_short,
                        dailyUsage.xTotalSeconds / 60,
                        appSettings.x.appTotalDailyLimitMinutes
                    )
                } else {
                    stringResource(R.string.block_x_summary)
                },
                icon = TrackedAppIcon.X,
                value = appSettings.x.blocked,
                enabled = isAccessibilityGranted,
                showDivider = false,
                onOpenSettings = {
                    selectedApp = appSettings.x
                    showSettingsDialog = true
                }
            ) { overviewViewModel.updateX(appSettings.x.copy(blocked = it)) }
        }

        PreferenceGroup(title = stringResource(R.string.adult_blocker_title)) {
            SwitchPreference(
                value = appSettings.adultContentBlockerEnabled,
                title = stringResource(R.string.adult_blocker_title),
                summary = stringResource(R.string.adult_blocker_desc),
                grouped = true,
                confirmDisable = false,
                leadingIcon = { TintedGlyph(Icons.Rounded.Shield) }
            ) { enabled ->
                if (enabled) {
                    overviewViewModel.setAdultContentBlocker(true)
                } else {
                    showDisableAdultBlockerDialog = true
                }
            }
        }

        PreferenceGroup(title = stringResource(R.string.section_focus)) {
            SwitchPreference(
                value = appSettings.strictModeEnabled,
                title = stringResource(R.string.strict_mode_title),
                summary = stringResource(R.string.strict_mode_desc),
                grouped = true,
                showDivider = false,
                confirmDisable = false,
                leadingIcon = { TintedGlyph(Icons.Rounded.Lock) }
            ) { enabled ->
                if (enabled) {
                    overviewViewModel.setStrictMode(true, "MIDNIGHT")
                } else if (overviewViewModel.isStrictLocked()) {
                    showStrictModeDialog = true
                } else {
                    overviewViewModel.setStrictMode(false)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
    }

    if (showSettingsDialog) {
        val todayUsed = when (selectedApp.name) {
            appSettings.instagram.name -> dailyUsage.instagramSeconds
            appSettings.youtube.name -> dailyUsage.youtubeSeconds
            appSettings.tiktok.name -> dailyUsage.tiktokTotalSeconds
            appSettings.facebook.name -> dailyUsage.facebookSeconds
            appSettings.x.name -> dailyUsage.xTotalSeconds
            else -> 0L
        }

        EditAppBottomSheet(
            onDismiss = { showSettingsDialog = false },
            app = selectedApp,
            todayUsedSeconds = todayUsed,
            isStrictLocked = overviewViewModel.isStrictLocked(),
            onResetUsage = { overviewViewModel.resetDailyUsage(selectedApp.name) },
            onSave = {
                when (it.name) {
                    appSettings.instagram.name -> overviewViewModel.updateInstagram(it)
                    appSettings.youtube.name -> overviewViewModel.updateYoutube(it)
                    appSettings.tiktok.name -> overviewViewModel.updateTikTok(it)
                    appSettings.facebook.name -> overviewViewModel.updateFacebook(it)
                    appSettings.x.name -> overviewViewModel.updateX(it)
                }
                showSettingsDialog = false
            }
        )
    }

    if (showStrictModeDialog) {
        StrictModeDialog(
            isMidnightLock = appSettings.strictModeType == "MIDNIGHT",
            onDismiss = { showStrictModeDialog = false },
            onUnlockSuccess = {
                overviewViewModel.setStrictMode(false)
                showStrictModeDialog = false
            }
        )
    }

    if (showDisableAdultBlockerDialog) {
        DisableAdultContentDialog(
            onDismissRequest = { showDisableAdultBlockerDialog = false },
            onConfirmDisable = {
                overviewViewModel.setAdultContentBlocker(false)
                showDisableAdultBlockerDialog = false
            }
        )
    }
}

@Composable
private fun dailyLimitSummary(limitMinutes: Int, usedSeconds: Long, fallback: String): String {
    return if (limitMinutes > 0) {
        stringResource(R.string.daily_limit_format, limitMinutes, usedSeconds / 60)
    } else {
        fallback
    }
}

@Composable
private fun BlockedAppPreference(
    title: String,
    summary: String,
    icon: TrackedAppIcon,
    value: Boolean,
    enabled: Boolean,
    showDivider: Boolean,
    onOpenSettings: () -> Unit,
    onValueChange: (Boolean) -> Unit,
) {
    SwitchPreference(
        value = value,
        enabled = enabled,
        title = title,
        summary = summary,
        grouped = true,
        showDivider = showDivider,
        leadingIcon = { AppBrandIcon(icon) },
        onRowClick = onOpenSettings,
        settingsIcon = { modifier ->
            IconButton(modifier = modifier, onClick = onOpenSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = title)
            }
        },
        onValueChange = onValueChange
    )
}

@Preview
@Composable
fun SettingsLayoutPreview() {
    SettingsScreen()
}
