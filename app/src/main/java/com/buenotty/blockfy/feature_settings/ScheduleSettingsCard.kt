package com.buenotty.blockfy.feature_settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.buenotty.blockfy.R
import com.buenotty.blockfy.datastore.AppSettings
import com.buenotty.blockfy.feature_monitor.BlockPolicy
import com.buenotty.blockfy.feature_preferences.ui.composables.DisableBlockerDialog
import com.buenotty.blockfy.feature_preferences.ui.composables.PreferenceGroup
import com.buenotty.blockfy.feature_preferences.ui.composables.ScheduleEditor

/**
 * Hours and weekdays when every app's blocks and limits are enforced. Narrowing them is a way of
 * loosening the blocks, so strict mode refuses it and otherwise it needs a timed confirmation.
 */
@Composable
fun ScheduleSettingsCard(
    saved: AppSettings,
    streakDays: Int,
    onSave: (start: Int, end: Int, weekdays: Int) -> Unit
) {
    var start by remember(saved.scheduleStart) { mutableStateOf(saved.scheduleStart) }
    var end by remember(saved.scheduleEnd) { mutableStateOf(saved.scheduleEnd) }
    var days by remember(saved.scheduleWeekdays) { mutableStateOf(saved.scheduleWeekdays) }
    var showConfirm by remember { mutableStateOf(false) }

    val draft = saved.copy(scheduleStart = start, scheduleEnd = end, scheduleWeekdays = days)
    val dirty = draft != saved
    val loosening = BlockPolicy.isScheduleLoosening(saved, draft)
    val locked = saved.isStrictLocked()
    val canSave = dirty && start != end && !(locked && loosening)

    PreferenceGroup(title = stringResource(R.string.schedule_card_title)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.schedule_card_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ScheduleEditor(
                start = start,
                end = end,
                days = days,
                onStartChange = { start = it },
                onEndChange = { end = it },
                onDaysChange = { days = it }
            )
            if (locked && loosening) {
                Text(
                    stringResource(R.string.strict_locked_banner),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(
                onClick = { if (loosening) showConfirm = true else onSave(start, end, days) },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.schedule_save_btn))
            }
        }
    }

    if (showConfirm) {
        DisableBlockerDialog(
            title = stringResource(R.string.loosen_title),
            message = stringResource(R.string.loosen_msg),
            confirmLabel = stringResource(R.string.loosen_confirm),
            streakDays = streakDays,
            why = saved.myWhy,
            onDismissRequest = { showConfirm = false },
            onConfirmation = {
                showConfirm = false
                onSave(start, end, days)
            }
        )
    }
}
