package com.buenotty.blockfy.feature_preferences.ui.composables

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.buenotty.blockfy.R

@Composable
fun DisableBlockerDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    title: String = stringResource(R.string.disable_dialog_title),
    message: String = stringResource(R.string.disable_dialog_desc),
    confirmLabel: String = stringResource(R.string.disable_dialog_confirm),
    streakDays: Int = 0,
    why: String = "",
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (streakDays > 0) {
                    StakesBanner(streakDays = streakDays, why = why)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Text(
                    text = message,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(R.string.disable_dialog_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TimedFilledButton(
                        text = confirmLabel,
                        onClick = onConfirmation,
                        modifier = Modifier
                            .height(48.dp)
                            .widthIn(min = 150.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TimedFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isClickEnabled by remember { mutableStateOf(false) }
    var animationEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animationEnabled = true
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (animationEnabled) 1.0f else 0f,
        animationSpec = tween(durationMillis = 5000, easing = LinearEasing),
        finishedListener = {
            isClickEnabled = true
        }
    )
    Box(modifier = modifier) {
        LinearProgressIndicator(
            progress = { animatedProgress },
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surfaceTint,
            trackColor = MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.2f)
        )
        Button(
            onClick = {
                if (isClickEnabled) {
                    onClick()
                }
            },
            modifier = Modifier.matchParentSize(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
        ) {
            Text(text)
        }
    }
}

@Preview
@Composable
fun DisableBlockerDialogPreview() {
    DisableBlockerDialog({}, {})
}


/** Makes the cost of giving up concrete: the clean days that would be lost, and the user's own reason. */
@Composable
fun StakesBanner(streakDays: Int, why: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = stringResource(R.string.stake_title, streakDays),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.stake_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            if (why.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.stake_why, why),
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
