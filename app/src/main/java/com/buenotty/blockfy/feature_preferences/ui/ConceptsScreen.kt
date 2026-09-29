package com.buenotty.blockfy.feature_preferences.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.buenotty.blockfy.R
import com.buenotty.blockfy.feature_preferences.ui.composables.TintedGlyph
import kotlinx.coroutines.launch

@Composable
fun ConceptsScreen(onSupport: () -> Unit) {
    val topic = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<ConceptTopic?>(null) }
    val selected = topic.value
    if (selected == null) {
        ConceptMenu(onPick = { topic.value = it }, onSupport = onSupport)
    } else {
        ConceptCards(topic = selected, onBack = { topic.value = null })
    }
}

private enum class ConceptTopic { SHORTS, PORN }

@Composable
private fun ConceptMenu(onPick: (ConceptTopic) -> Unit, onSupport: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.concepts_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ConceptChoice(
            title = stringResource(R.string.concepts_shorts_title),
            summary = stringResource(R.string.concepts_shorts_summary),
            icon = Icons.Rounded.Psychology,
            onClick = { onPick(ConceptTopic.SHORTS) }
        )
        ConceptChoice(
            title = stringResource(R.string.concepts_porn_title),
            summary = stringResource(R.string.concepts_porn_summary),
            icon = Icons.Rounded.Shield,
            onClick = { onPick(ConceptTopic.PORN) }
        )
        ConceptChoice(
            title = stringResource(R.string.support_creator_title),
            summary = stringResource(R.string.support_creator_subtitle),
            icon = Icons.Rounded.VolunteerActivism,
            onClick = onSupport
        )
    }
}

@Composable
private fun ConceptChoice(
    title: String,
    summary: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            TintedGlyph(icon)
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConceptCards(topic: ConceptTopic, onBack: () -> Unit) {
    val titles = stringArrayResource(
        if (topic == ConceptTopic.SHORTS) R.array.concept_shorts_titles else R.array.concept_porn_titles
    )
    val bodies = stringArrayResource(
        if (topic == ConceptTopic.SHORTS) R.array.concept_shorts_bodies else R.array.concept_porn_bodies
    )
    val count = minOf(titles.size, bodies.size)
    val pagerState = rememberPagerState(pageCount = { count })
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.btn_back))
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 12.dp
        ) { page ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = titles[page],
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = bodies[page],
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.concepts_page, pagerState.currentPage + 1, count),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        androidx.compose.material3.TextButton(
            onClick = {
                if (pagerState.currentPage < count - 1) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                if (pagerState.currentPage < count - 1) {
                    stringResource(R.string.concepts_next)
                } else {
                    stringResource(R.string.concepts_swipe_hint)
                }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
