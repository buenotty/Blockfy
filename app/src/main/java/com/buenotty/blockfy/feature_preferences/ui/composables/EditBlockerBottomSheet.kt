package com.buenotty.blockfy.feature_preferences.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.buenotty.blockfy.R
import com.buenotty.blockfy.feature_preferences.repository.models.App

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var blockedWeekdays by remember { mutableIntStateOf(app.blockedWeekdays) }
    var totalTyped by remember { mutableStateOf(if (app.appTotalDailyLimitMinutes == 0) "" else app.appTotalDailyLimitMinutes.toString()) }
    var shortsTyped by remember { mutableStateOf(if (app.dailyLimitMinutes == 0) "" else app.dailyLimitMinutes.toString()) }

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
                appTotalDailyLimitMinutes = appTotalDailyLimitMinutes,
                blockedWeekdays = blockedWeekdays
            )
        )
        onDismiss()
    }

    ModalBottomSheet(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surface,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(vertical = 8.dp, horizontal = 20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppBrandIcon(trackedAppIcon(app.name))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.settings_dialog_title, app.name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.weekdays_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val labels = listOf(
                        R.string.weekday_sun,
                        R.string.weekday_mon,
                        R.string.weekday_tue,
                        R.string.weekday_wed,
                        R.string.weekday_thu,
                        R.string.weekday_fri,
                        R.string.weekday_sat
                    )
                    labels.forEachIndexed { index, label ->
                        val bit = 1 shl index
                        SelectableFilterChip(
                            selected = blockedWeekdays and bit != 0,
                            enabled = !isStrictLocked,
                            onClick = {
                                val next = blockedWeekdays xor bit
                                if (next != 0) blockedWeekdays = next
                            },
                            label = stringResource(label)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.schedule_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
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

                Text(
                    text = stringResource(R.string.app_total_limit_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))

                IconRow(icon = Icons.Rounded.HourglassBottom) {
                    Column {
                        Text(
                            text = if (appTotalDailyLimitMinutes == 0) {
                                stringResource(R.string.app_total_limit_none)
                            } else {
                                stringResource(R.string.app_total_limit_on, appTotalDailyLimitMinutes)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))

                        val totalOptions = listOf(0, 15, 30, 45, 60, 90, 120)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            totalOptions.forEach { min ->
                                SelectableFilterChip(
                                    selected = appTotalDailyLimitMinutes == min,
                                    onClick = {
                                        if (!isStrictLocked) {
                                            appTotalDailyLimitMinutes = min
                                            totalTyped = if (min == 0) "" else min.toString()
                                        }
                                    },
                                    enabled = !isStrictLocked,
                                    label = if (min == 0) {
                                        stringResource(R.string.daily_limit_off)
                                    } else {
                                        "${min}m"
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = totalTyped,
                            onValueChange = { raw ->
                                val digits = raw.filter { it.isDigit() }.take(4)
                                totalTyped = digits
                                appTotalDailyLimitMinutes = digits.toIntOrNull()?.coerceIn(0, 1440) ?: 0
                            },
                            enabled = !isStrictLocked,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text(stringResource(R.string.minutes_custom_hint)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }


                Spacer(Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.daily_limit_picker_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
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

                        val options = listOf(0, 5, 10, 15, 30, 45, 60)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            options.forEach { min ->
                                SelectableFilterChip(
                                    selected = dailyLimitMinutes == min,
                                    onClick = {
                                        dailyLimitMinutes = min
                                        shortsTyped = if (min == 0) "" else min.toString()
                                    },
                                    label = if (min == 0) {
                                        stringResource(R.string.daily_limit_off)
                                    } else {
                                        "${min}m"
                                    },
                                    enabled = !isStrictLocked
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = shortsTyped,
                            onValueChange = { raw ->
                                val digits = raw.filter { it.isDigit() }.take(4)
                                shortsTyped = digits
                                dailyLimitMinutes = digits.toIntOrNull()?.coerceIn(0, 1440) ?: 0
                            },
                            enabled = !isStrictLocked,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text(stringResource(R.string.minutes_custom_hint)) },
                            modifier = Modifier.fillMaxWidth()
                        )

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

                if (isStrictLocked) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.errorContainer,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.strict_locked_banner),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
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
            tint = MaterialTheme.colorScheme.primary,
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
