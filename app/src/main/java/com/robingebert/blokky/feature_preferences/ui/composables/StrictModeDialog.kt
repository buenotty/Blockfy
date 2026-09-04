package com.robingebert.blokky.feature_preferences.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robingebert.blokky.R
import com.robingebert.blokky.ui.theme.BlockfyPrimary
import kotlin.random.Random

@Composable
fun StrictModeDialog(
    isMidnightLock: Boolean,
    onDismiss: () -> Unit,
    onUnlockSuccess: () -> Unit
) {
    if (isMidnightLock) {
        // Inviolable Midnight Lock: Informative lock dialog without override
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(20.dp),
            containerColor = Color(0xFF131728),
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Modo Inviolável Ativo",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.strict_mode_midnight_locked_msg),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Você optou por proteger seu futuro contra impulsos momentâneos. O bloqueio só poderá ser alterado após as 00:00.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = BlockfyPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Manter o Foco", fontWeight = FontWeight.Bold)
                }
            }
        )
    } else {
        // Challenge mode: requires typing a complex random string to defeat impulsive dopamine urge
        val challengeCode = remember { generateChallengeCode() }
        var userInput by remember { mutableStateOf("") }
        val isMatch = userInput.trim().equals(challengeCode, ignoreCase = false)

        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(20.dp),
            containerColor = Color(0xFF131728),
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.strict_challenge_title),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.strict_challenge_instruction),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Code box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0C0E1A), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = challengeCode,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCD34D),
                            letterSpacing = 1.sp,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Digite o código acima...", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isMatch) {
                            onUnlockSuccess()
                        }
                    },
                    enabled = isMatch,
                    colors = ButtonDefaults.buttonColors(containerColor = BlockfyPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.strict_challenge_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = Color(0xFF94A3B8))
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
