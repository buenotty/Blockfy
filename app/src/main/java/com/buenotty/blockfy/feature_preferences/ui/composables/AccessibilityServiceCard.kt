package com.buenotty.blockfy.feature_preferences.ui.composables

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.buenotty.blockfy.R

@Composable
fun AccessibilityServiceCard(
    isAccessibilityGranted: Boolean
) {
    var showAccessibilityServiceDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val update: (Boolean) -> Unit = {
        if (isAccessibilityGranted) {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } else {
            showAccessibilityServiceDialog = true
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (!isAccessibilityGranted) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                Color(0x807DEF87)
            }
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(15.dp))
            .toggleable(
                value = isAccessibilityGranted,
                role = Role.Switch,
                onValueChange = update
            ),
        shape = RoundedCornerShape(15.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.Rounded.Accessibility,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.accessibility_service),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (isAccessibilityGranted) {
                        stringResource(R.string.accessibility_active)
                    } else {
                        stringResource(R.string.accessibility_inactive)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Switch(
                checked = isAccessibilityGranted,
                onCheckedChange = { update(it) }
            )
        }
    }

    if (showAccessibilityServiceDialog) {
        AccessibilityServiceDialog {
            showAccessibilityServiceDialog = false
        }
    }
}

@Composable
fun AccessibilityServiceDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var site by remember { mutableIntStateOf(0) }
    var dialogHeightPx by remember { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current

    Dialog(onDismissRequest = onDismissRequest) {
        val baseModifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                if (dialogHeightPx == null) {
                    dialogHeightPx = coords.size.height
                }
            }
        val sizedModifier = if (dialogHeightPx != null) {
            baseModifier.height(with(density) { dialogHeightPx!!.toDp() })
        } else {
            baseModifier.wrapContentHeight()
        }

        Card(
            modifier = sizedModifier,
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.accessibility_dialog_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    AnimatedVisibility(
                        visible = site == 0,
                        exit = slideOutHorizontally() + fadeOut()
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(R.string.accessibility_dialog_desc),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(R.string.restricted_settings_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Text(stringResource(R.string.btn_open_app_info), color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = onDismissRequest) {
                                    Text(stringResource(R.string.accessibility_dialog_decline))
                                }
                                TextButton(onClick = { site = 1 }) {
                                    Text(stringResource(R.string.accessibility_dialog_accept))
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = site == 1,
                        enter = slideInHorizontally {
                            with(density) { -40.dp.roundToPx() }
                        } + fadeIn(initialAlpha = 0.3f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(R.string.accessibility_dialog_howto),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                    onDismissRequest()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(R.string.btn_open_accessibility))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                        contentDescription = null
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

fun android.content.Context.isAccessibilityGranted(): Boolean {
    val expectedId = "$packageName/.feature_accessibility.ReelsBlockAccessibilityService"
    val am = getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val runningServices = am.getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK)
    if (runningServices.any { it.id == expectedId || it.id.endsWith("ReelsBlockAccessibilityService") }) {
        return true
    }
    val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    return enabled?.contains("ReelsBlockAccessibilityService") == true
}

@Preview
@Composable
fun AccessibilityServiceCardPreview() {
    AccessibilityServiceCard(false)
}
