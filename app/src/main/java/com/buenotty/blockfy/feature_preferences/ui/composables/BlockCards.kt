package com.buenotty.blockfy.feature_preferences.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalFireDepartment
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.buenotty.blockfy.R
import com.buenotty.blockfy.feature_monitor.BlockPolicy
import com.buenotty.blockfy.feature_monitor.DayState
import com.buenotty.blockfy.feature_monitor.StreakInfo
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

/** A short encouraging line that changes every day. */
@Composable
fun DailyMotivationCard(text: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.motivation_title),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

private val MILESTONES = listOf(3, 7, 14, 30, 60, 90, 180, 365)

private fun tierRes(days: Int): Int = when {
    days <= 0 -> R.string.tier_0
    days < 3 -> R.string.tier_1
    days < 7 -> R.string.tier_2
    days < 14 -> R.string.tier_3
    days < 30 -> R.string.tier_4
    days < 90 -> R.string.tier_5
    else -> R.string.tier_6
}

/**
 * The centrepiece of the home screen: the clean-day streak, how far the next milestone is, the last
 * seven days, and the user's own reason. When today was broken it says plainly what was lost.
 */
@Composable
fun StreakHero(
    info: StreakInfo,
    why: String,
    onWriteWhy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val broken = info.lastDays.lastOrNull() == DayState.BROKEN
    val colors = MaterialTheme.colorScheme
    val brush = if (broken) {
        Brush.linearGradient(listOf(colors.errorContainer, colors.errorContainer))
    } else {
        Brush.linearGradient(listOf(colors.primary, colors.tertiary))
    }
    val onHero = if (broken) colors.onErrorContainer else colors.onPrimary

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .background(brush)
                .padding(22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = onHero, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.hero_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = onHero,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    stringResource(R.string.streak_best, info.best),
                    style = MaterialTheme.typography.labelMedium,
                    color = onHero.copy(alpha = 0.85f)
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = info.current.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 84.sp, lineHeight = 88.sp),
                    fontWeight = FontWeight.Black,
                    color = onHero
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(if (info.current == 1) R.string.streak_day else R.string.streak_days),
                    style = MaterialTheme.typography.titleLarge,
                    color = onHero,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }

            Text(
                text = when {
                    broken && info.lostDays > 0 -> stringResource(R.string.streak_lost, info.lostDays)
                    broken -> stringResource(R.string.streak_broken)
                    else -> stringResource(tierRes(info.current))
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = onHero
            )

            if (!broken) {
                val next = MILESTONES.firstOrNull { it > info.current }
                Spacer(Modifier.height(14.dp))
                if (next != null) {
                    LinearProgressIndicator(
                        progress = { info.current.toFloat() / next },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = onHero,
                        trackColor = onHero.copy(alpha = 0.25f)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.milestone_next, next - info.current, next),
                        style = MaterialTheme.typography.bodySmall,
                        color = onHero.copy(alpha = 0.9f)
                    )
                } else {
                    Text(
                        stringResource(R.string.milestone_max),
                        style = MaterialTheme.typography.bodySmall,
                        color = onHero.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            val today = remember { java.time.LocalDate.now() }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                info.lastDays.forEachIndexed { index, state ->
                    val date = today.minusDays((info.lastDays.size - 1 - index).toLong())
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(
                                    when (state) {
                                        DayState.CLEAN -> onHero
                                        DayState.BROKEN -> colors.error
                                        DayState.EMPTY -> onHero.copy(alpha = 0.22f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when (state) {
                                DayState.CLEAN -> Icon(
                                    Icons.Rounded.Check, contentDescription = null,
                                    tint = if (broken) colors.errorContainer else colors.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                DayState.BROKEN -> Icon(
                                    Icons.Rounded.Close, contentDescription = null,
                                    tint = colors.onError, modifier = Modifier.size(16.dp)
                                )
                                DayState.EMPTY -> Unit
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            date.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            color = onHero.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            if (why.isNotBlank()) {
                Text(
                    text = "\u201C$why\u201D",
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = onHero
                )
            } else {
                TextButton(onClick = onWriteWhy, contentPadding = PaddingValues(0.dp)) {
                    Text(stringResource(R.string.hero_write_why), color = onHero)
                }
            }
        }
    }
}
