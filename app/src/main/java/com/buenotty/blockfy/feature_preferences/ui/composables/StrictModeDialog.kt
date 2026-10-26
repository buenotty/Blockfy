package com.buenotty.blockfy.feature_preferences.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.buenotty.blockfy.R

/** Shown when the user tries to loosen a block while strict mode is holding it. */
@Composable
fun StrictModeDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.strict_mode_title)) },
        text = {
            Column {
                Text(stringResource(R.string.strict_mode_midnight_locked_msg))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.strict_mode_midnight_extra),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.strict_mode_keep_focus))
            }
        }
    )
}
