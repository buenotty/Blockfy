package com.buenotty.blockfy.feature_setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.buenotty.blockfy.R
import com.buenotty.blockfy.feature_preferences.ui.composables.AccessibilityServiceDialog
import com.buenotty.blockfy.feature_preferences.ui.composables.isAccessibilityGranted

/**
 * The two things Blockfy cannot work without: Accessibility, and being allowed to keep running
 * in the background. Shown on first use, and again whenever Accessibility is found switched off.
 * [onFinish] is called once the user is done, so the caller can remember it.
 */
@Composable
fun SetupScreen(onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var accessibilityOn by remember { mutableStateOf(context.isAccessibilityGranted()) }
    var batteryOn by remember { mutableStateOf(context.isIgnoringBatteryOptimizations()) }
    var batterySkipped by remember { mutableStateOf(false) }
    var showDisclosure by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        accessibilityOn = context.isAccessibilityGranted()
        batteryOn = context.isIgnoringBatteryOptimizations()
    }

    val ready = accessibilityOn && (batteryOn || batterySkipped)
    // Leave on its own as soon as both are done, so the user is not left on a finished screen.
    LaunchedEffect(accessibilityOn, batteryOn) {
        if (accessibilityOn && batteryOn) onFinish()
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.setup_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))

            StepCard(
                number = 1,
                icon = Icons.Rounded.Accessibility,
                title = stringResource(R.string.setup_step1_title),
                description = stringResource(R.string.setup_step1_desc),
                done = accessibilityOn,
                buttonLabel = stringResource(R.string.setup_btn_accessibility),
                onClick = { showDisclosure = true }
            )
            StepCard(
                number = 2,
                icon = Icons.Rounded.BatteryChargingFull,
                title = stringResource(R.string.setup_step2_title),
                description = stringResource(R.string.setup_step2_desc),
                done = batteryOn,
                buttonLabel = stringResource(R.string.setup_btn_battery),
                onClick = { context.requestIgnoreBatteryOptimizations() }
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onFinish,
                enabled = ready,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(stringResource(R.string.setup_continue), fontWeight = FontWeight.SemiBold)
            }
            if (accessibilityOn && !batteryOn && !batterySkipped) {
                TextButton(
                    onClick = { batterySkipped = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(stringResource(R.string.setup_skip_battery), textAlign = TextAlign.Center)
                }
            }
        }
    }

    if (showDisclosure) {
        AccessibilityServiceDialog(onDismissRequest = { showDisclosure = false })
    }
}

@Composable
private fun StepCard(
    number: Int,
    icon: ImageVector,
    title: String,
    description: String,
    done: Boolean,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (done) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (done) Icons.Rounded.CheckCircle else icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "$number. $title",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(description, style = MaterialTheme.typography.bodyMedium)
            if (done) {
                Text(
                    stringResource(R.string.setup_done),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(buttonLabel) }
            }
        }
    }
}
