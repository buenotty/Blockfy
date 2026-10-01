package com.buenotty.blockfy.feature_preferences.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.buenotty.blockfy.R
import com.buenotty.blockfy.datastore.AppSettings
import com.buenotty.blockfy.datastore.DailyUsage
import com.buenotty.blockfy.feature_monitor.BlockPolicy
import com.buenotty.blockfy.feature_preferences.OverviewViewModel
import com.buenotty.blockfy.feature_preferences.repository.models.App
import com.buenotty.blockfy.feature_preferences.ui.composables.AppBrandIcon
import com.buenotty.blockfy.feature_preferences.ui.composables.DisableBlockerDialog
import com.buenotty.blockfy.feature_preferences.ui.composables.SectionTitle
import com.buenotty.blockfy.feature_preferences.ui.composables.SelectableFilterChip
import com.buenotty.blockfy.feature_preferences.ui.composables.StrictModeDialog
import com.buenotty.blockfy.feature_preferences.ui.composables.appDisplayName
import com.buenotty.blockfy.feature_preferences.ui.composables.daysLabel
import com.buenotty.blockfy.feature_preferences.ui.composables.hoursLabel
import com.buenotty.blockfy.feature_preferences.ui.composables.shortsFeatureLabel
import com.buenotty.blockfy.feature_preferences.ui.composables.trackedAppIcon
import org.koin.androidx.compose.koinViewModel

private const val MAX_MINUTES = 1440
private const val MINUTES_STEP = 5
private val MINUTE_SHORTCUTS = listOf(15, 30, 45, 60, 90, 120)
private const val DEFAULT_WHOLE_MINUTES = 60
private const val DEFAULT_SHORTS_MINUTES = 15

/** What one rule does: nothing, block outright, or allow some minutes a day. */
private enum class RuleMode { OFF, BLOCK, LIMIT }

