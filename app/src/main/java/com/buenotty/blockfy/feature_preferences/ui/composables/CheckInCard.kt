package com.buenotty.blockfy.feature_preferences.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.buenotty.blockfy.R

/**
 * One honest question, asked once: have you seen porn inside any of these apps? Whatever apps
 * the user names, the follow-up offers to block them completely, so the user's own words become
 * the reason to keep them blocked. Answers stay on the phone.
 */
@Composable
fun CheckInCard(
    apps: List<String>,
    onAnswer: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    Card(
        onClick = { open = true },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            TintedGlyph(Icons.Rounded.Shield)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.checkin_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.checkin_card_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
    if (open) CheckInDialog(apps = apps, onDismiss = { open = false }, onAnswer = { onAnswer(it); open = false })
}

@Composable
private fun CheckInDialog(apps: List<String>, onDismiss: () -> Unit, onAnswer: (List<String>) -> Unit) {
    var picked by remember { mutableStateOf(setOf<String>()) }
    var suggesting by remember { mutableStateOf(false) }
    val ordered = apps.filter { it in picked }

    if (!suggesting) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.checkin_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(R.string.checkin_dialog_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    apps.forEach { name ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(
                                checked = name in picked,
                                onCheckedChange = { picked = if (it) picked + name else picked - name }
                            )
                            Text(name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                if (picked.isEmpty()) {
                    TextButton(onClick = { onAnswer(emptyList()) }) { Text(stringResource(R.string.checkin_none)) }
                } else {
                    TextButton(onClick = { suggesting = true }) { Text(stringResource(R.string.adult_sites_next)) }
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.checkin_later)) } }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.checkin_suggest_title, ordered.joinToString(", "))) },
            text = { Text(stringResource(R.string.checkin_suggest_body)) },
            confirmButton = {
                TextButton(onClick = { onAnswer(ordered) }) { Text(stringResource(R.string.checkin_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { suggesting = false }) { Text(stringResource(R.string.adult_sites_back)) }
            }
        )
    }
}
