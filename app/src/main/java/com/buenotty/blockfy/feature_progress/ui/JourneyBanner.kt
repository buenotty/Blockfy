package com.buenotty.blockfy.feature_progress.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.buenotty.blockfy.R
import com.buenotty.blockfy.feature_preferences.OverviewViewModel
import com.buenotty.blockfy.feature_progress.Medals
import com.buenotty.blockfy.feature_progress.Ranks
import kotlinx.coroutines.delay

/**
 * A strip at the top of the app that announces a promotion or a new medal, instead of a pop-up in
 * the way. It leaves by itself after a few seconds, or when closed, and either way counts as seen.
 */
@Composable
fun JourneyBanner(viewModel: OverviewViewModel, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val ui by viewModel.journeyUi.collectAsState()
    val journey = ui.journey
    val promoted = journey.enlisted && ui.rank.level > journey.seenLevel
    val medal = if (journey.enlisted && !promoted) Medals.all.firstOrNull { it.id in journey.medals && it.id !in journey.seenMedals } else null
    val key = if (promoted) "rank-${ui.rank.level}" else medal?.id

    fun markSeen() {
        if (promoted) viewModel.markJourneySeen(ui.rank.level, emptyList())
        else if (medal != null) viewModel.markJourneySeen(journey.seenLevel, listOf(medal.id))
    }

    LaunchedEffect(key) {
        if (key != null) {
            delay(BANNER_MILLIS)
            markSeen()
        }
    }

    AnimatedVisibility(
        visible = key != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface)
        ) {
            Row(modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (promoted) RankBadge(ui.rank, size = 52.dp) else if (medal != null) MedalBadge(medal, unlocked = true, size = 52.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(if (promoted) R.string.rankup_title else R.string.medal_new_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.inverseOnSurface
                    )
                    Text(
                        if (promoted) {
                            val from = Ranks.of(Ranks.xpForLevel(journey.seenLevel))
                            stringResource(R.string.rankup_body, rankTitle(from), rankTitle(ui.rank))
                        } else {
                            stringResource(medal!!.title)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
                TextButton(onClick = { markSeen(); onOpen() }) {
                    Text(stringResource(R.string.banner_see), color = MaterialTheme.colorScheme.inversePrimary)
                }
                IconButton(onClick = { markSeen() }) {
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = MaterialTheme.colorScheme.inverseOnSurface)
                }
            }
        }
    }
}

private const val BANNER_MILLIS = 10_000L
