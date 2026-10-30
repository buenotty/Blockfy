package com.buenotty.blockfy.feature_progress.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
    val Bronze = Metal(Color(0xFFF6C99A), Color(0xFFC07A3C), Color(0xFF5A3216), Color(0xFFFFE6C8))
    val Silver = Metal(Color(0xFFFFFFFF), Color(0xFFBFC7D3), Color(0xFF566070), Color(0xFFF6F9FD))
    val Gold = Metal(Color(0xFFFFF0B3), Color(0xFFF5B82A), Color(0xFF8A5A00), Color(0xFFFFF7D6))
    val Platinum = Metal(Color(0xFFF0FDFF), Color(0xFF86D6EC), Color(0xFF2A7088), Color(0xFFEAFBFF))
    val Diamond = Metal(Color(0xFFE6F0FF), Color(0xFF6A9BFF), Color(0xFF17358E), Color(0xFFEEF4FF))
    val Obsidian = Metal(Color(0xFF7A7A92), Color(0xFF30303E), Color(0xFF0A0A12), Color(0xFFFFCB45))
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
 * The rank crest. Every layer is drawn by hand: a soft shadow, a bevelled rim in the rank's metal
 * with a sheen, a dark engraved plate with a double border, and an embossed emblem centred on it
 * (stars for the rank, chevrons for the sub-rank I, II, III). From Major a laurel wreath curves
 * round the shield; Colonel and General add rays behind it. One small star above marks each
 * prestige. A common person gets a plain silhouette, so the first promotion is a real change.
 */
@Composable
fun RankBadge(rank: RankState, size: Dp = 120.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension
        if (rank.rankIndex < 0) {
            drawCommonPerson(s)
        } else {
            val metal = Metals.forRank(rank.rankIndex)
            if (rank.rankIndex >= 8) drawRays(s, metal, if (rank.rankIndex == 9) 14 else 10)
            if (rank.rankIndex >= 6) drawLaurel(s, metal)
            drawShield(s, metal)
            drawEmblem(s, rank, metal)
        }
        if (rank.prestige > 0) drawPrestigeStars(s, minOf(rank.prestige, MAX_PRESTIGE_STARS))
    }
}

private const val MAX_PRESTIGE_STARS = 7

/** Heater shield, in 0..1 of the box, centred with room around it for stars, laurel and rays. */
private fun shieldPath(s: Float, inset: Float = 0f): Path {
    val l = s * (0.20f + inset)
    val r = s * (0.80f - inset)
    val t = s * (0.14f + inset)
    val shoulder = s * (0.55f - inset * 0.4f)
    val tip = s * (0.93f - inset * 1.3f)
    val mid = s * 0.5f
    return Path().apply {
        moveTo(l, t + s * 0.03f)
        quadraticTo(mid, t - s * 0.045f, r, t + s * 0.03f)
        lineTo(r, shoulder)
        cubicTo(r, shoulder + (tip - shoulder) * 0.55f, mid + (r - mid) * 0.55f, tip - (tip - shoulder) * 0.22f, mid, tip)
        cubicTo(mid - (r - mid) * 0.55f, tip - (tip - shoulder) * 0.22f, l, shoulder + (tip - shoulder) * 0.55f, l, shoulder)
        close()
    }
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
    drawPath(shield, Color(0xFF7C8496), style = Stroke(width = s * 0.018f, join = StrokeJoin.Round))
    val tone = Color(0xFF8B93A6)
    drawCircle(tone, radius = s * 0.095f, center = Offset(s * 0.5f, s * 0.40f))
    val shoulders = Path().apply {
        moveTo(s * 0.32f, s * 0.70f)
        cubicTo(s * 0.32f, s * 0.52f, s * 0.68f, s * 0.52f, s * 0.68f, s * 0.70f)
        close()
    }
    drawPath(shoulders, tone)
}

