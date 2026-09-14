package com.robingebert.blokky.feature_preferences.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
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
import androidx.compose.ui.unit.sp
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Shield
import com.robingebert.blokky.feature_preferences.ui.composables.DisableAdultContentDialog
import com.robingebert.blokky.feature_preferences.ui.composables.SavedTimeDashboardCard
import com.robingebert.blokky.feature_preferences.ui.composables.StrictModeDialog
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
    var showStrictModeDialog by remember { mutableStateOf(false) }
    var showDisableAdultBlockerDialog by remember { mutableStateOf(false) }

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

    Column(
        modifier = Modifier
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {

        // 1. Dashboard de Tempo Salvo e Economia de Vida
        SavedTimeDashboardCard(dailyUsage = dailyUsage)
        Spacer(modifier = Modifier.height(14.dp))

        // 2. Card de Serviço de Acessibilidade
        AccessibilityServiceCard(isAccessibilityGranted)
        Spacer(modifier = Modifier.height(14.dp))

        // 3. Toggles de Mentalidade e Modo Inviolável
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = spacedBy(10.dp)) {
                // Modo Choque de Consciência
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Rounded.Psychology,
                            contentDescription = null,
                            tint = Color(0xFFC084FC),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                stringResource(R.string.provocation_mode_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                stringResource(R.string.provocation_mode_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    androidx.compose.material3.Switch(
                        checked = appSettings.provocationModeEnabled,
                        onCheckedChange = { overviewViewModel.setProvocationMode(it) },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BlockfyPrimary
                        )
                    )
                }

                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Modo Inviolável (Hardcore)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                stringResource(R.string.strict_mode_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                stringResource(R.string.strict_mode_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    androidx.compose.material3.Switch(
                        checked = appSettings.strictModeEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                overviewViewModel.setStrictMode(true, "MIDNIGHT")
                            } else {
                                if (overviewViewModel.isStrictLocked()) {
                                    showStrictModeDialog = true
                                } else {
                                    overviewViewModel.setStrictMode(false)
                                }
                            }
                        },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFF59E0B)
                        )
                    )
                }

                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Escudo Anti-Pornô & Conteúdo Adulto
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                stringResource(R.string.adult_blocker_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                stringResource(R.string.adult_blocker_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    androidx.compose.material3.Switch(
                        checked = appSettings.adultContentBlockerEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                overviewViewModel.setAdultContentBlocker(true)
                            } else {
                                showDisableAdultBlockerDialog = true
                            }
                        },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFEF4444)
                        )
                    )
                }

                if (appSettings.adultContentBlockerEnabled) {
                    Spacer(Modifier.height(8.dp))
                    val isPixPaused = appSettings.pixPauseUntilEpoch > System.currentTimeMillis()
                    val remainingMinutes = if (isPixPaused) {
                        ((appSettings.pixPauseUntilEpoch - System.currentTimeMillis()) / 60000L + 1).coerceAtLeast(1L)
                    } else 0L

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isPixPaused) Color(0xFF1E293B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (isPixPaused) Icons.Rounded.Timer else Icons.Rounded.AccountBalance,
                                        contentDescription = null,
                                        tint = if (isPixPaused) Color(0xFF38BDF8) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (isPixPaused) "Modo Seguro Pix Ativo (${remainingMinutes}m)" else "Compatibilidade com Área Pix (Nubank)",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPixPaused) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isPixPaused)
                                                "Navegadores pausados para você realizar transferências no Nubank/bancos."
                                            else
                                                "Pausa a leitura de navegadores por 10 min para liberar a Área Pix do Nubank sem desativar o app.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                if (isPixPaused) {
                                    OutlinedButton(
                                        onClick = { overviewViewModel.resumeFromPixPause() },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Reativar Proteção Agora", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                } else {
                                    Button(
                                        onClick = { overviewViewModel.pauseForPix(10) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Pausa Rápida Pix (10 min)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Lista de Aplicativos Rastreados
        Column(verticalArrangement = spacedBy(10.dp)) {
            // Instagram
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

            // YouTube
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

            // TikTok
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

            // Facebook
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
                    BlockfyThemedAppIcon("Facebook")
                },
                settingsIcon = {
                    IconButton(
                        modifier = it,
                        onClick = {
                            selectedApp = appSettings.facebook
                            showSettingsDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.facebook_reels),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            ) {
                overviewViewModel.updateFacebook(
                    appSettings.facebook.copy(
                        blocked = it
                    )
                )
            }

            // X (Twitter)
            val xSummary = if (appSettings.x.appTotalDailyLimitMinutes > 0) {
                "${dailyUsage.xTotalSeconds / 60}m / ${appSettings.x.appTotalDailyLimitMinutes}m diários"
            } else {
                stringResource(R.string.block_x_summary)
            }

            SwitchPreference(
                value = appSettings.x.blocked,
                enabled = isAccessibilityGranted,
                title = stringResource(R.string.x_app),
                summary = xSummary,
                leadingIcon = {
                    BlockfyThemedAppIcon("X")
                },
                settingsIcon = {
                    IconButton(
                        modifier = it,
                        onClick = {
                            selectedApp = appSettings.x
                            showSettingsDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.x_app),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            ) {
                overviewViewModel.updateX(
                    appSettings.x.copy(
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
        SupportCreatorDialog(
            onDismiss = { showSupportDialog = false }
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
    // 1. Primary check via Settings.Secure (most reliable across all Android versions)
    try {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (!enabledServices.isNullOrBlank()) {
            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.contains("ReelsBlockAccessibilityService", ignoreCase = true) &&
                    (componentName.contains(packageName, ignoreCase = true) ||
                     componentName.contains("blokky", ignoreCase = true) ||
                     componentName.contains("blockfy", ignoreCase = true))
                ) {
                    return true
                }
            }
        }
    } catch (e: Exception) {
        // Fallback to AccessibilityManager below
    }

    // 2. Secondary check via AccessibilityManager with FEEDBACK_ALL_MASK
    try {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val runningServices = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        if (!runningServices.isNullOrEmpty()) {
            return runningServices.any { service ->
                service.id.contains("ReelsBlockAccessibilityService") &&
                (service.id.contains(packageName) ||
                 service.id.contains("blokky") ||
                 service.id.contains("blockfy"))
            }
        }
    } catch (e: Exception) {
        // Ignored
    }

    return false
}

@Preview
@Composable
fun SettingsLayoutPreview() {
    SettingsScreen()
}