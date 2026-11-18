package com.buenotty.blockfy.feature_games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.buenotty.blockfy.R
import kotlinx.coroutines.delay
import kotlin.random.Random

/** The short games a person can pick when the urge hits. */
private enum class Game(val icon: ImageVector, val title: Int, val subtitle: Int) {
    BREATHE(Icons.Rounded.SelfImprovement, R.string.game_breathe_title, R.string.game_breathe_sub),
    HOLD(Icons.Rounded.TouchApp, R.string.game_hold_title, R.string.game_hold_sub),
    MEMORY(Icons.Rounded.Extension, R.string.game_memory_title, R.string.game_memory_sub),
    QUIZ(Icons.Rounded.Quiz, R.string.game_quiz_title, R.string.game_quiz_sub)
}

/**
 * "The urge hit": a small menu of short, finite things to do for a few minutes instead of
 * scrolling. [onFinished] is told when a game ends well, so it can pay a little XP; [xpHint] says
 * whether the next finished game still pays today.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(xpHint: Boolean, onFinished: () -> Unit, onBack: () -> Unit) {
    var game by remember { mutableStateOf<Game?>(null) }
    var done by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.games_title)) },
            navigationIcon = {
                IconButton(onClick = { if (game != null) { game = null; done = false } else onBack() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                }
            }
        )
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val current = game
            if (current == null) {
                Hub(xpHint) { game = it; done = false }
            } else if (done) {
                Finished(xpHint, onAnother = { game = null; done = false }, onClose = onBack)
            } else {
                val finish = { onFinished(); done = true }
                when (current) {
                    Game.BREATHE -> BreatheGame(finish)
                    Game.HOLD -> HoldGame(finish)
                    Game.MEMORY -> MemoryGame(finish)
                    Game.QUIZ -> QuizGame(finish)
                }
            }
        }
    }
}

@Composable
private fun Hub(xpHint: Boolean, onPick: (Game) -> Unit) {
    Text(stringResource(R.string.games_intro), style = MaterialTheme.typography.titleMedium)
    Text(
        stringResource(if (xpHint) R.string.games_xp_yes else R.string.games_xp_no),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Game.entries.forEach { g ->
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onPick(g) },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(g.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(stringResource(g.title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(g.subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun Finished(xpHint: Boolean, onAnother: () -> Unit, onClose: () -> Unit) {
    val line = remember { Random.nextInt(100) }
    val lines = stringArrayResource(R.array.games_done_lines)
    Spacer(Modifier.height(24.dp))
    Text(stringResource(R.string.games_done_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Text(lines[line % lines.size], style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    if (xpHint) {
        Text(stringResource(R.string.games_xp_earned), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
    Spacer(Modifier.height(8.dp))
    Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.games_close)) }
    OutlinedButton(onClick = onAnother, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.games_another)) }
}

// ---- Breathe ----------------------------------------------------------------------------------

private const val BREATH_TOTAL_MILLIS = 2 * 60_000L

@Composable
private fun BreatheGame(onFinish: () -> Unit) {
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = withFrameMillis { it }
        while (elapsed < BREATH_TOTAL_MILLIS) {
            withFrameMillis { elapsed = it - start }
        }
        onFinish()
    }
    val step = Breath.at(elapsed)
    val label = when (step.kind) {
        BreathKind.IN -> R.string.breathe_in
        BreathKind.HOLD -> R.string.breathe_hold
        BreathKind.OUT -> R.string.breathe_out
    }
    val lines = stringArrayResource(R.array.breathe_lines)
    val remaining = ((BREATH_TOTAL_MILLIS - elapsed) / 1000L).coerceAtLeast(0L)
    Text(stringResource(R.string.breathe_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
        val accent = MaterialTheme.colorScheme.primary
        Canvas(Modifier.size(260.dp)) {
            val r = size.minDimension / 2 * step.size
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.55f), accent.copy(alpha = 0.12f)), center, r), r, center)
            drawCircle(accent, r, center, style = Stroke(width = 4f))
        }
        Text(stringResource(label), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    }
    Text(lines[((elapsed / 14_000L).toInt()) % lines.size], textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text("${remaining / 60}:${(remaining % 60).toString().padStart(2, '0')}", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}

// ---- Hold on ----------------------------------------------------------------------------------

private const val HOLD_GOAL_MILLIS = 15_000L

@Composable
private fun HoldGame(onFinish: () -> Unit) {
    var state by remember { mutableStateOf(HoldState(HOLD_GOAL_MILLIS)) }
    var pressed by remember { mutableStateOf(false) }
    var everLetGo by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (!state.won) withFrameMillis { now -> state = Hold.step(state, now, pressed) }
        onFinish()
    }
    val lines = stringArrayResource(R.array.hold_lines)
    val line = when {
        everLetGo && state.heldMillis == 0L -> stringResource(R.string.hold_letgo)
        else -> lines[(state.fraction * lines.size).toInt().coerceAtMost(lines.size - 1)]
    }
    Text(stringResource(R.string.hold_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
        val track = MaterialTheme.colorScheme.surfaceVariant
        val accent = MaterialTheme.colorScheme.primary
        Canvas(Modifier.size(250.dp)) {
            val stroke = 22f
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(stroke / 2, stroke / 2), arc, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(accent, -90f, 360f * state.fraction, false, Offset(stroke / 2, stroke / 2), arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Box(
            Modifier
                .size(170.dp)
                .clip(RoundedCornerShape(85.dp))
                .background(if (pressed) accent else MaterialTheme.colorScheme.primaryContainer)
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                        everLetGo = true
                    })
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(if (pressed) R.string.hold_keep else R.string.hold_press),
                style = MaterialTheme.typography.titleMedium,
                color = if (pressed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
    Text(line, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyLarge)
    Text("${(state.fraction * 100).toInt()}%", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium)
}

// ---- Memory -----------------------------------------------------------------------------------

private val MEMORY_FACES: List<ImageVector> = listOf(
    Icons.Rounded.Notifications, Icons.Rounded.Favorite, Icons.Rounded.Share,
    Icons.AutoMirrored.Rounded.Chat, Icons.Rounded.PlayArrow, Icons.Rounded.Star
)

@Composable
private fun MemoryGame(onFinish: () -> Unit) {
    var state by remember { mutableStateOf(Memory.deal(MEMORY_FACES.size, Random.nextLong())) }
    LaunchedEffect(state.pendingMismatch) {
        if (state.pendingMismatch != null) {
            delay(800)
            state = Memory.clearMismatch(state)
        }
    }
    LaunchedEffect(state.won) {
        if (state.won) {
            delay(700)
            onFinish()
        }
    }
    Text(stringResource(R.string.memory_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text(stringResource(R.string.memory_hint, state.moves), color = MaterialTheme.colorScheme.onSurfaceVariant)
    val columns = 3
    state.faces.chunked(columns).forEachIndexed { row, cards ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            cards.forEachIndexed { col, face ->
                val index = row * columns + col
                val up = index in state.revealed || index in state.matched
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            when {
                                index in state.matched -> MaterialTheme.colorScheme.tertiaryContainer
                                up -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        )
                        .clickable(enabled = !up) { state = Memory.flip(state, index) },
                    contentAlignment = Alignment.Center
                ) {
                    if (up) Icon(MEMORY_FACES[face], contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    else Text("?", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ---- Quiz -------------------------------------------------------------------------------------

private const val QUIZ_QUESTIONS = 5

@Composable
private fun QuizGame(onFinish: () -> Unit) {
    val questions = stringArrayResource(R.array.quiz_questions)
    val feedback = stringArrayResource(R.array.quiz_feedback)
    val options = listOf(stringArrayResource(R.array.quiz_opt0), stringArrayResource(R.array.quiz_opt1), stringArrayResource(R.array.quiz_opt2))
    val correctRaw = stringArrayResource(R.array.quiz_correct)
    val correct = IntArray(questions.size) { correctRaw[it].toInt() }
    var state by remember { mutableStateOf(Quiz.start(questions.size, QUIZ_QUESTIONS, Random.nextLong())) }
    if (state.finished) {
        LaunchedEffect(Unit) { onFinish() }
        return
    }
    val q = state.current
    Text(stringResource(R.string.quiz_progress, state.index + 1, state.order.size), color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(questions[q], style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    for (i in 0..2) {
        val picked = state.picked
        val container = when {
            picked == null -> MaterialTheme.colorScheme.surfaceContainerHigh
            i == correct[q] -> MaterialTheme.colorScheme.tertiaryContainer
            i == picked -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        }
        Card(
            modifier = Modifier.fillMaxWidth().clickable(enabled = picked == null) { state = Quiz.answer(state, i, correct) },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = container)
        ) {
            Text(options[i][q], modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
        }
    }
    if (state.picked != null) {
        Text(feedback[q], style = MaterialTheme.typography.bodyLarge)
        Button(onClick = { state = Quiz.next(state) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(stringResource(if (state.index + 1 >= state.order.size) R.string.quiz_finish else R.string.quiz_next))
        }
    }
}
