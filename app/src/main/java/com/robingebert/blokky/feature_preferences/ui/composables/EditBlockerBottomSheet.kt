package com.robingebert.blokky.feature_preferences.ui.composables

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.robingebert.blokky.feature_preferences.repository.models.App

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAppBottomSheet(
    app: App,
    todayUsedSeconds: Long = 0L,
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

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    fun save() {
        onSave(
            app.copy(
                blockedStart = blockedStart,
                blockedEnd = blockedEnd,
                dailyLimitMinutes = dailyLimitMinutes
            )
        )
        onDismiss()
    }

    ModalBottomSheet(
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "Configurações - ${app.name}",
                    style = MaterialTheme.typography.headlineSmall,
                )

                Spacer(Modifier.height(16.dp))

                // Schedule Section
                Text(
                    text = "Horário de Bloqueio",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
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
                            label = { Text("Início") }
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
                            label = { Text("Fim") }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Daily Limit Section
                Text(
                    text = "Limite Diário de Uso",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))

                IconRow(icon = Icons.Rounded.Timer) {
                    Column {
                        Text(
                            text = if (dailyLimitMinutes == 0) {
                                "Sem limite de tempo (bloqueia direto conforme horário)"
                            } else {
                                "Permitir até $dailyLimitMinutes min por dia"
                            },
                            style = MaterialTheme.typography.bodyMedium
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
                                    label = { Text(if (min == 0) "Desat." else "${min}m") }
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
                                    label = { Text("${min}m") }
                                )
                            }
                        }

                        // Usage info & reset button
                        if (dailyLimitMinutes > 0) {
                            Spacer(Modifier.height(10.dp))
                            val usedMin = todayUsedSeconds / 60
                            val usedSec = todayUsedSeconds % 60
                            Text(
                                text = "Uso hoje: ${usedMin}m ${usedSec}s de ${dailyLimitMinutes}m",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (onResetUsage != null && todayUsedSeconds > 0) {
                                Spacer(Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = onResetUsage,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Zerar contador de hoje")
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Save button
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { save() }
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Salvar")
                }

                Spacer(Modifier.height(16.dp))
            }
        },
        sheetState = sheetState,
        onDismissRequest = { onDismiss() }
    )

    if (showStartTimePicker) {
        TimePickerDialog(
            initialTime = blockedStart,
            onConfirm = {
                blockedStart = it
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false },
            title = "Horário de Início"
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            initialTime = blockedEnd,
            onConfirm = {
                blockedEnd = it
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false },
            title = "Horário de Término"
        )
    }
}

@Composable
fun IconRow(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(26.dp),
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
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
    return "%02d:%02d".format(hours, minutes)
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun ShowResetBottomSheetPreview() {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(key1 = Unit) {
        bottomSheetState.expand()
    }
    EditAppBottomSheet(
        app = App(
            name = "Instagram",
            blocked = true,
            blockedStart = 0,
            blockedEnd = 1439,
            blockedTimer = 0,
            features = emptyList(),
            dailyLimitMinutes = 15
        ),
        sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = Density(LocalContext.current),
            initialValue = SheetValue.Expanded
        ),
        onDismiss = {},
        onSave = {}
    )
}