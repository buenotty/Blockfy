package com.buenotty.blockfy.feature_progress.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.buenotty.blockfy.R
import com.buenotty.blockfy.feature_preferences.OverviewViewModel
import com.buenotty.blockfy.feature_progress.JourneyEngine
import com.buenotty.blockfy.feature_progress.JourneyUi
import com.buenotty.blockfy.feature_progress.MedalDef
import com.buenotty.blockfy.feature_progress.Medals
import com.buenotty.blockfy.feature_progress.RankState
import com.buenotty.blockfy.feature_progress.Ranks
import org.koin.androidx.compose.koinViewModel

private val ROMAN = listOf("I", "II", "III")

/** "Recruta II", or "Pessoa comum" before the first promotion. */
@Composable
fun rankTitle(rank: RankState): String {
    if (rank.rankIndex < 0) return stringResource(R.string.rank_common)
    return "${stringArrayResource(R.array.rank_names)[rank.rankIndex]} ${ROMAN[rank.sub]}"
}

/** The clean-time goals as words: 1 day, 1 week, 3 months, 1 year, 2 years... */
@Composable
fun goalLabel(days: Int): String = when (days) {
    1 -> stringResource(R.string.goal_1d)
    3 -> stringResource(R.string.goal_3d)
    7 -> stringResource(R.string.goal_1w)
    14 -> stringResource(R.string.goal_2w)
    30 -> stringResource(R.string.goal_1m)
    60 -> stringResource(R.string.goal_2m)
    90 -> stringResource(R.string.goal_3m)
    180 -> stringResource(R.string.goal_6m)
    365 -> stringResource(R.string.goal_1y)
    else -> stringResource(R.string.goal_years, days / 365)
}

@Composable
fun JourneyScreen(viewModel: OverviewViewModel = koinViewModel()) {
    val ui by viewModel.journeyUi.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RankHero(ui)
        GoalCard(ui)
        StatsRow(ui)
        MedalsSection(ui)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RankHero(ui: JourneyUi) {
    val next = Ranks.of(Ranks.xpForLevel(ui.rank.level + 1))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RankBadge(ui.rank, size = 168.dp)
            Text(rankTitle(ui.rank), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (ui.rank.prestige > 0) {
                Text(
                    stringResource(R.string.rank_prestige, ui.rank.prestige),
                    style = MaterialTheme.typography.labelLarge,
                    color = Metals.Gold.mid
                )
            }
            LinearProgressIndicator(
                progress = { ui.rank.fraction.toFloat() },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                strokeCap = StrokeCap.Round
            )
            Text(
                stringResource(R.string.journey_next_level, rankTitle(next)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.journey_xp, ui.journey.xp.toInt()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GoalCard(ui: JourneyUi) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val percent = (ui.goal.fraction * 100).toInt()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(modifier = Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(112.dp)) {
                    val stroke = size.minDimension * 0.11f
                    val inset = stroke / 2
                    val arc = Size(size.width - stroke, size.height - stroke)
                    drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
                    drawArc(
                        Brush.sweepGradient(listOf(Metals.Gold.mid, Metals.Gold.light, Metals.Gold.mid)),
                        -90f, 360f * ui.goal.fraction.toFloat(), false, Offset(inset, inset), arc,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                Text("$percent%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.journey_goal_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.journey_goal_pct, percent, goalLabel(ui.goal.days)),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StatsRow(ui: JourneyUi) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        StatTile(stringResource(R.string.journey_run), "${ui.liveRun}", Modifier.weight(1f))
        StatTile(stringResource(R.string.journey_best), "${maxOf(ui.journey.bestRun, ui.liveRun)}", Modifier.weight(1f))
        StatTile(stringResource(R.string.journey_shields), "${ui.shieldsLeft}/${JourneyEngine.SHIELDS_PER_MONTH}", Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MedalsSection(ui: JourneyUi) {
    var opened by remember { mutableStateOf<MedalDef?>(null) }
    val earned = ui.journey.medals
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.journey_medals), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.journey_medals_count, earned.size, Medals.all.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            maxItemsInEachRow = 3
        ) {
            // Earned medals first, then the ones still to win, each group in catalogue order.
            Medals.all.sortedByDescending { it.id in earned }.forEach { def ->
                val unlocked = def.id in earned
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    androidx.compose.material3.TextButton(onClick = { opened = def }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        MedalBadge(def, unlocked, size = 88.dp)
                    }
                    Text(
                        if (unlocked) stringResource(def.title) else stringResource(R.string.journey_locked),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    opened?.let { def ->
        MedalDialog(def, unlocked = def.id in earned, onDismiss = { opened = null })
    }
}

@Composable
private fun MedalDialog(def: MedalDef, unlocked: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(def.title), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                MedalBadge(def, unlocked, size = 160.dp)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(def.description), textAlign = TextAlign.Center)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.rankup_cta)) } }
    )
}

/**
 * Celebrates what the user has not seen yet, one thing at a time: first the promotion, then each
 * new medal. Marking it seen is what brings the next one up.
 */
@Composable
fun JourneyCelebrationHost(viewModel: OverviewViewModel) {
    val ui by viewModel.journeyUi.collectAsState()
    val journey = ui.journey
    if (!journey.enlisted) return
    if (ui.rank.level > journey.seenLevel) {
        val from = Ranks.of(Ranks.xpForLevel(journey.seenLevel))
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.rankup_title), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    RankBadge(ui.rank, size = 200.dp)
                    Text(
                        stringResource(R.string.rankup_body, rankTitle(from), rankTitle(ui.rank)),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.markJourneySeen(ui.rank.level, emptyList()) }) { Text(stringResource(R.string.rankup_cta)) }
            }
        )
        return
    }
    val unseen = Medals.all.firstOrNull { it.id in journey.medals && it.id !in journey.seenMedals } ?: return
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.medal_new_title), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                MedalBadge(unseen, unlocked = true, size = 180.dp)
                Text(stringResource(unseen.title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(stringResource(unseen.description), textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.markJourneySeen(journey.seenLevel, listOf(unseen.id)) }) { Text(stringResource(R.string.rankup_cta)) }
        }
    )
}
