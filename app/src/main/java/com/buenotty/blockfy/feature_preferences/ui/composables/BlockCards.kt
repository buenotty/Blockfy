package com.buenotty.blockfy.feature_preferences.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.buenotty.blockfy.R
import com.buenotty.blockfy.feature_monitor.BlockPolicy
import com.buenotty.blockfy.datastore.DailyUsage
import com.buenotty.blockfy.feature_preferences.repository.models.App

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 4.dp, top = 4.dp)
    )
}

/** Big, impossible-to-miss answer to "is Blockfy actually protecting me right now?". */
@Composable
fun ProtectionStatusCard(
    isGranted: Boolean,
    onEnable: () -> Unit,
    onManage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = if (isGranted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    val content = if (isGranted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Rounded.Shield else Icons.Rounded.Warning,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(if (isGranted) R.string.status_active_title else R.string.status_inactive_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(if (isGranted) R.string.status_active_desc else R.string.status_inactive_desc),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.width(8.dp))
            if (isGranted) {
                TextButton(onClick = onManage) { Text(stringResource(R.string.status_manage_btn)) }
            } else {
                Button(onClick = onEnable) { Text(stringResource(R.string.status_enable_btn)) }
            }
        }
    }
}

@Composable
fun TodaySummaryCard(blocks: Int, savedSeconds: Long, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.today_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat(
                    value = blocks.toString(),
                    label = stringResource(R.string.today_blocks_label),
                    modifier = Modifier.weight(1f)
                )
                Stat(
                    value = stringResource(R.string.today_minutes_format, (savedSeconds / 60).toInt()),
                    label = stringResource(R.string.today_saved_label),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * One app. Tapping the card opens its rules; the switch only turns the whole block on or off,
 * so the two actions can never be confused. Each active rule gets its own line and, when it has a
 * limit, its own progress bar.
 */
@Composable
fun AppBlockCard(
    app: App,
    usage: DailyUsage,
    onToggle: (Boolean) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wholeOn = BlockPolicy.wholeRuleOn(app)
    val shortsOn = BlockPolicy.shortsRuleOn(app)
    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppBrandIcon(trackedAppIcon(app.name))
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appDisplayName(app.name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!app.blocked) {
                        Text(
                            text = stringResource(R.string.app_state_off),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (!wholeOn && !shortsOn) {
                        Text(
                            text = stringResource(R.string.app_no_rule),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Switch(checked = app.blocked, onCheckedChange = onToggle)
            }
            if (app.blocked) {
                if (wholeOn) {
                    RuleLine(
                        label = stringResource(R.string.app_scope_whole),
                        limitMinutes = app.appTotalDailyLimitMinutes,
                        usedSeconds = BlockPolicy.totalSeconds(usage, app.name)
                    )
                }
                if (shortsOn) {
                    RuleLine(
                        label = shortsFeatureLabel(app.name),
                        limitMinutes = app.dailyLimitMinutes,
                        usedSeconds = BlockPolicy.featureSeconds(usage, app.name)
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleLine(label: String, limitMinutes: Int, usedSeconds: Long) {
    Spacer(Modifier.height(10.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = limitLabel(limitMinutes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (limitMinutes > 0) {
        val over = usedSeconds >= limitMinutes * 60L
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (usedSeconds.toFloat() / (limitMinutes * 60f)).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.today_usage_label, (usedSeconds / 60).toInt(), limitMinutes),
            style = MaterialTheme.typography.labelMedium,
            color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** A whole-row switch card for global options (adult shield, strict mode). */
@Composable
fun ToggleCard(
    icon: ImageVector,
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TintedGlyph(icon)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = checked, onCheckedChange = null, enabled = enabled)
        }
    }
}

/** Shows the global hours and days and links to where they are changed. */
@Composable
fun ScheduleSummaryCard(
    summary: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TintedGlyph(Icons.Rounded.Schedule)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.schedule_link_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onOpen) { Text(stringResource(R.string.schedule_change_btn)) }
        }
    }
}
