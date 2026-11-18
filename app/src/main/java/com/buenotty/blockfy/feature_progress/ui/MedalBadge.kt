package com.buenotty.blockfy.feature_progress.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WorkspacePremium
import com.buenotty.blockfy.feature_progress.MedalDef
import com.buenotty.blockfy.feature_progress.MedalGlyph
import com.buenotty.blockfy.feature_progress.MedalTier

private fun metalFor(tier: MedalTier): Metal = when (tier) {
    MedalTier.BRONZE -> Metals.Bronze
    MedalTier.SILVER -> Metals.Silver
    MedalTier.GOLD -> Metals.Gold
    MedalTier.PLATINUM -> Metals.Platinum
    MedalTier.DIAMOND -> Metals.Diamond
}

private fun ribbonFor(tier: MedalTier): Color = when (tier) {
    MedalTier.DIAMOND, MedalTier.PLATINUM -> Color(0xFF2B4FC4)
    else -> Color(0xFFB3302B)
}

private fun iconFor(glyph: MedalGlyph): ImageVector = when (glyph) {
    MedalGlyph.SUN -> Icons.Rounded.WbSunny
    MedalGlyph.FLAME -> Icons.Rounded.LocalFireDepartment
    MedalGlyph.SHIELD -> Icons.Rounded.Shield
    MedalGlyph.BOLT -> Icons.Rounded.Bolt
    MedalGlyph.WALL -> Icons.Rounded.Security
    MedalGlyph.PHOENIX -> Icons.Rounded.AutoAwesome
    MedalGlyph.CLOCK -> Icons.Rounded.Timer
    MedalGlyph.STAR -> Icons.Rounded.Star
    MedalGlyph.CROWN -> Icons.Rounded.WorkspacePremium
}

/**
 * A medal on a ribbon: a ring in the metal of its tier (bronze to diamond), a dark face with a
 * glint, and a picture in the middle. Locked medals show the same shape in dull steel with a lock.
 */
@Composable
fun MedalBadge(def: MedalDef, unlocked: Boolean, size: Dp = 84.dp, modifier: Modifier = Modifier) {
    val metal = if (unlocked) metalFor(def.tier) else Metals.Locked
    val ribbon = if (unlocked) ribbonFor(def.tier) else Color(0xFF3A404E)
    Box(modifier = modifier.size(size), contentAlignment = Alignment.TopCenter) {
        Canvas(modifier = Modifier.size(size)) {
            val s = this.size.minDimension
            drawRibbon(s, ribbon)
            drawMedalFace(s, metal)
        }
        Box(modifier = Modifier.size(size * 0.62f), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (unlocked) iconFor(def.glyph) else Icons.Rounded.Lock,
                contentDescription = null,
                tint = if (unlocked) metal.accent else metal.accent.copy(alpha = 0.7f),
                modifier = Modifier.size(size * 0.34f)
            )
        }
    }
}

private fun DrawScope.drawRibbon(s: Float, color: Color) {
    for (side in listOf(-1f, 1f)) {
        val x0 = s * 0.5f + side * s * 0.10f
        val path = Path().apply {
            moveTo(x0 - s * 0.10f, s * 0.55f)
            lineTo(x0 + s * 0.10f, s * 0.55f)
            lineTo(x0 + side * s * 0.14f + s * 0.07f, s * 0.97f)
            lineTo(x0 + side * s * 0.14f, s * 0.88f)
            lineTo(x0 + side * s * 0.14f - s * 0.07f, s * 0.97f)
            close()
        }
        drawPath(path, Brush.verticalGradient(listOf(color, color.copy(alpha = 0.7f)), s * 0.55f, s * 0.97f))
    }
}

private fun DrawScope.drawMedalFace(s: Float, metal: Metal) {
    val center = Offset(s * 0.5f, s * 0.38f)
    val radius = s * 0.36f
    drawCircle(Color.Black.copy(alpha = 0.3f), radius, Offset(center.x + s * 0.012f, center.y + s * 0.02f))
    drawCircle(
        Brush.linearGradient(listOf(metal.light, metal.mid, metal.dark), Offset(center.x - radius, center.y - radius), Offset(center.x + radius, center.y + radius)),
        radius, center
    )
    // Notched edge: short ticks around the rim.
    for (i in 0 until 28) {
        val a = Math.PI * 2 * i / 28
        val inner = radius * 0.93f
        val outer = radius * 0.995f
        drawLine(
            metal.dark.copy(alpha = 0.55f),
            Offset(center.x + (inner * Math.cos(a)).toFloat(), center.y + (inner * Math.sin(a)).toFloat()),
            Offset(center.x + (outer * Math.cos(a)).toFloat(), center.y + (outer * Math.sin(a)).toFloat()),
            strokeWidth = s * 0.012f
        )
    }
    val face = radius * 0.78f
    drawCircle(Brush.radialGradient(listOf(metal.dark.copy(alpha = 0.85f), Color.Black.copy(alpha = 0.95f)), center, face), face, center)
    drawCircle(metal.light.copy(alpha = 0.6f), face, center, style = Stroke(width = s * 0.012f))
    // Glint.
    val glint = Path().apply {
        addArc(
            androidx.compose.ui.geometry.Rect(center.x - radius * 0.86f, center.y - radius * 0.86f, center.x + radius * 0.86f, center.y + radius * 0.86f),
            200f, 60f
        )
    }
    drawPath(glint, Color.White.copy(alpha = 0.55f), style = Stroke(width = s * 0.022f, cap = StrokeCap.Round))
}
