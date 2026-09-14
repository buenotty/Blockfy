package com.robingebert.blokky.feature_monitor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robingebert.blokky.R
import com.robingebert.blokky.ui.theme.BlokkyTheme
import kotlinx.coroutines.delay

class InterruptActivity : ComponentActivity() {

    private var kindState = mutableStateOf(KIND_BLOCK)
    private var titleState = mutableStateOf("Blockfy")
    private var messageState = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyIntent(intent)
        setContent {
            BlokkyTheme {
                InterruptScreen(
                    kind = kindState.value,
                    title = titleState.value,
                    message = messageState.value,
                    onDismiss = { dismiss(kindState.value) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyIntent(intent)
    }

    private fun applyIntent(intent: Intent?) {
        kindState.value = intent?.getStringExtra(EXTRA_KIND) ?: KIND_BLOCK
        titleState.value = intent?.getStringExtra(EXTRA_TITLE) ?: "Blockfy"
        messageState.value = intent?.getStringExtra(EXTRA_MESSAGE).orEmpty()
    }

    private fun dismiss(kind: String) {
        if (kind != KIND_MINDFULNESS) {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        }
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        dismiss(kindState.value)
    }

    companion object {
        const val EXTRA_KIND = "extra_kind"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val KIND_BLOCK = "block"
        const val KIND_MINDFULNESS = "mindfulness"
        const val KIND_ADULT = "adult"

        fun intent(context: Context, kind: String, title: String, message: String): Intent {
            return Intent(context, InterruptActivity::class.java).apply {
                putExtra(EXTRA_KIND, kind)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_MESSAGE, message)
            }
        }
    }
}

@Composable
private fun InterruptScreen(
    kind: String,
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    val isAdult = kind == InterruptActivity.KIND_ADULT
    val isMindfulness = kind == InterruptActivity.KIND_MINDFULNESS
    val autoSeconds = if (isMindfulness || isAdult) 4 else 0
    var secondsRemaining by remember(kind, message) { mutableIntStateOf(autoSeconds) }

    LaunchedEffect(kind, message, autoSeconds) {
        if (autoSeconds <= 0) return@LaunchedEffect
        secondsRemaining = autoSeconds
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
        }
        onDismiss()
    }

    val background = when {
        isAdult -> Color(0xFF0F0406)
        isMindfulness -> Color(0xFF101426)
        else -> Color(0xFF140B1F)
    }
    val accent = when {
        isAdult -> Color(0xFFFF5252)
        isMindfulness -> Color(0xFF7C83FD)
        else -> Color(0xFFF59E0B)
    }
    val icon: ImageVector = if (isMindfulness) Icons.Rounded.Psychology else Icons.Rounded.Shield

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(46.dp)
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = Color(0xFFE7E7F0),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )
            if (autoSeconds > 0) {
                Spacer(modifier = Modifier.height(28.dp))
                val progress by animateFloatAsState(
                    targetValue = if (autoSeconds == 0) 0f else secondsRemaining / autoSeconds.toFloat(),
                    animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
                    label = "countdown"
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = accent,
                    trackColor = Color.White.copy(alpha = 0.12f),
                    strokeCap = StrokeCap.Round
                )
            } else {
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.interrupt_go_home),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}