@Composable
fun EditAppScreen(
    appName: String,
    onClose: () -> Unit,
    onOpenSchedule: () -> Unit,
    viewModel: OverviewViewModel = koinViewModel()
) {
    val settings by viewModel.appSettings.collectAsState()
    val usage by viewModel.dailyUsage.collectAsState()
    val loaded by viewModel.isLoaded.collectAsState()

    if (!loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    EditAppForm(
        saved = BlockPolicy.appConfig(settings, appName),
        settings = settings,
        usage = usage,
        locked = settings.isStrictLocked(),
        onSave = { viewModel.updateApp(it) },
        onResetUsage = { viewModel.resetDailyUsage(appName) },
        onOpenSchedule = onOpenSchedule,
        onClose = onClose
    )
}

private fun modeOf(ruleOn: Boolean, limit: Int) = when {
    !ruleOn -> RuleMode.OFF
    limit > 0 -> RuleMode.LIMIT
    else -> RuleMode.BLOCK
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditAppForm(
    saved: App,
    settings: AppSettings,
    usage: DailyUsage,
    locked: Boolean,
    onSave: (App) -> Unit,
    onResetUsage: () -> Unit,
    onOpenSchedule: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val wholeOnly = BlockPolicy.isWholeAppOnly(saved.name)

    // The draft is remembered by app name only, so a background settings update (for example the
    // usage counter) can never wipe what the user is typing.
    var enabled by rememberSaveable(saved.name) { mutableStateOf(true) }
    var wholeMode by rememberSaveable(saved.name) {
        mutableStateOf(modeOf(BlockPolicy.wholeRuleOn(saved), saved.appTotalDailyLimitMinutes))
    }
    var wholeMinutes by rememberSaveable(saved.name) {
        mutableStateOf(saved.appTotalDailyLimitMinutes.takeIf { it > 0 } ?: DEFAULT_WHOLE_MINUTES)
    }
    var shortsMode by rememberSaveable(saved.name) {
        mutableStateOf(modeOf(BlockPolicy.shortsRuleOn(saved), saved.dailyLimitMinutes))
    }
    var shortsMinutes by rememberSaveable(saved.name) {
        mutableStateOf(saved.dailyLimitMinutes.takeIf { it > 0 } ?: DEFAULT_SHORTS_MINUTES)
    }

    var showLoosenConfirm by remember { mutableStateOf(false) }
    var showStrictDialog by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }

    val draft = saved.copy(
        blocked = enabled,
        wholeApp = wholeMode != RuleMode.OFF,
        appTotalDailyLimitMinutes = if (wholeMode == RuleMode.LIMIT) wholeMinutes else 0,
        shortsRuleOn = shortsMode != RuleMode.OFF,
        dailyLimitMinutes = if (shortsMode == RuleMode.LIMIT) shortsMinutes else 0
    )
    val dirty = draft != saved
    val minutesValid = (wholeMode != RuleMode.LIMIT || wholeMinutes in 1..MAX_MINUTES) &&
        (wholeOnly || shortsMode != RuleMode.LIMIT || shortsMinutes in 1..MAX_MINUTES)

    // Strict mode still lets the user make a block stricter; it only refuses to loosen one.
    val loosening = BlockPolicy.isLoosening(saved, draft)
    val canSave = dirty && minutesValid && !(locked && loosening)

    fun commit() {
        onSave(draft)
        Toast.makeText(context, R.string.saved_toast, Toast.LENGTH_SHORT).show()
        onClose()
    }

    fun requestClose() {
        if (dirty) showDiscard = true else onClose()
    }

    BackHandler(enabled = dirty) { showDiscard = true }

    Box(Modifier.fillMaxSize().imePadding()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppBrandIcon(trackedAppIcon(saved.name), wellSize = 36.dp, glyphSize = 20.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(appDisplayName(saved.name), fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = ::requestClose) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                        }
                    }
                )
            },
            bottomBar = {
                Surface(tonalElevation = 3.dp) {
                    Column(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                if (loosening) showLoosenConfirm = true else commit()
                            },
                            enabled = canSave,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    if (!saved.blocked && enabled) R.string.edit_save_and_enable else R.string.save_btn
                                ),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RuleSummaryCard(draft = draft, settings = settings)

                if (locked) LockedBanner(blocking = loosening)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.edit_enable_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                stringResource(R.string.edit_enable_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { wantsOn ->
                                when {
                                    wantsOn -> enabled = true
                                    locked && saved.blocked -> showStrictDialog = true
                                    else -> enabled = false
                                }
                            }
                        )
                    }
                }

                RuleCard(
                    title = stringResource(R.string.rule_whole_title),
                    description = stringResource(R.string.rule_whole_desc),
                    mode = wholeMode,
                    allowOff = !wholeOnly,
                    minutes = wholeMinutes,
                    onModeChange = { wholeMode = it },
                    onMinutesChange = { wholeMinutes = it },
                    onDone = { focusManager.clearFocus() },
                    usedSeconds = BlockPolicy.totalSeconds(usage, saved.name),
                    savedLimit = saved.appTotalDailyLimitMinutes,
                    canReset = !locked,
                    onReset = onResetUsage
                )

                if (!wholeOnly) {
                    RuleCard(
                        title = stringResource(R.string.rule_shorts_title, shortsFeatureLabel(saved.name)),
                        description = stringResource(R.string.rule_shorts_desc),
                        mode = shortsMode,
                        allowOff = true,
                        minutes = shortsMinutes,
                        onModeChange = { shortsMode = it },
                        onMinutesChange = { shortsMinutes = it },
                        onDone = { focusManager.clearFocus() },
                        usedSeconds = BlockPolicy.featureSeconds(usage, saved.name),
                        savedLimit = saved.dailyLimitMinutes,
                        canReset = !locked,
                        onReset = onResetUsage
                    )
                    if (wholeMode == RuleMode.LIMIT && shortsMode == RuleMode.LIMIT && shortsMinutes > wholeMinutes) {
                        Text(
                            stringResource(R.string.limits_hint_shorts_above_whole),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                ScheduleLinkCard(settings = settings, onOpen = onOpenSchedule)

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showLoosenConfirm) {
        DisableBlockerDialog(
            title = stringResource(R.string.loosen_title),
            message = stringResource(R.string.loosen_msg),
            confirmLabel = stringResource(R.string.loosen_confirm),
            onDismissRequest = { showLoosenConfirm = false },
            onConfirmation = {
                showLoosenConfirm = false
                commit()
            }
        )
    }
    if (showStrictDialog) {
        StrictModeDialog(onDismiss = { showStrictDialog = false })
    }
    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text(stringResource(R.string.discard_title)) },
            text = { Text(stringResource(R.string.discard_msg)) },
            confirmButton = {
                TextButton(onClick = { showDiscard = false; onClose() }) {
                    Text(stringResource(R.string.discard_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscard = false }) {
                    Text(stringResource(R.string.discard_keep))
                }
            }
        )
    }
}

@Composable
private fun RuleSummaryCard(draft: App, settings: AppSettings) {
    val name = appDisplayName(draft.name)
    val wholeOn = BlockPolicy.wholeRuleOn(draft)
    val shortsOn = BlockPolicy.shortsRuleOn(draft)
    val subject = stringResource(R.string.subject_shorts, name, shortsFeatureLabel(draft.name))

    val lines = buildList {
        if (!draft.blocked) {
            add(stringResource(R.string.summary_off, name))
        } else if (!wholeOn && !shortsOn) {
            add(stringResource(R.string.summary_none, name))
        } else {
            if (wholeOn) {
                add(
                    if (draft.appTotalDailyLimitMinutes > 0) {
                        stringResource(R.string.summary_whole_limit, name, draft.appTotalDailyLimitMinutes)
                    } else {
                        stringResource(R.string.summary_whole_block, name)
                    }
                )
            }
            if (shortsOn) {
                add(
                    if (draft.dailyLimitMinutes > 0) {
                        stringResource(R.string.summary_shorts_limit, subject, draft.dailyLimitMinutes)
                    } else {
                        stringResource(R.string.summary_shorts_block, subject)
                    }
                )
            }
            add(
                stringResource(
                    R.string.summary_schedule,
                    daysLabel(settings.scheduleWeekdays),
                    hoursLabel(settings.scheduleStart, settings.scheduleEnd)
                )
            )
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.summary_title), style = MaterialTheme.typography.labelLarge)
            lines.forEachIndexed { index, line ->
                Text(
                    line,
                    style = if (index == 0) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                    fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun LockedBanner(blocking: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (blocking) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (blocking) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.strict_locked_banner), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ScheduleLinkCard(settings: AppSettings, onOpen: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.schedule_link_title))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${daysLabel(settings.scheduleWeekdays)} · ${hoursLabel(settings.scheduleStart, settings.scheduleEnd)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.schedule_link_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onOpen) { Text(stringResource(R.string.schedule_link_btn)) }
            }
        }
    }
}

