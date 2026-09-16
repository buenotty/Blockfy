package com.buenotty.blockfy.feature_preferences.ui

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.buenotty.blockfy.AppLocale
import com.buenotty.blockfy.R
import com.buenotty.blockfy.feature_preferences.OverviewViewModel
import com.buenotty.blockfy.feature_preferences.ui.composables.AccessibilityServiceCard
import com.buenotty.blockfy.feature_preferences.ui.composables.DisableAdultContentDialog
import com.buenotty.blockfy.feature_preferences.ui.composables.EditAppBottomSheet
import com.buenotty.blockfy.feature_preferences.ui.composables.InstagramColoredIcon
import com.buenotty.blockfy.feature_preferences.ui.composables.StrictModeDialog
import com.buenotty.blockfy.feature_preferences.ui.composables.SwitchPreference
import com.buenotty.blockfy.feature_preferences.ui.composables.isAccessibilityGranted
import com.buenotty.blockfy.feature_vpn.AdultBlockVpnService
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(overviewViewModel: OverviewViewModel = koinViewModel()) {
    val context = LocalContext.current
    val appSettings by overviewViewModel.appSettings.collectAsState()
    val dailyUsage by overviewViewModel.dailyUsage.collectAsState()

    var selectedApp by remember { mutableStateOf(appSettings.instagram) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showStrictModeDialog by remember { mutableStateOf(false) }
    var showDisableAdultBlockerDialog by remember { mutableStateOf(false) }
    var isAccessibilityGranted by remember { mutableStateOf(context.isAccessibilityGranted()) }
    val languageTag = remember { mutableStateOf(AppLocale.currentTag(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            overviewViewModel.setAdultContentBlocker(true)
            AdultBlockVpnService.start(context)
        }
    }

    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            isAccessibilityGranted = context.isAccessibilityGranted()
            languageTag.value = AppLocale.currentTag(context)
        }
    }

    Column(
        modifier = Modifier
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        AccessibilityServiceCard(isAccessibilityGranted)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8F)
            ),
            shape = RoundedCornerShape(15.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Language, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.language_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.language_desc),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Row(horizontalArrangement = spacedBy(8.dp)) {
                    FilterChip(
                        selected = languageTag.value == AppLocale.ENGLISH,
                        onClick = {
                            AppLocale.apply(context, AppLocale.ENGLISH)
                            languageTag.value = AppLocale.ENGLISH
                        },
                        label = { Text(stringResource(R.string.language_english)) }
                    )
                    FilterChip(
                        selected = languageTag.value == AppLocale.PORTUGUESE,
                        onClick = {
                            AppLocale.apply(context, AppLocale.PORTUGUESE)
                            languageTag.value = AppLocale.PORTUGUESE
                        },
                        label = { Text(stringResource(R.string.language_portuguese)) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8F)
            ),
            shape = RoundedCornerShape(15.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Psychology, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.provocation_mode_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.provocation_mode_desc), style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = appSettings.provocationModeEnabled,
                        onCheckedChange = { overviewViewModel.setProvocationMode(it) }
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.strict_mode_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.strict_mode_desc), style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = appSettings.strictModeEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                overviewViewModel.setStrictMode(true, "MIDNIGHT")
                            } else if (overviewViewModel.isStrictLocked()) {
                                showStrictModeDialog = true
                            } else {
                                overviewViewModel.setStrictMode(false)
                            }
                        }
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Shield, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.adult_blocker_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.adult_blocker_desc), style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = appSettings.adultContentBlockerEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                val prepareIntent = VpnService.prepare(context)
                                if (prepareIntent != null) {
                                    vpnPrepareLauncher.launch(prepareIntent)
                                } else {
                                    overviewViewModel.setAdultContentBlocker(true)
                                    AdultBlockVpnService.start(context)
                                }
                            } else {
                                showDisableAdultBlockerDialog = true
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = spacedBy(8.dp)) {
            val instaSummary = if (appSettings.instagram.dailyLimitMinutes > 0) {
                stringResource(
                    R.string.daily_limit_format,
                    appSettings.instagram.dailyLimitMinutes,
                    dailyUsage.instagramSeconds / 60
                )
            } else {
                stringResource(R.string.block_instagram_summary)
            }

            SwitchPreference(
                value = appSettings.instagram.blocked,
                enabled = isAccessibilityGranted,
                title = stringResource(R.string.instagram_reels),
                summary = instaSummary,
                leadingIcon = { InstagramColoredIcon() },
                settingsIcon = {
                    IconButton(modifier = it, onClick = {
                        selectedApp = appSettings.instagram
                        showSettingsDialog = true
                    }) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.instagram_reels))
                    }
                }
            ) {
                overviewViewModel.updateInstagram(appSettings.instagram.copy(blocked = it))
            }

            val ytSummary = if (appSettings.youtube.dailyLimitMinutes > 0) {
                stringResource(
                    R.string.daily_limit_format,
                    appSettings.youtube.dailyLimitMinutes,
                    dailyUsage.youtubeSeconds / 60
                )
            } else {
                stringResource(R.string.block_youtube_summary)
            }

            SwitchPreference(
                value = appSettings.youtube.blocked,
                enabled = isAccessibilityGranted,
                title = stringResource(R.string.youtube_shorts),
                summary = ytSummary,
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_youtube), null, tint = Color.Red)
                },
                settingsIcon = {
                    IconButton(modifier = it, onClick = {
                        selectedApp = appSettings.youtube
                        showSettingsDialog = true
                    }) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.youtube_shorts))
                    }
                }
            ) {
                overviewViewModel.updateYoutube(appSettings.youtube.copy(blocked = it))
            }

            SwitchPreference(
                value = appSettings.tiktok.blocked,
                enabled = isAccessibilityGranted,
                title = stringResource(R.string.tiktok_app),
                summary = stringResource(R.string.block_tiktok_summary),
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_tiktok), null, tint = Color.Unspecified)
                },
                settingsIcon = {
                    IconButton(modifier = it, onClick = {
                        selectedApp = appSettings.tiktok
                        showSettingsDialog = true
                    }) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.tiktok_app))
                    }
                }
            ) {
                overviewViewModel.updateTikTok(appSettings.tiktok.copy(blocked = it))
            }

            val fbSummary = if (appSettings.facebook.dailyLimitMinutes > 0) {
                stringResource(
                    R.string.daily_limit_format,
                    appSettings.facebook.dailyLimitMinutes,
                    dailyUsage.facebookSeconds / 60
                )
            } else {
                stringResource(R.string.block_facebook_summary)
            }

            SwitchPreference(
                value = appSettings.facebook.blocked,
                enabled = isAccessibilityGranted,
                title = stringResource(R.string.facebook_reels),
                summary = fbSummary,
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_facebook_themed), null, tint = Color.Unspecified)
                },
                settingsIcon = {
                    IconButton(modifier = it, onClick = {
                        selectedApp = appSettings.facebook
                        showSettingsDialog = true
                    }) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.facebook_reels))
                    }
                }
            ) {
                overviewViewModel.updateFacebook(appSettings.facebook.copy(blocked = it))
            }

            val xSummary = if (appSettings.x.appTotalDailyLimitMinutes > 0) {
                stringResource(
                    R.string.daily_usage_short,
                    dailyUsage.xTotalSeconds / 60,
                    appSettings.x.appTotalDailyLimitMinutes
                )
            } else {
                stringResource(R.string.block_x_summary)
            }

            SwitchPreference(
                value = appSettings.x.blocked,
                enabled = isAccessibilityGranted,
                title = stringResource(R.string.x_app),
                summary = xSummary,
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_x_themed), null, tint = Color.Unspecified)
                },
                settingsIcon = {
                    IconButton(modifier = it, onClick = {
                        selectedApp = appSettings.x
                        showSettingsDialog = true
                    }) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.x_app))
                    }
                }
            ) {
                overviewViewModel.updateX(appSettings.x.copy(blocked = it))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8F)
                ),
                shape = RoundedCornerShape(15.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSupportDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.VolunteerActivism, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.support_creator_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.support_creator_subtitle), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        val todayUsed = when (selectedApp.name) {
            appSettings.instagram.name -> dailyUsage.instagramSeconds
            appSettings.youtube.name -> dailyUsage.youtubeSeconds
            appSettings.tiktok.name -> dailyUsage.tiktokSeconds
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

    if (showSupportDialog) {
        SupportCreatorDialog(onDismiss = { showSupportDialog = false })
    }

    if (showDisableAdultBlockerDialog) {
        DisableAdultContentDialog(
            onDismissRequest = { showDisableAdultBlockerDialog = false },
            onConfirmDisable = {
                overviewViewModel.setAdultContentBlocker(false)
                AdultBlockVpnService.stop(context)
                showDisableAdultBlockerDialog = false
            }
        )
    }
}

@Composable
fun SupportCreatorDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val pixKey = "496f008e-c67d-4175-9fad-e6b3c9bbd248"
    val toastMessage = stringResource(R.string.pix_copied_toast)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Rounded.VolunteerActivism, contentDescription = null, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text(stringResource(R.string.support_dialog_title), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.support_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(stringResource(R.string.pix_key_label), style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        SelectionContainer {
                            Text(pixKey, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        clipboardManager.setText(AnnotatedString(pixKey))
                        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.copy_pix_btn))
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, "https://github.com/buenotty/Blockfy".toUri())
                        )
                    }
                ) {
                    Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.star_github_btn))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.btn_close))
                }
            }
        }
    }
}

@Preview
@Composable
fun SettingsLayoutPreview() {
    SettingsScreen()
}
