package com.buenotty.blockfy.feature_progress.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.buenotty.blockfy.feature_progress.RankState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Colours of one metal: a highlight, the body, the shadow, and a bright accent for the details. */
data class Metal(val light: Color, val mid: Color, val dark: Color, val accent: Color)

object Metals {
    val Bronze = Metal(Color(0xFFF0BE8A), Color(0xFFB87333), Color(0xFF63391A), Color(0xFFFFE2BF))
    val Silver = Metal(Color(0xFFFFFFFF), Color(0xFFB9C1CD), Color(0xFF5E6877), Color(0xFFF4F7FB))
    val Gold = Metal(Color(0xFFFFE9A3), Color(0xFFF2B01E), Color(0xFF8F5F00), Color(0xFFFFF4CC))
    val Platinum = Metal(Color(0xFFEAFBFF), Color(0xFF7FCFE6), Color(0xFF2F7690), Color(0xFFE6FAFF))
    val Diamond = Metal(Color(0xFFDDEBFF), Color(0xFF5C8DFF), Color(0xFF1B3A94), Color(0xFFEAF2FF))
    val Obsidian = Metal(Color(0xFF6A6A80), Color(0xFF2A2A36), Color(0xFF0C0C14), Color(0xFFFFC83D))
    val Locked = Metal(Color(0xFF5A6070), Color(0xFF3A404E), Color(0xFF242833), Color(0xFF7C8496))

    /** Ten ranks climb through the metals: bronze, silver, gold, platinum, diamond, obsidian. */
    fun forRank(rankIndex: Int): Metal = when (rankIndex) {
        0, 1 -> Bronze
        2, 3 -> Silver
        4, 5 -> Gold
        6, 7 -> Platinum
        8 -> Diamond
        9 -> Obsidian
        else -> Locked
    }
}

/**
 * The rank crest: a shield in the metal of the rank, chevrons for the sub-rank (I, II, III), a
 * star from the middle ranks, laurel wings from Major up, and one small star above for every
 * prestige. A common person gets a plain silhouette, so the first promotion is a real change.
 */
@Composable
fun RankBadge(rank: RankState, size: Dp = 120.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension
        if (rank.prestige > 0) drawPrestigeStars(s, minOf(rank.prestige, MAX_PRESTIGE_STARS))
        withTransform({
            translate(left = s * 0.11f, top = s * 0.15f)
            scale(0.78f, 0.78f, pivot = Offset.Zero)
        }) {
            if (rank.rankIndex < 0) drawCommonPerson(s) else drawCrest(s, rank, Metals.forRank(rank.rankIndex))
        }
    }
}

private const val MAX_PRESTIGE_STARS = 7

private fun shieldPath(s: Float): Path = Path().apply {
    moveTo(s * 0.10f, s * 0.08f)
    lineTo(s * 0.90f, s * 0.08f)
    lineTo(s * 0.90f, s * 0.52f)
    cubicTo(s * 0.90f, s * 0.76f, s * 0.68f, s * 0.90f, s * 0.50f, s * 0.99f)
    cubicTo(s * 0.32f, s * 0.90f, s * 0.10f, s * 0.76f, s * 0.10f, s * 0.52f)
    close()
}

