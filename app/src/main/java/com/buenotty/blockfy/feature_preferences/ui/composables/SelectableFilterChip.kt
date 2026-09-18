package com.buenotty.blockfy.feature_preferences.ui.composables

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun SelectableFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
        colors = selectableFilterChipColors(),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = Color.Transparent,
            disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f),
            disabledSelectedBorderColor = Color.Transparent
        )
    )
}

@Composable
fun selectableFilterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    labelColor = MaterialTheme.colorScheme.onSurface,
    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
    selectedTrailingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
    disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    disabledSelectedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
)
