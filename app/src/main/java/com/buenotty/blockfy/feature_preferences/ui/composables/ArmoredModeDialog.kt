package com.buenotty.blockfy.feature_preferences.ui.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.buenotty.blockfy.R

/** Asks once, in plain words, before the permanent lock is switched on. */
@Composable
fun ArmoredModeDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.armored_title)) },
        text = { Text(stringResource(R.string.armored_confirm_msg)) },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(R.string.armored_confirm_btn)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_btn)) } }
    )
}