/** One rule: off, block, or a daily limit with a hard-to-get-wrong minutes picker. */
@Composable
private fun RuleCard(
    title: String,
    description: String,
    mode: RuleMode,
    allowOff: Boolean,
    minutes: Int,
    onModeChange: (RuleMode) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onDone: () -> Unit,
    usedSeconds: Long,
    savedLimit: Int,
    canReset: Boolean,
    onReset: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val modes = if (allowOff) listOf(RuleMode.OFF, RuleMode.BLOCK, RuleMode.LIMIT) else listOf(RuleMode.BLOCK, RuleMode.LIMIT)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    modes.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = mode == option,
                            onClick = { onModeChange(option) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size)
                        ) {
                            Text(
                                stringResource(
                                    when (option) {
                                        RuleMode.OFF -> R.string.mode_off
                                        RuleMode.BLOCK -> R.string.edit_rule_always
                                        RuleMode.LIMIT -> R.string.edit_rule_limit
                                    }
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
                Text(
                    stringResource(
                        when (mode) {
                            RuleMode.OFF -> R.string.edit_rule_off_desc
                            RuleMode.BLOCK -> R.string.edit_rule_always_desc
                            RuleMode.LIMIT -> R.string.edit_rule_limit_desc
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (mode == RuleMode.LIMIT) {
                    MinutesPicker(minutes = minutes, onChange = onMinutesChange, onDone = onDone)
                    if (savedLimit > 0 && usedSeconds > 0) {
                        UsageRow(usedSeconds = usedSeconds, limitMinutes = savedLimit, canReset = canReset, onReset = onReset)
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageRow(usedSeconds: Long, limitMinutes: Int, canReset: Boolean, onReset: () -> Unit) {
    val limitSeconds = limitMinutes.coerceAtLeast(1) * 60L
    val over = usedSeconds >= limitSeconds
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LinearProgressIndicator(
            progress = { (usedSeconds.toFloat() / limitSeconds).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        Text(
            stringResource(R.string.today_usage_label, (usedSeconds / 60).toInt(), limitMinutes),
            style = MaterialTheme.typography.bodyMedium,
            color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        OutlinedButton(
            onClick = onReset,
            enabled = canReset,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.reset_usage_btn))
        }
    }
}

/**
 * Number entry that is hard to get wrong: a numeric keyboard, big -/+ buttons, one-tap shortcuts,
 * and the keyboard closes on Done or when you tap outside.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MinutesPicker(
    minutes: Int,
    onChange: (Int) -> Unit,
    onDone: () -> Unit
) {
    var text by remember { mutableStateOf(if (minutes > 0) minutes.toString() else "") }
    // Keep the field in step with the -/+ buttons and shortcuts without fighting the cursor
    // while the user types.
    LaunchedEffect(minutes) {
        if (text.toIntOrNull() != minutes && !(text.isEmpty() && minutes == 0)) {
            text = if (minutes > 0) minutes.toString() else ""
        }
    }
    val invalid = minutes !in 1..MAX_MINUTES

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            FilledIconButton(
                onClick = { onChange((minutes - MINUTES_STEP).coerceAtLeast(MINUTES_STEP)) },
                enabled = minutes > MINUTES_STEP,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.minutes_less))
            }
            OutlinedTextField(
                value = text,
                onValueChange = { raw ->
                    val digits = raw.filter { it.isDigit() }.take(4)
                    text = digits
                    onChange((digits.toIntOrNull() ?: 0).coerceAtMost(MAX_MINUTES))
                },
                singleLine = true,
                isError = invalid,
                textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                suffix = { Text(stringResource(R.string.minutes_suffix)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                modifier = Modifier.width(150.dp)
            )
            FilledIconButton(
                onClick = { onChange((minutes + MINUTES_STEP).coerceAtMost(MAX_MINUTES)) },
                enabled = minutes < MAX_MINUTES,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.minutes_more))
            }
        }
        if (invalid) {
            Text(
                stringResource(R.string.minutes_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MINUTE_SHORTCUTS.forEach { option ->
                SelectableFilterChip(
                    selected = minutes == option,
                    label = "$option ${stringResource(R.string.minutes_suffix)}",
                    onClick = { onChange(option) }
                )
            }
        }
    }
}
