package com.buenotty.blockfy.feature_settings

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Shop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.buenotty.blockfy.feature_onboarding.NoticeScreen
import androidx.core.net.toUri
import com.buenotty.blockfy.AppLocale
import com.buenotty.blockfy.R
import com.buenotty.blockfy.SupportLinks
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.runtime.collectAsState
import com.buenotty.blockfy.feature_preferences.OverviewViewModel
import com.buenotty.blockfy.feature_preferences.ui.composables.PreferenceGroup
import com.buenotty.blockfy.feature_preferences.ui.composables.SelectableFilterChip
import com.buenotty.blockfy.feature_preferences.ui.composables.SwitchPreference
import com.buenotty.blockfy.feature_preferences.ui.composables.TintedGlyph
import org.koin.androidx.compose.koinViewModel
import com.buenotty.blockfy.updater.UpdateManager
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import qrcode.QRCode

@Composable
fun AboutScreen(overviewViewModel: OverviewViewModel = koinViewModel()) {
    val context = LocalContext.current
    val appSettings by overviewViewModel.appSettings.collectAsState()
    var showQrCodeDialog by remember { mutableStateOf(false) }
    var showNotice by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = spacedBy(20.dp)
    ) {
        val streak by overviewViewModel.streak.collectAsState()
        ScheduleSettingsCard(saved = appSettings, streakDays = streak.current, onSave = overviewViewModel::setSchedule)

        PreferenceGroup(title = stringResource(R.string.provocation_mode_title)) {
            SwitchPreference(
                value = appSettings.provocationModeEnabled,
                title = stringResource(R.string.provocation_mode_title),
                summary = stringResource(R.string.provocation_mode_desc),
                grouped = true,
                showDivider = false,
                confirmDisable = false,
                leadingIcon = { TintedGlyph(Icons.Rounded.Psychology) }
            ) { overviewViewModel.setProvocationMode(it) }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_policy),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = stringResource(R.string.version_label, getVersionName(context)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                IconButton(onClick = { showQrCodeDialog = true }) {
                    Icon(
                        Icons.Rounded.QrCode2,
                        contentDescription = stringResource(R.string.share_app_title),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        LanguageSettingsCard()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = spacedBy(12.dp)
        ) {
            HeroCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Shop,
                title = stringResource(R.string.check_updates_btn)
            ) {
                UpdateManager.openPlayStoreOrSource(context)
            }
            HeroCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.BugReport,
                title = stringResource(R.string.report_bug_btn)
            ) {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, SupportLinks.GITHUB_ISSUES.toUri())
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = spacedBy(12.dp)
        ) {
            HeroCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Balance,
                title = stringResource(R.string.licenses_btn)
            ) {
                context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
            }
            HeroCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Code,
                title = stringResource(R.string.source_code_btn)
            ) {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, SupportLinks.GITHUB.toUri())
                )
            }
        }

        PreferenceGroup(title = stringResource(R.string.authorship_title)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.about_story),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Samuel Bueno, ${stringResource(R.string.fork_author_role)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Robin Gebert, ${stringResource(R.string.original_author_role)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        PreferenceGroup(title = stringResource(R.string.privacy_title)) {
            Text(
                text = stringResource(R.string.privacy_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
            TextButton(
                modifier = Modifier.padding(horizontal = 8.dp),
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, SupportLinks.PRIVACY_POLICY.toUri())
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.privacy_policy_btn))
            }
        }

        PreferenceGroup(title = stringResource(R.string.notice_title)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.notice_reread_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { showNotice = true }) {
                    Text(stringResource(R.string.notice_reread_btn))
                }
            }
        }


        Spacer(modifier = Modifier.height(8.dp))
    }

    if (showNotice) {
        Dialog(
            onDismissRequest = { showNotice = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            NoticeScreen(onAccept = { showNotice = false })
        }
    }

    if (showQrCodeDialog) {
        QrCodeDialog { showQrCodeDialog = false }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageSettingsCard() {
    val context = LocalContext.current
    var selection by remember { mutableStateOf(AppLocale.currentSelection(context)) }

    PreferenceGroup(title = stringResource(R.string.language_title)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.language_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = spacedBy(8.dp),
                verticalArrangement = spacedBy(8.dp)
            ) {
                SelectableFilterChip(
                    selected = selection == AppLocale.AUTO,
                    label = stringResource(R.string.language_automatic),
                    onClick = {
                        AppLocale.apply(context, AppLocale.AUTO)
                        selection = AppLocale.AUTO
                    }
                )
                SelectableFilterChip(
                    selected = selection == AppLocale.ENGLISH,
                    label = stringResource(R.string.language_english),
                    onClick = {
                        AppLocale.apply(context, AppLocale.ENGLISH)
                        selection = AppLocale.ENGLISH
                    }
                )
                SelectableFilterChip(
                    selected = selection == AppLocale.PORTUGUESE,
                    label = stringResource(R.string.language_portuguese),
                    onClick = {
                        AppLocale.apply(context, AppLocale.PORTUGUESE)
                        selection = AppLocale.PORTUGUESE
                    }
                )
            }
        }
    }
}

fun getVersionName(context: Context): String {
    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return pInfo?.versionName ?: "Unknown"
}

@Composable
fun HeroCard(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            verticalArrangement = spacedBy(10.dp)
        ) {
            TintedGlyph(icon)
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun QrCodeDialog(onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val qrPng = remember(context.packageName) {
        QRCode.ofRoundedSquares()
            .withBackgroundColor(android.graphics.Color.TRANSPARENT)
            .withColor(android.graphics.Color.BLACK)
            .build("https://play.google.com/store/apps/details?id=${context.packageName}")
            .renderToBytes()
    }
    val qrBitmap = remember(qrPng) {
        BitmapFactory.decodeByteArray(qrPng, 0, qrPng.size).asImageBitmap()
    }
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.share_app_title), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    modifier = Modifier.background(Color.White).padding(8.dp),
                    bitmap = qrBitmap,
                    contentDescription = stringResource(R.string.share_app_title),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(R.string.share_app_thanks), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.btn_close))
                }
            }
        }
    }
}

@Preview
@Composable
fun AboutScreenPreview() {
    AboutScreen()
}
