package com.buenotty.blockfy.feature_preferences.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SwitchPreference(
    value: Boolean,
    title: String,
    summary: String,
    enabled: Boolean = true,
    grouped: Boolean = false,
    showDivider: Boolean = false,
    leadingIcon: @Composable (() -> Unit),
    settingsIcon: @Composable ((Modifier) -> Unit)? = null,
    confirmDisable: Boolean = true,
    onValueChange: (Boolean) -> Unit,
) {
    var showDisableBlockerDialog by remember { mutableStateOf(false) }

    fun edit(newValue: Boolean, force: Boolean = false) {
        if (!force && confirmDisable && !newValue) {
            showDisableBlockerDialog = true
            return
        }
        onValueChange(newValue)
    }

    val row = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (grouped) {
                        Modifier.toggleable(
                            value = value,
                            enabled = enabled,
                            role = Role.Switch,
                            onValueChange = { edit(it) }
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon()
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    }
                )
            }
            Switch(
                checked = value,
                onCheckedChange = { edit(it) },
                enabled = enabled
            )
            if (settingsIcon != null) {
                Spacer(modifier = Modifier.width(4.dp))
                VerticalDivider(modifier = Modifier.height(28.dp))
                settingsIcon(Modifier)
            }
        }
    }

    if (grouped) {
        Column(modifier = Modifier.fillMaxWidth()) {
            row()
            if (showDivider) PreferenceDivider()
        }
    } else {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .toggleable(
                    value = value,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = { edit(it) }
                ),
            shape = RoundedCornerShape(16.dp)
        ) {
            row()
        }
    }

    if (showDisableBlockerDialog) {
        DisableBlockerDialog(
            onDismissRequest = { showDisableBlockerDialog = false },
            onConfirmation = {
                showDisableBlockerDialog = false
                edit(newValue = false, force = true)
            }
        )
    }
}

@Preview
@Composable
fun SwitchPreview() {
    SwitchPreference(
        value = true,
        title = "Test",
        summary = "Test preference summary",
        enabled = true,
        leadingIcon = { Icon(imageVector = Icons.Rounded.Numbers, contentDescription = null) },
        settingsIcon = {
            Icon(
                modifier = it,
                imageVector = Icons.Rounded.Settings,
                contentDescription = null
            )
        }
    ) { }
}
