package com.robingebert.blokky.feature_preferences.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.robingebert.blokky.R
import com.robingebert.blokky.feature_preferences.repository.models.App
import com.robingebert.blokky.ui.theme.BlockfyPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAppBottomSheet(
    app: App,
    todayUsedSeconds: Long = 0L,
    isStrictLocked: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (App) -> Unit,
    onResetUsage: (() -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
) {
    var blockedStart by remember { mutableIntStateOf(app.blockedStart) }
    var blockedEnd by remember { mutableIntStateOf(app.blockedEnd) }
    var dailyLimitMinutes by remember { mutableIntStateOf(app.dailyLimitMinutes) }
    var appTotalDailyLimitMinutes by remember { mutableIntStateOf(app.appTotalDailyLimitMinutes) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    fun save() {
        if (isStrictLocked) {
            onDismiss()
            return
        }
        onSave(
            app.copy(
                blockedStart = blockedStart,
                blockedEnd = blockedEnd,
                dailyLimitMinutes = dailyLimitMinutes,
                appTotalDailyLimitMinutes = appTotalDailyLimitMinutes
            )
        )
        onDismiss()
    }

    ModalBottomSheet(
        containerColor = MaterialTheme.colorScheme.surface,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    BlockfyThemedAppIcon(app.name)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.settings_dialog_title, app.name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Schedule Section
                Text(
                    text = stringResource(R.string.schedule_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = BlockfyPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))

                IconRow(icon = Icons.Rounded.Event) {
                    Row {
                        val resetSourceStart = remember { MutableInteractionSource() }
                        if (resetSourceStart.collectIsPressedAsState().value) {
                            showStartTimePicker = true
                        }
                        TextField(
                            modifier = Modifier.weight(1f),
                            value = blockedStart.toTime(),
                            readOnly = true,
                            interactionSource = resetSourceStart,
                            onValueChange = { },
                            label = { Text(stringResource(R.string.start_time)) },
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        val resetSourceTime = remember { MutableInteractionSource() }
                        if (resetSourceTime.collectIsPressedAsState().value) {
                            showEndTimePicker = true
                        }
                        TextField(
                            modifier = Modifier.weight(1f),
                            value = blockedEnd.toTime(),
                            readOnly = true,
                            interactionSource = resetSourceTime,
                            onValueChange = { },
                            label = { Text(stringResource(R.string.end_time)) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Daily Limit Section
                Text(
                    text = stringResource(R.string.daily_limit_picker_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = BlockfyPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))

                IconRow(icon = Icons.Rounded.Timer) {
                    Column {
                        Text(
                            text = if (dailyLimitMinutes == 0) {
                                stringResource(R.string.daily_limit_picker_desc)
                            } else {
                                "${dailyLimitMinutes} min"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))

                        // Quick selection chips
                        val options = listOf(0, 5, 10, 15, 30, 45, 60)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            options.take(4).forEach { min ->
                                FilterChip(
                                    selected = dailyLimitMinutes == min,
                                    onClick = { dailyLimitMinutes = min },
                                    label = { Text(if (min == 0) stringResource(R.string.daily_limit_off) else "${min}m") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BlockfyPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            options.drop(4).forEach { min ->
                                FilterChip(
                                    selected = dailyLimitMinutes == min,
                                    onClick = { dailyLimitMinutes = min },
                                    label = { Text("${min}m") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BlockfyPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        // Usage info & reset button
                        if (dailyLimitMinutes > 0) {
                            Spacer(Modifier.height(10.dp))
                            val usedMin = todayUsedSeconds / 60
                            val usedSec = todayUsedSeconds % 60
                            Text(
                                text = "${stringResource(R.string.today_usage_label, usedMin.toInt(), dailyLimitMinutes)} (${usedSec}s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (onResetUsage != null && todayUsedSeconds > 0) {
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = onResetUsage,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.reset_usage_btn))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // App Total Daily Limit Section
                Text(
                    text = stringResource(R.string.app_total_limit_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF60A5FA),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))

                IconRow(icon = Icons.Rounded.HourglassBottom) {
                    Column {
                        Text(
                            text = if (appTotalDailyLimitMinutes == 0) {
                                "Sem limite geral (apenas vídeos curtos)"
                            } else {
                                "${appTotalDailyLimitMinutes} min no total do app"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))

                        val totalOptions = listOf(0, 15, 30, 45, 60, 90, 120)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            totalOptions.take(4).forEach { min ->
                                FilterChip(
                                    selected = appTotalDailyLimitMinutes == min,
                                    onClick = { if (!isStrictLocked) appTotalDailyLimitMinutes = min },
                                    enabled = !isStrictLocked,
                                    label = { Text(if (min == 0) stringResource(R.string.daily_limit_off) else "${min}m") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF3B82F6),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            totalOptions.drop(4).forEach { min ->
                                FilterChip(
                                    selected = appTotalDailyLimitMinutes == min,
                                    onClick = { if (!isStrictLocked) appTotalDailyLimitMinutes = min },
                                    enabled = !isStrictLocked,
                                    label = { Text("${min}m") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF3B82F6),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                if (isStrictLocked) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Modo Inviolável Ativo: limites travados até a meia-noite.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFCA5A5)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Save button
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isStrictLocked) Color.Gray else BlockfyPrimary
                    ),
                    enabled = !isStrictLocked,
                    onClick = { save() }
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.save_btn), fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(16.dp))
            }
        },
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    )

    if (showStartTimePicker) {
        TimePickerDialog(
            initialTime = blockedStart,
            onDismiss = { showStartTimePicker = false },
            onConfirm = {
                blockedStart = it
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            initialTime = blockedEnd,
            onDismiss = { showEndTimePicker = false },
            onConfirm = {
                blockedEnd = it
                showEndTimePicker = false
            }
        )
    }
}

@Composable
fun IconRow(
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BlockfyPrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

fun Int.toTime(): String {
    val hours = this / 60
    val minutes = this % 60
    return String.format("%02d:%02d", hours, minutes)
}