private fun DrawScope.drawShield(s: Float, metal: Metal) {
    val rim = shieldPath(s)
    // Soft shadow, three offsets so the edge is not hard.
    for ((i, a) in listOf(0.22f, 0.14f, 0.08f).withIndex()) {
        withTransform({ translate(left = s * 0.006f * (i + 1), top = s * 0.012f * (i + 1)) }) { drawPath(rim, Color.Black.copy(alpha = a)) }
    }
    // Rim: metal that is light at the top-left and dark at the bottom-right.
    drawPath(rim, Brush.linearGradient(listOf(metal.light, metal.mid, metal.dark), Offset(s * 0.2f, s * 0.1f), Offset(s * 0.8f, s * 0.95f)))
    // Bevel: a bright edge on the upper left, a dark one on the lower right.
    drawPath(rim, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.75f), Color.Transparent, Color.Black.copy(alpha = 0.55f)), Offset(s * 0.2f, s * 0.1f), Offset(s * 0.8f, s * 0.95f)), style = Stroke(width = s * 0.016f, join = StrokeJoin.Round))
    // Sheen: a diagonal band of light across the rim.
    withTransform({ clipPath(rim) }) {
        drawRect(
            Brush.linearGradient(
                0.30f to Color.Transparent, 0.42f to Color.White.copy(alpha = 0.34f), 0.50f to Color.Transparent,
                start = Offset(s * 0.2f, s * 0.15f), end = Offset(s * 0.8f, s * 0.75f)
            )
        )
    }
    // Plate.
    val plate = shieldPath(s, inset = 0.034f)
    drawPath(plate, Brush.verticalGradient(listOf(metal.dark.copy(alpha = 0.78f), Color(0xFF07070C)), startY = s * 0.14f, endY = s * 0.93f))
    // A soft glow behind the emblem so it lifts off the plate.
    withTransform({ clipPath(plate) }) {
        drawCircle(Brush.radialGradient(listOf(metal.mid.copy(alpha = 0.30f), Color.Transparent), Offset(s * 0.5f, s * 0.50f), s * 0.30f), s * 0.30f, Offset(s * 0.5f, s * 0.50f))
    }
    // Engraved double border.
    drawPath(plate, metal.light.copy(alpha = 0.7f), style = Stroke(width = s * 0.010f, join = StrokeJoin.Round))
    drawPath(shieldPath(s, inset = 0.056f), metal.mid.copy(alpha = 0.55f), style = Stroke(width = s * 0.006f, join = StrokeJoin.Round))
}

