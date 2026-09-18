package com.buenotty.blockfy.feature_preferences.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.buenotty.blockfy.R
import kotlin.random.Random

@Composable
fun StrictModeDialog(
    isMidnightLock: Boolean,
    onDismiss: () -> Unit,
    onUnlockSuccess: () -> Unit
) {
    if (isMidnightLock) {
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
    } else {
        val challengeCode = remember { generateChallengeCode() }
        var userInput by remember { mutableStateOf("") }
        val isMatch = userInput.trim() == challengeCode

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.strict_challenge_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.strict_challenge_instruction))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = challengeCode,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.strict_challenge_placeholder)) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = { if (isMatch) onUnlockSuccess() }, enabled = isMatch) {
                    Text(stringResource(R.string.strict_challenge_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel_btn))
                }
            }
        )
    }
}

private fun generateChallengeCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    fun block(len: Int) = (1..len).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    return "${block(4)}-${block(4)}-${block(4)}-${block(4)}"
}
