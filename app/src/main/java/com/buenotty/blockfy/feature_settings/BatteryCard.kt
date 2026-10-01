package com.buenotty.blockfy.feature_settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.buenotty.blockfy.R
import com.buenotty.blockfy.feature_preferences.ui.composables.PreferenceGroup

/** True when the system will not put Blockfy to sleep to save battery. */
fun Context.isIgnoringBatteryOptimizations(): Boolean {
    val manager = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return manager.isIgnoringBatteryOptimizations(packageName)
}

/**
 * Explains the battery behaviour and sends the user to the system screens where the app can be set
 * to "unrestricted". Blockfy does not request the exemption through the system dialog: Google Play
 * only allows that for apps that break completely without it, so the user is guided instead.
 */
@Composable
fun BatteryCard() {
    val context = LocalContext.current
    var ignoring by remember { mutableStateOf(context.isIgnoringBatteryOptimizations()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        ignoring = context.isIgnoringBatteryOptimizations()
    }

    PreferenceGroup(title = stringResource(R.string.battery_card_title)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(R.string.battery_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(if (ignoring) R.string.battery_status_ok else R.string.battery_status_bad),
                style = MaterialTheme.typography.bodyMedium,
                color = if (ignoring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            if (!ignoring) {
                Text(
                    stringResource(R.string.battery_steps),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.battery_btn_list), maxLines = 1) }
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.battery_btn_app), maxLines = 1) }
            }
        }
    }
}
