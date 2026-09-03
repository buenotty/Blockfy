package com.robingebert.blokky.feature_preferences.ui

import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.robingebert.blokky.R
import com.robingebert.blokky.feature_preferences.OverviewViewModel
import com.robingebert.blokky.feature_preferences.ui.composables.AccessibilityServiceCard
import com.robingebert.blokky.feature_preferences.ui.composables.BlockfyThemedAppIcon
import com.robingebert.blokky.feature_preferences.ui.composables.EditAppBottomSheet
import com.robingebert.blokky.feature_preferences.ui.composables.SwitchPreference
import com.robingebert.blokky.ui.theme.BlockfyPrimary
import com.robingebert.blokky.ui.theme.BlockfySecondary
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

    //region Accessibility Service
    var isAccessibilityGranted by remember { mutableStateOf(context.isAccessibilityGranted()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    LaunchedEffect(lifecycleState) {
        when (lifecycleState) {
            Lifecycle.State.DESTROYED -> {}
            Lifecycle.State.INITIALIZED -> {}
            Lifecycle.State.CREATED -> {}
            Lifecycle.State.STARTED -> {}
            Lifecycle.State.RESUMED -> {
                isAccessibilityGranted = context.isAccessibilityGranted()
            }
        }
    }
    //endregion

    Column(modifier = Modifier.padding(12.dp)) {

        AccessibilityServiceCard(isAccessibilityGranted)
        Spacer(modifier = Modifier.height(14.dp))

        Column(verticalArrangement = spacedBy(10.dp)) {
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
                leadingIcon = {
                    BlockfyThemedAppIcon("Instagram")
                },
                settingsIcon = {
                    IconButton(
                        modifier = it,
                        onClick = {
                            selectedApp = appSettings.instagram
                            showSettingsDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.instagram_reels),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
            ) {
                overviewViewModel.updateInstagram(
                    appSettings.instagram.copy(
                        blocked = it
                    )
                )
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
                    BlockfyThemedAppIcon("YouTube")
                },
                settingsIcon = {
                    IconButton(
                        modifier = it,
                        onClick = {
                            selectedApp = appSettings.youtube
                            showSettingsDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.youtube_shorts),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            ) {
                overviewViewModel.updateYoutube(
                    appSettings.youtube.copy(
                        blocked = it
                    )
                )
            }

            SwitchPreference(
                value = appSettings.tiktok.blocked,
                enabled = isAccessibilityGranted,
                title = stringResource(R.string.tiktok_app),
                summary = stringResource(R.string.block_tiktok_summary),
                leadingIcon = {
                    BlockfyThemedAppIcon("TikTok")
                },
                settingsIcon = {
                    IconButton(
                        modifier = it,
                        onClick = {
                            selectedApp = appSettings.tiktok
                            showSettingsDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.tiktok_app),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            ) {
                overviewViewModel.updateTikTok(
                    appSettings.tiktok.copy(
                        blocked = it
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Support Creator Card on Home Screen
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showSupportDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFF43F5E),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.support_creator_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.support_creator_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showSettingsDialog) {
        val todayUsed = when (selectedApp.name) {
            appSettings.instagram.name -> dailyUsage.instagramSeconds
            appSettings.youtube.name -> dailyUsage.youtubeSeconds
            appSettings.tiktok.name -> dailyUsage.tiktokSeconds
            else -> 0L
        }

        EditAppBottomSheet(
            onDismiss = { showSettingsDialog = false },
            app = selectedApp,
            todayUsedSeconds = todayUsed,
            onResetUsage = { overviewViewModel.resetDailyUsage(selectedApp.name) },
            onSave = {
                when (it.name) {
                    appSettings.instagram.name -> overviewViewModel.updateInstagram(it)
                    appSettings.youtube.name -> overviewViewModel.updateYoutube(it)
                    appSettings.tiktok.name -> overviewViewModel.updateTikTok(it)
                }
                showSettingsDialog = false
            }
        )
    }

    if (showSupportDialog) {
        SupportCreatorDialog(
            onDismiss = { showSupportDialog = false }
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
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.VolunteerActivism,
                    contentDescription = null,
                    tint = BlockfyPrimary,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.support_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.support_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Pix Key Display Box
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.pix_key_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = BlockfyPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        SelectionContainer {
                            Text(
                                text = pixKey,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Copy Pix Button
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlockfyPrimary
                    ),
                    onClick = {
                        clipboardManager.setText(AnnotatedString(pixKey))
                        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.copy_pix_btn), fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Star on GitHub Button
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/buenotty/Blockfy".toUri())
                        context.startActivity(intent)
                    }
                ) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.star_github_btn), fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.btn_close))
                }
            }
        }
    }
}

fun Context.isAccessibilityGranted(): Boolean {
    val am = this.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val runningServices =
        am.getEnabledAccessibilityServiceList(AccessibilityEvent.TYPE_VIEW_CLICKED)
    return runningServices.any { service ->
        service.id.contains("ReelsBlockAccessibilityService") &&
        (service.id.startsWith("${packageName}/") ||
         service.id.startsWith("com.robingebert.blokky/") ||
         service.id.startsWith("com.buenotty.blockfy/"))
    }
}

@Preview
@Composable
fun SettingsLayoutPreview() {
    SettingsScreen()
}