private fun starPath(cx: Float, cy: Float, outer: Float, inner: Float): Path = Path().apply {
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val a = -PI / 2 + i * PI / 5
        val x = cx + (r * cos(a)).toFloat()
        val y = cy + (r * sin(a)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun DrawScope.drawCommonPerson(s: Float) {
    val shield = shieldPath(s)
    drawPath(shield, Brush.verticalGradient(listOf(Color(0xFF3A404E), Color(0xFF242833))))
    drawPath(shield, Color(0xFF7C8496), style = Stroke(width = s * 0.02f, join = StrokeJoin.Round))
    val tone = Color(0xFF8B93A6)
    drawCircle(tone, radius = s * 0.12f, center = Offset(s * 0.5f, s * 0.36f))
    val shoulders = Path().apply {
        moveTo(s * 0.26f, s * 0.74f)
        cubicTo(s * 0.26f, s * 0.54f, s * 0.74f, s * 0.54f, s * 0.74f, s * 0.74f)
        close()
    }
    drawPath(shoulders, tone)
}

private fun DrawScope.drawCrest(s: Float, rank: RankState, metal: Metal) {
    if (rank.rankIndex >= 6) drawWings(s, metal)

    val shield = shieldPath(s)
    // Soft shadow so the crest lifts off the card.
    withTransform({ translate(left = s * 0.012f, top = s * 0.02f) }) {
        drawPath(shield, Color.Black.copy(alpha = 0.35f))
    }
    // Rim in the metal.
    drawPath(shield, Brush.linearGradient(listOf(metal.light, metal.mid, metal.dark), Offset(0f, 0f), Offset(s, s)))
    // Plate.
    withTransform({ scale(0.84f, 0.84f, pivot = Offset(s * 0.5f, s * 0.48f)) }) {
        drawPath(
            shield,
            Brush.verticalGradient(listOf(metal.dark.copy(alpha = 0.95f), Color.Black.copy(alpha = 0.92f)), startY = 0f, endY = s)
        )
        drawPath(shield, metal.light.copy(alpha = 0.55f), style = Stroke(width = s * 0.014f, join = StrokeJoin.Round))
    }

    // Star from Sergeant up (more weight the higher the rank).
    if (rank.rankIndex >= 3) {
        val outer = s * (0.075f + 0.005f * rank.rankIndex)
        drawPath(starPath(s * 0.5f, s * 0.30f, outer, outer * 0.45f), Brush.verticalGradient(listOf(metal.accent, metal.mid), s * 0.2f, s * 0.4f))
    }

    // Chevrons: one, two or three for I, II, III.
    val chevrons = rank.sub + 1
    val firstY = if (rank.rankIndex >= 3) s * 0.44f else s * 0.34f
    for (i in 0 until chevrons) {
        val y = firstY + i * s * 0.105f
        val path = Path().apply {
            moveTo(s * 0.27f, y + s * 0.095f)
            lineTo(s * 0.50f, y)
            lineTo(s * 0.73f, y + s * 0.095f)
        }
        drawPath(path, metal.dark, style = Stroke(width = s * 0.062f, cap = StrokeCap.Round, join = StrokeJoin.Round), alpha = 0.7f)
        drawPath(
            path, Brush.horizontalGradient(listOf(metal.mid, metal.accent, metal.mid), s * 0.27f, s * 0.73f),
            style = Stroke(width = s * 0.045f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    // Glint across the top-left of the rim.
    drawPath(shield, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent), Offset(0f, 0f), Offset(s * 0.6f, s * 0.6f)))
}

private fun DrawScope.drawWings(s: Float, metal: Metal) {
    for (side in listOf(-1f, 1f)) {
        for (i in 0 until 5) {
            val cx = s * 0.5f + side * s * (0.50f - i * 0.012f)
            val cy = s * (0.30f + i * 0.105f)
            val angle = side * (-35f + i * 14f)
            rotate(angle, pivot = Offset(cx, cy)) {
                val leaf = Size(s * 0.15f, s * 0.062f)
                // The darkest metal would vanish against the card, so its leaves take the accent.
                val body = if (metal === Metals.Obsidian) listOf(Metals.Obsidian.accent.copy(alpha = 0.55f), Metals.Obsidian.accent) else listOf(metal.dark, metal.mid, metal.light)
                drawOval(
                    Brush.horizontalGradient(body),
                    topLeft = Offset(cx - leaf.width / 2, cy - leaf.height / 2),
                    size = leaf
                )
            }
        }
    }
}

private fun DrawScope.drawPrestigeStars(s: Float, count: Int) {
    val spread = if (count == 1) 0.0 else 70.0
    for (i in 0 until count) {
        val t = if (count == 1) 0.0 else -spread / 2 + spread * i / (count - 1)
        val a = (-90.0 + t) * PI / 180.0
        val cx = s * 0.5f + (s * 0.46f * cos(a)).toFloat()
        val cy = s * 0.58f + (s * 0.50f * sin(a)).toFloat()
        drawPath(starPath(cx, cy, s * 0.055f, s * 0.025f), Brush.verticalGradient(listOf(Metals.Gold.light, Metals.Gold.mid), cy - s * 0.05f, cy + s * 0.05f))
    }
}
