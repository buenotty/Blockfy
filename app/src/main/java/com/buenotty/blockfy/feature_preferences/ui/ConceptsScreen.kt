package com.buenotty.blockfy.feature_preferences.ui

import androidx.annotation.ArrayRes
import org.koin.androidx.compose.koinViewModel
import com.buenotty.blockfy.feature_preferences.ui.composables.SectionTitle
import com.buenotty.blockfy.feature_preferences.OverviewViewModel
import com.buenotty.blockfy.feature_preferences.MAX_WHY_LENGTH
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Check
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.annotation.StringRes
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
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.HealthAndSafety
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

enum class ConceptTopic(
    val route: String,
    @StringRes val title: Int,
    @StringRes val summary: Int,
    @ArrayRes val titles: Int,
    @ArrayRes val bodies: Int,
    val icon: ImageVector
) {
    SHORTS("shorts", R.string.concepts_shorts_title, R.string.concepts_shorts_summary,
        R.array.concept_shorts_titles, R.array.concept_shorts_bodies, Icons.Rounded.Psychology),
    TIME("time", R.string.concepts_time_title, R.string.concepts_time_summary,
        R.array.concept_time_titles, R.array.concept_time_bodies, Icons.Rounded.Timer),
    PORN("porn", R.string.concepts_porn_title, R.string.concepts_porn_summary,
        R.array.concept_porn_titles, R.array.concept_porn_bodies, Icons.Rounded.Shield),
    MINDSET("mindset", R.string.concepts_mindset_title, R.string.concepts_mindset_summary,
        R.array.concept_mindset_titles, R.array.concept_mindset_bodies, Icons.Rounded.Lightbulb),
    HABITS("habits", R.string.concepts_habits_title, R.string.concepts_habits_summary,
        R.array.concept_habits_titles, R.array.concept_habits_bodies, Icons.Rounded.Autorenew),
    RELAPSE("relapse", R.string.concepts_relapse_title, R.string.concepts_relapse_summary,
        R.array.concept_relapse_titles, R.array.concept_relapse_bodies, Icons.Rounded.Replay),
    FOCUS("focus", R.string.concepts_focus_title, R.string.concepts_focus_summary,
        R.array.concept_focus_titles, R.array.concept_focus_bodies, Icons.Rounded.CenterFocusStrong),
    PLAN("plan", R.string.concepts_plan_title, R.string.concepts_plan_summary,
        R.array.concept_plan_titles, R.array.concept_plan_bodies, Icons.Rounded.Checklist),
    HELP("help", R.string.concepts_help_title, R.string.concepts_help_summary,
        R.array.concept_help_titles, R.array.concept_help_bodies, Icons.Rounded.HealthAndSafety);

    companion object {
        fun fromRoute(route: String?): ConceptTopic = entries.firstOrNull { it.route == route } ?: SHORTS
    }
}

@Composable
private fun ConceptTopic.titles(): Array<String> = stringArrayResource(titles)

@Composable
private fun ConceptTopic.bodies(): Array<String> = stringArrayResource(bodies)

/** The Concepts tab: a path of readings, in order, plus the user's own reason for changing. */
@Composable
fun ConceptsHomeScreen(
    onOpenTopic: (ConceptTopic) -> Unit,
    viewModel: OverviewViewModel = koinViewModel()
) {
    val settings by viewModel.appSettings.collectAsState()
    var showWhyDialog by remember { mutableStateOf(false) }
    val nextTopic = ConceptTopic.entries.firstOrNull { topic ->
        (settings.conceptsRead[topic.route] ?: 0) < stringArrayResource(topic.titles).size
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)))
                    .padding(22.dp)
            ) {
                Text(
                    stringResource(R.string.concepts_hero_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.concepts_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Card(
            onClick = { showWhyDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.why_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(if (settings.myWhy.isBlank()) R.string.why_add else R.string.why_edit),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (settings.myWhy.isBlank()) stringResource(R.string.why_hint) else "\u201C${settings.myWhy}\u201D",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = if (settings.myWhy.isBlank()) FontStyle.Normal else FontStyle.Italic
                    ),
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        SectionTitle(stringResource(R.string.concepts_path_title))
        ConceptTopic.entries.forEachIndexed { index, topic ->
            val total = topic.titles().size
            TopicCard(
                number = index + 1,
                title = stringResource(topic.title),
                summary = stringResource(topic.summary),
                read = (settings.conceptsRead[topic.route] ?: 0).coerceAtMost(total),
                total = total,
                icon = topic.icon,
                highlight = topic == nextTopic,
                onClick = { onOpenTopic(topic) }
            )
        }
    }

    if (showWhyDialog) {
        WhyDialog(
            current = settings.myWhy,
            onDismiss = { showWhyDialog = false },
            onSave = {
                viewModel.setMyWhy(it)
                showWhyDialog = false
            }
        )
    }
}

@Composable
private fun WhyDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.why_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(MAX_WHY_LENGTH) },
                placeholder = { Text(stringResource(R.string.why_placeholder)) },
                supportingText = { Text("${text.length}/$MAX_WHY_LENGTH") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) { Text(stringResource(R.string.why_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_btn)) }
        }
    )
}

@Composable
private fun TopicCard(
    number: Int,
    title: String,
    summary: String,
    read: Int,
    total: Int,
    icon: ImageVector,
    highlight: Boolean,
    onClick: () -> Unit
) {
    val done = read >= total
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = if (highlight) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                if (done) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(
                        number.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else read.toFloat() / total },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when {
                        done -> stringResource(R.string.concepts_done_badge)
                        highlight -> stringResource(R.string.concepts_continue) + " · " + stringResource(R.string.concepts_progress, read, total)
                        else -> stringResource(R.string.concepts_progress, read, total)
                    },
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
fun ConceptDetailScreen(
    topic: ConceptTopic,
    onBack: () -> Unit,
    viewModel: OverviewViewModel = koinViewModel()
) {
    val titles = topic.titles()
    val bodies = topic.bodies()
    val count = minOf(titles.size, bodies.size)
    val pagerState = rememberPagerState(pageCount = { count })
    val scope = rememberCoroutineScope()
    val page = pagerState.currentPage
    LaunchedEffect(page) { viewModel.markConceptRead(topic.route, page + 1) }
    val screenTitle = stringResource(topic.title)

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
