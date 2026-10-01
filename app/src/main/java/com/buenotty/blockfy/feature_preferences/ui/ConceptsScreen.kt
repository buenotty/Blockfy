package com.buenotty.blockfy.feature_preferences.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.buenotty.blockfy.R
import com.buenotty.blockfy.feature_preferences.ui.composables.TintedGlyph
import kotlinx.coroutines.launch

enum class ConceptTopic(val route: String) {
    SHORTS("shorts"),
    PORN("porn");

    companion object {
        fun fromRoute(route: String?): ConceptTopic = entries.firstOrNull { it.route == route } ?: SHORTS
    }
}

@Composable
private fun ConceptTopic.titles(): Array<String> = stringArrayResource(
    if (this == ConceptTopic.SHORTS) R.array.concept_shorts_titles else R.array.concept_porn_titles
)

@Composable
private fun ConceptTopic.bodies(): Array<String> = stringArrayResource(
    if (this == ConceptTopic.SHORTS) R.array.concept_shorts_bodies else R.array.concept_porn_bodies
)

/** The Concepts tab: a short reminder of why the blocks exist, then the topics to read. */
@Composable
fun ConceptsHomeScreen(onOpenTopic: (ConceptTopic) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    stringResource(R.string.concepts_hero_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.concepts_intro), style = MaterialTheme.typography.bodyMedium)
            }
        }

        TopicCard(
            title = stringResource(R.string.concepts_shorts_title),
            summary = stringResource(R.string.concepts_shorts_summary),
            points = ConceptTopic.SHORTS.titles().size,
            icon = Icons.Rounded.Psychology,
            onClick = { onOpenTopic(ConceptTopic.SHORTS) }
        )
        TopicCard(
            title = stringResource(R.string.concepts_porn_title),
            summary = stringResource(R.string.concepts_porn_summary),
            points = ConceptTopic.PORN.titles().size,
            icon = Icons.Rounded.Shield,
            onClick = { onOpenTopic(ConceptTopic.PORN) }
        )
    }
}

@Composable
private fun TopicCard(
    title: String,
    summary: String,
    points: Int,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TintedGlyph(icon, wellSize = 48.dp, glyphSize = 26.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.concepts_points, points),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Full-screen reader: one idea per card, a progress bar, and big previous/next buttons. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConceptDetailScreen(topic: ConceptTopic, onBack: () -> Unit) {
    val titles = topic.titles()
    val bodies = topic.bodies()
    val count = minOf(titles.size, bodies.size)
    val pagerState = rememberPagerState(pageCount = { count })
    val scope = rememberCoroutineScope()
    val page = pagerState.currentPage
    val screenTitle = stringResource(
        if (topic == ConceptTopic.SHORTS) R.string.concepts_shorts_title else R.string.concepts_porn_title
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(page - 1) } },
                        enabled = page > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) { Text(stringResource(R.string.concepts_prev)) }
                    Button(
                        onClick = {
                            if (page < count - 1) {
                                scope.launch { pagerState.animateScrollToPage(page + 1) }
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(stringResource(if (page < count - 1) R.string.concepts_next else R.string.concepts_done))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { (page + 1f) / count.coerceAtLeast(1) },
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.concepts_page, page + 1, count),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                pageSpacing = 12.dp
            ) { index ->
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp)
                    ) {
                        Column {
                            Text(
                                text = titles[index],
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = bodies[index],
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