/** Stars for the rank above, chevrons for the sub-rank below, the pair centred on the plate. */
private fun DrawScope.drawEmblem(s: Float, rank: RankState, metal: Metal) {
    val stars = (rank.rankIndex + 1) / 2
    val chevrons = rank.sub + 1
    val starSize = when { stars <= 2 -> 0.062f; stars <= 3 -> 0.054f; else -> 0.046f }
    val starBlock = if (stars > 0) starSize * 2f else 0f
    val gap = if (stars > 0) 0.035f else 0f
    val step = 0.098f
    val chevronBlock = step * (chevrons - 1) + 0.085f
    val total = starBlock + gap + chevronBlock
    val top = 0.50f - total / 2f - 0.01f

    if (stars > 0) {
        val spacing = starSize * 2.25f
        for (i in 0 until stars) {
            val cx = 0.5f + (i - (stars - 1) / 2f) * spacing
            val cy = top + starSize
            val star = starPath(s * cx, s * cy, s * starSize, s * starSize * 0.44f)
            withTransform({ translate(top = s * 0.006f) }) { drawPath(star, Color.Black.copy(alpha = 0.6f)) }
            drawPath(star, Brush.verticalGradient(listOf(metal.accent, metal.mid), s * (cy - starSize), s * (cy + starSize)))
            drawPath(star, metal.light.copy(alpha = 0.8f), style = Stroke(width = s * 0.004f, join = StrokeJoin.Round))
        }
    }
    val chevronTop = top + starBlock + gap
    for (i in 0 until chevrons) {
        val y = s * (chevronTop + i * step)
        val half = s * 0.215f
        val path = Path().apply {
            moveTo(s * 0.5f - half, y + s * 0.085f)
            lineTo(s * 0.5f, y)
            lineTo(s * 0.5f + half, y + s * 0.085f)
        }
        val w = s * 0.052f
        withTransform({ translate(top = s * 0.008f) }) { drawPath(path, Color.Black.copy(alpha = 0.65f), style = Stroke(w * 1.15f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
        drawPath(path, metal.dark, style = Stroke(w * 1.12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, Brush.horizontalGradient(listOf(metal.mid, metal.accent, metal.mid), s * 0.5f - half, s * 0.5f + half), style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
        withTransform({ translate(top = -s * 0.009f) }) { drawPath(path, Color.White.copy(alpha = 0.45f), style = Stroke(w * 0.22f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    }
}

/** A laurel branch on each side, leaves lying along the curve of the shield. */
private fun DrawScope.drawLaurel(s: Float, metal: Metal) {
    val cx = s * 0.5f
    val cy = s * 0.55f
    val radius = s * 0.40f
    val length = s * 0.080f
    val width = s * 0.030f
    val body = if (metal === Metals.Obsidian) listOf(metal.accent.copy(alpha = 0.6f), metal.accent) else listOf(metal.dark, metal.mid, metal.light)
    for (side in listOf(-1f, 1f)) {
        for (i in 0 until 9) {
            val phi = (22.0 + i * 11.5) * PI / 180.0
            // Point on the arc, going up the side; tangent points up the arc, normal points outwards.
            val ax = cx + side * radius * sin(phi).toFloat()
            val ay = cy + radius * cos(phi).toFloat()
            val tx = side * cos(phi).toFloat()
            val ty = -sin(phi).toFloat()
            val nx = side * sin(phi).toFloat()
            val ny = cos(phi).toFloat()
            for (face in listOf(-1f, 1f)) {
                val bx = ax + nx * face * s * 0.010f
                val by = ay + ny * face * s * 0.010f
                val ex = bx + (tx + nx * face * 0.45f) * length
                val ey = by + (ty + ny * face * 0.45f) * length
                val mx = (bx + ex) / 2f
                val my = (by + ey) / 2f
                val dx = ex - bx
                val dy = ey - by
                val len = kotlin.math.sqrt(dx * dx + dy * dy)
                val px = -dy / len * width
                val py = dx / len * width
                val leaf = Path().apply {
                    moveTo(bx, by)
                    quadraticTo(mx + px, my + py, ex, ey)
                    quadraticTo(mx - px, my - py, bx, by)
                }
                drawPath(leaf, Brush.linearGradient(body, Offset(bx, by), Offset(ex, ey)))
                drawPath(leaf, metal.dark.copy(alpha = 0.55f), style = Stroke(width = s * 0.003f))
            }
        }
    }
}

/** Rays behind the crest for the top ranks. */
private fun DrawScope.drawRays(s: Float, metal: Metal, count: Int) {
    val cx = s * 0.5f
    val cy = s * 0.52f
    for (i in 0 until count) {
        val a = 2 * PI * i / count - PI / 2
        val half = PI / count * 0.42
        val inner = s * 0.34f
        val outer = s * (if (i % 2 == 0) 0.52f else 0.44f)
        val ray = Path().apply {
            moveTo(cx + (inner * cos(a - half)).toFloat(), cy + (inner * sin(a - half)).toFloat())
            lineTo(cx + (outer * cos(a)).toFloat(), cy + (outer * sin(a)).toFloat())
            lineTo(cx + (inner * cos(a + half)).toFloat(), cy + (inner * sin(a + half)).toFloat())
            close()
        }
        drawPath(ray, Brush.radialGradient(listOf(metal.accent.copy(alpha = 0.0f), metal.accent.copy(alpha = 0.55f)), Offset(cx, cy), outer))
    }
}

private fun DrawScope.drawPrestigeStars(s: Float, count: Int) {
    val spread = if (count == 1) 0.0 else 76.0
    for (i in 0 until count) {
        val t = if (count == 1) 0.0 else -spread / 2 + spread * i / (count - 1)
        val a = (-90.0 + t) * PI / 180.0
        val cx = s * 0.5f + (s * 0.46f * cos(a)).toFloat()
        val cy = s * 0.60f + (s * 0.53f * sin(a)).toFloat()
        val star = starPath(cx, cy, s * 0.048f, s * 0.021f)
        drawPath(star, Color.Black.copy(alpha = 0.5f), style = Stroke(width = s * 0.012f, join = StrokeJoin.Round))
        drawPath(star, Brush.verticalGradient(listOf(Metals.Gold.light, Metals.Gold.mid), cy - s * 0.05f, cy + s * 0.05f))
    }
}
