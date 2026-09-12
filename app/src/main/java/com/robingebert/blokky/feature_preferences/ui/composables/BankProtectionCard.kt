package com.robingebert.blokky.feature_preferences.ui.composables

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.robingebert.blokky.R
import com.robingebert.blokky.feature_accessibility.BankProtectionManager
import com.robingebert.blokky.ui.theme.BlockfyPrimary
import com.robingebert.blokky.ui.theme.BlockfySuccess

@Composable
fun BankProtectionCard(
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var showInfoDialog by remember { mutableStateOf(false) }

    val hasUsageStats = BankProtectionManager.hasUsageStatsPermission(context)
    val hasAdbPermission = BankProtectionManager.hasWriteSecureSettingsPermission(context)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalance,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.bank_protection_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            IconButton(
                                onClick = { showInfoDialog = true },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Info,
                                    contentDescription = "Info",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.bank_protection_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF38BDF8)
                    )
                )
            }

            // Status badges if enabled
            if (enabled) {
                if (!hasUsageStats) {
                    // Usage Stats required banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFEF3C7).copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.bank_usage_access_needed),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFBBF24)
                            )
                        }
                        TextButton(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                } catch (e: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                }
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.btn_allow_usage_access),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24)
                            )
                        }
                    }
                } else if (hasAdbPermission) {
                    // ADB Auto-reactivation active
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(BlockfySuccess.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = BlockfySuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.bank_adb_active_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = BlockfySuccess,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    // Standard mode (notification on exit)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.bank_standard_active_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showInfoDialog) {
        BankProtectionInfoDialog(onDismiss = { showInfoDialog = false })
    }
}

@Composable
fun BankProtectionInfoDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val adbScript = "adb shell pm grant com.buenotty.blockfy android.permission.WRITE_SECURE_SETTINGS"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalance,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.bank_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.bank_dialog_explanation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(14.dp))

                // Card 1: Como Funciona
                OutlinedCard(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.bank_dialog_how_it_works_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.bank_dialog_how_it_works_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Card 2: ADB Auto-reactivation (Super Power)
                OutlinedCard(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, BlockfyPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Terminal,
                                contentDescription = null,
                                tint = BlockfyPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.bank_dialog_adb_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BlockfyPrimary
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.bank_dialog_adb_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SelectionContainer {
                                Text(
                                    text = adbScript,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(adbScript))
                                Toast.makeText(context, "Comando ADB copiado!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = BlockfyPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copiar Comando ADB", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                TextButton(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = onDismiss
                ) {
                    Text(stringResource(R.string.btn_close))
                }
            }
        }
    }
}
