package com.buenotty.blockfy.feature_preferences.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.buenotty.blockfy.R
import java.text.DateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/**
 * Focus mode: blocks every tracked app for a fixed time and cannot be ended early, so starting
 * it is a real commitment. The Quick Settings tile starts the same thing for one hour.
 */
@Composable
fun FocusCard(
    focusUntil: Long,
    onStart: (untilMillis: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val running = focusUntil > now
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.focus_card_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                if (running) {
                    stringResource(R.string.tile_focus_until, DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(focusUntil)))
                } else {
                    stringResource(R.string.focus_card_desc)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!running) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onStart(System.currentTimeMillis() + HOUR) }) { Text(stringResource(R.string.focus_1h)) }
                    OutlinedButton(onClick = { onStart(System.currentTimeMillis() + 2 * HOUR) }) { Text(stringResource(R.string.focus_2h)) }
                    OutlinedButton(onClick = { onStart(tomorrowStart()) }) { Text(stringResource(R.string.focus_tomorrow)) }
                }
            }
        }
    }
}

private const val HOUR = 60L * 60L * 1000L

private fun tomorrowStart(): Long =
    LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
