package com.buenotty.blockfy.feature_preferences.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.buenotty.blockfy.R
import com.buenotty.blockfy.feature_accessibility.AdultContentDetector

/**
 * Lists the sites the user added to the adult list and lets them add more. Adding is permanent,
 * so it takes two steps: type the address, then confirm after seeing the exact host that will be
 * blocked.
 */
@Composable
fun AdultSitesCard(
    sites: List<String>,
    onAdd: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var adding by remember { mutableStateOf(false) }
    Card(
        onClick = { adding = true },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            TintedGlyph(Icons.Rounded.Block)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.adult_sites_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (sites.isEmpty()) stringResource(R.string.adult_sites_summary_empty)
                    else stringResource(R.string.adult_sites_summary_count, sites.size, sites.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { adding = true }) { Text(stringResource(R.string.adult_sites_add)) }
        }
    }
    if (adding) AddAdultSiteDialog(known = sites, onDismiss = { adding = false }, onConfirm = { onAdd(it); adding = false })
}

@Composable
private fun AddAdultSiteDialog(known: List<String>, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var confirming by remember { mutableStateOf(false) }
    var checked by remember { mutableStateOf(false) }
    val host = AdultContentDetector.normalizeSite(text)
    val alreadyBlocked = host != null && (host in known || AdultContentDetector.isBlockedHost(host))

    if (!confirming) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.adult_sites_add)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text(stringResource(R.string.adult_sites_field)) },
                        placeholder = { Text("example.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth()
                    )
                    val note = when {
                        text.isBlank() -> null
                        host == null -> stringResource(R.string.adult_sites_invalid) to true
                        alreadyBlocked -> stringResource(R.string.adult_sites_already) to true
                        else -> stringResource(R.string.adult_sites_preview, host) to false
                    }
                    if (note != null) {
                        Text(
                            note.first,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (note.second) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { confirming = true }, enabled = host != null && !alreadyBlocked) {
                    Text(stringResource(R.string.adult_sites_next))
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } }
        )
    } else if (host != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.adult_sites_confirm_title, host)) },
            text = {
                Column {
                    Text(stringResource(R.string.adult_sites_confirm_body), color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { checked = it })
                        Text(stringResource(R.string.adult_sites_confirm_check), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onConfirm(host) }, enabled = checked) {
                    Text(stringResource(R.string.adult_sites_confirm_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false; checked = false }) { Text(stringResource(R.string.adult_sites_back)) }
            }
        )
    }
}
