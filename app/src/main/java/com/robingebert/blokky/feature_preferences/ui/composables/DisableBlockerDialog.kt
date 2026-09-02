package com.robingebert.blokky.feature_preferences.ui.composables

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.robingebert.blokky.R
import com.robingebert.blokky.ui.theme.BlockfyError
import com.robingebert.blokky.ui.theme.BlockfyPrimary

@Composable
fun DisableBlockerDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = BlockfyError,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.disable_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.disable_dialog_desc),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimedFilledButton(
                        text = stringResource(R.string.disable_dialog_confirm),
                        onClick = onConfirmation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(R.string.disable_dialog_cancel))
                    }
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
        animationSpec = tween(durationMillis = 3500, easing = LinearEasing),
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
                .clip(RoundedCornerShape(12.dp)),
            color = BlockfyError.copy(alpha = 0.35f),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Button(
            onClick = {
                if (isClickEnabled) {
                    onClick()
                }
            },
            modifier = Modifier.matchParentSize(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isClickEnabled) BlockfyError else Color.Transparent,
                contentColor = if (isClickEnabled) Color.White else MaterialTheme.colorScheme.onSurface
            ),
        ) {
            Text(
                text = if (!isClickEnabled) "$text (${((1.0f - animatedProgress) * 3.5).toInt() + 1}s)" else text,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Preview
@Composable
fun DisableBlockerDialogPreview() {
    DisableBlockerDialog({}, {})
}