package com.robingebert.blokky.feature_preferences.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
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
    leadingIcon: @Composable (() -> Unit),
    settingsIcon: @Composable ((Modifier) -> Unit)? = null,
    onValueChange: (Boolean) -> Unit,
) {

    var showDisableBlockerDialog by remember { mutableStateOf(false) }

    fun edit(newValue: Boolean, force: Boolean = false) {
        if (!force) {
            if (!newValue) {
                showDisableBlockerDialog = true
                return
            }
        }
        onValueChange(newValue)
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .toggleable(
                value = value,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = { edit(it) }
            ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon()
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = value,
                onCheckedChange = { edit(it) },
                enabled = enabled,
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = com.robingebert.blokky.ui.theme.BlockfyPrimary,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surface
                )
            )
            Spacer(modifier = Modifier.width(10.dp))
            VerticalDivider(modifier = Modifier.height(36.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.width(6.dp))
            settingsIcon?.invoke(Modifier.size(28.dp))
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
fun SwitchPreview(){
    SwitchPreference(
        value = true,
        title = "Test",
        summary = "Test preference summary",
        enabled = true,
        leadingIcon = { Icon(imageVector = Icons.Rounded.Numbers, contentDescription = null)},
        settingsIcon = {
            Icon(
                modifier = it,
                imageVector = Icons.Rounded.Settings,
                contentDescription = null
            )
        }
    ) { }
}