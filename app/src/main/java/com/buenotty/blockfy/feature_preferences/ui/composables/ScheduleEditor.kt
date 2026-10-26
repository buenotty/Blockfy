package com.buenotty.blockfy.feature_preferences.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.buenotty.blockfy.R

private const val NIGHT_START = 22 * 60
private const val NIGHT_END = 6 * 60
private const val WORK_START = 9 * 60
private const val WORK_END = 18 * 60

/** Hours and weekdays picker. Stateless: the caller owns the values and decides when to save. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleEditor(
    start: Int,
    end: Int,
    days: Int,
    onStartChange: (Int) -> Unit,
    onEndChange: (Int) -> Unit,
    onDaysChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val hoursValid = start != end

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.edit_hours_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SelectableFilterChip(
                selected = start == FULL_DAY_START && end == FULL_DAY_END,
                label = stringResource(R.string.schedule_all_day),
                onClick = { onStartChange(FULL_DAY_START); onEndChange(FULL_DAY_END) }
            )
            SelectableFilterChip(
                selected = start == NIGHT_START && end == NIGHT_END,
                label = stringResource(R.string.preset_night),
                onClick = { onStartChange(NIGHT_START); onEndChange(NIGHT_END) }
            )
            SelectableFilterChip(
                selected = start == WORK_START && end == WORK_END,
                label = stringResource(R.string.preset_work),
                onClick = { onStartChange(WORK_START); onEndChange(WORK_END) }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeField(
                label = stringResource(R.string.start_time),
                value = start.toTime(),
                onClick = { showStartPicker = true },
                modifier = Modifier.weight(1f)
            )
            TimeField(
                label = stringResource(R.string.end_time),
                value = end.toTime(),
                onClick = { showEndPicker = true },
                modifier = Modifier.weight(1f)
            )
        }
        if (!hoursValid) {
            Text(
                stringResource(R.string.hours_error_equal),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else if (start > end) {
            Text(
                stringResource(R.string.hours_crosses_midnight),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            stringResource(R.string.weekdays_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SelectableFilterChip(
                selected = days == ALL_DAYS_MASK,
                label = stringResource(R.string.weekdays_everyday),
                onClick = { onDaysChange(ALL_DAYS_MASK) }
            )
            SelectableFilterChip(
                selected = days == WORKDAYS_MASK,
                label = stringResource(R.string.weekdays_workdays),
                onClick = { onDaysChange(WORKDAYS_MASK) }
            )
            SelectableFilterChip(
                selected = days == WEEKEND_MASK,
                label = stringResource(R.string.weekdays_weekend),
                onClick = { onDaysChange(WEEKEND_MASK) }
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val labels = listOf(
                R.string.weekday_sun, R.string.weekday_mon, R.string.weekday_tue, R.string.weekday_wed,
                R.string.weekday_thu, R.string.weekday_fri, R.string.weekday_sat
            )
            labels.forEachIndexed { index, label ->
                val bit = 1 shl index
                SelectableFilterChip(
                    selected = days and bit != 0,
                    label = stringResource(label),
                    onClick = {
                        val next = days xor bit
                        if (next != 0) onDaysChange(next)
                    }
                )
            }
        }
    }

    if (showStartPicker) {
        TimePickerDialog(
            initialTime = start,
            title = stringResource(R.string.start_time),
            onDismiss = { showStartPicker = false },
            onConfirm = { onStartChange(it); showStartPicker = false }
        )
    }
    if (showEndPicker) {
        TimePickerDialog(
            initialTime = end,
            title = stringResource(R.string.end_time),
            onDismiss = { showEndPicker = false },
            onConfirm = { onEndChange(it); showEndPicker = false }
        )
    }
}

@Composable
private fun TimeField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}
