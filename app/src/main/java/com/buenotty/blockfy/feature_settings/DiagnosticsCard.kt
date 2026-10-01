package com.buenotty.blockfy.feature_settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.buenotty.blockfy.R
import com.buenotty.blockfy.feature_accessibility.ServiceDiagnostics
import com.buenotty.blockfy.feature_preferences.ui.composables.PreferenceGroup
import kotlinx.coroutines.delay

/**
 * Shows what the accessibility service sees right now. It exists so that when a block does not
 * fire, a screenshot of this card tells us whether the service is connected, which app it last
 * saw, and which screen elements that app is exposing.
 */
@Composable
fun DiagnosticsCard() {
    val snapshot by ServiceDiagnostics.snapshot.collectAsState()
    var expanded by rememberSaveable { mutableStateOf(false) }

    // Ids are only gathered while the details are open.
    DisposableEffect(expanded) {
        ServiceDiagnostics.collectingViewIds = expanded
        onDispose { ServiceDiagnostics.collectingViewIds = false }
    }

    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }

    PreferenceGroup(title = stringResource(R.string.diag_title)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                stringResource(R.string.diag_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(if (snapshot.connected) R.string.diag_service_on else R.string.diag_service_off),
                style = MaterialTheme.typography.bodyMedium,
                color = if (snapshot.connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Text(
                stringResource(
                    R.string.diag_last_app,
                    snapshot.lastPackage.ifEmpty { stringResource(R.string.diag_never) }
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            if (snapshot.lastEventAtMillis > 0) {
                Text(
                    stringResource(R.string.diag_last_event, ((now - snapshot.lastEventAtMillis) / 1000).toInt().coerceAtLeast(0)),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    stringResource(if (snapshot.shortsVisible) R.string.diag_shorts_yes else R.string.diag_shorts_no),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    stringResource(R.string.diag_decision, snapshot.lastDecision),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            TextButton(onClick = { expanded = !expanded }) {
                Text(stringResource(if (expanded) R.string.diag_hide else R.string.diag_show))
            }
            if (expanded) {
                Text(stringResource(R.string.diag_ids_title), style = MaterialTheme.typography.labelLarge)
                Text(
                    stringResource(R.string.diag_ids_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SelectionContainer {
                    Text(
                        text = snapshot.seenViewIds.joinToString("\n").ifEmpty { "-" },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
