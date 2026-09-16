package com.buenotty.blockfy.feature_preferences.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.buenotty.blockfy.R

enum class TrackedAppIcon {
    Instagram,
    YouTube,
    TikTok,
    Facebook,
    X
}

fun trackedAppIcon(name: String): TrackedAppIcon {
    return when (name) {
        "YouTube" -> TrackedAppIcon.YouTube
        "TikTok" -> TrackedAppIcon.TikTok
        "Facebook" -> TrackedAppIcon.Facebook
        "X" -> TrackedAppIcon.X
        else -> TrackedAppIcon.Instagram
    }
}

@Composable
fun AppBrandIcon(
    app: TrackedAppIcon,
    modifier: Modifier = Modifier,
    wellSize: Dp = 40.dp,
    glyphSize: Dp = 22.dp,
) {
    val res = when (app) {
        TrackedAppIcon.Instagram -> R.drawable.ic_instagram
        TrackedAppIcon.YouTube -> R.drawable.ic_youtube
        TrackedAppIcon.TikTok -> R.drawable.ic_tiktok_themed
        TrackedAppIcon.Facebook -> R.drawable.ic_facebook_themed
        TrackedAppIcon.X -> R.drawable.ic_x_themed
    }
    TintedGlyph(
        modifier = modifier,
        wellSize = wellSize,
        glyphSize = glyphSize
    ) {
        Icon(
            painter = painterResource(res),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(glyphSize)
        )
    }
}

@Composable
fun TintedGlyph(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    wellSize: Dp = 40.dp,
    glyphSize: Dp = 22.dp,
) {
    TintedGlyph(modifier = modifier, wellSize = wellSize, glyphSize = glyphSize) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(glyphSize)
        )
    }
}

@Composable
fun TintedGlyph(
    modifier: Modifier = Modifier,
    wellSize: Dp = 40.dp,
    glyphSize: Dp = 22.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(wellSize)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
