package com.robingebert.blokky.feature_preferences.ui.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.robingebert.blokky.R

@Composable
fun BlockfyThemedAppIcon(appName: String, modifier: Modifier = Modifier) {
    val (drawableRes, iconTint) = when (appName) {
        "Instagram" -> Pair(R.drawable.ic_instagram_themed, Color(0xFFC084FC)) // Electric Violet
        "YouTube" -> Pair(R.drawable.ic_youtube_themed, Color(0xFF818CF8)) // Periwinkle Indigo
        "TikTok" -> Pair(R.drawable.ic_tiktok_themed, Color(0xFF67E8F9)) // Cyber Cyan
        "Facebook" -> Pair(R.drawable.ic_facebook_themed, Color(0xFF60A5FA)) // Frost Blue
        "X" -> Pair(R.drawable.ic_x_themed, Color(0xFFF1F5F9)) // Ice White
        else -> Pair(R.drawable.ic_blockfy_logo, Color.White)
    }

    Box(
        modifier = modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1B2038),
                        Color(0xFF0F1322)
                    )
                )
            )
            .border(
                BorderStroke(1.dp, Color(0xFF2E385D)),
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(drawableRes),
            contentDescription = appName,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
    }
}
