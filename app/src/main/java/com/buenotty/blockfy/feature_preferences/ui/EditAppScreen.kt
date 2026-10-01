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
import androidx.compose.material3.OutlinedCard
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
import com.buenotty.blockfy.datastore.DailyUsage
import com.buenotty.blockfy.feature_monitor.BlockPolicy
import com.buenotty.blockfy.feature_preferences.OverviewViewModel
import com.buenotty.blockfy.feature_preferences.repository.models.App
import com.buenotty.blockfy.feature_preferences.ui.composables.ALL_DAYS_MASK
import com.buenotty.blockfy.feature_preferences.ui.composables.AppBrandIcon
import com.buenotty.blockfy.feature_preferences.ui.composables.DisableBlockerDialog
import com.buenotty.blockfy.feature_preferences.ui.composables.FULL_DAY_END
import com.buenotty.blockfy.feature_preferences.ui.composables.FULL_DAY_START
import com.buenotty.blockfy.feature_preferences.ui.composables.SectionTitle
import com.buenotty.blockfy.feature_preferences.ui.composables.SelectableFilterChip
import com.buenotty.blockfy.feature_preferences.ui.composables.StrictModeDialog
import com.buenotty.blockfy.feature_preferences.ui.composables.TimePickerDialog
import com.buenotty.blockfy.feature_preferences.ui.composables.WEEKEND_MASK
import com.buenotty.blockfy.feature_preferences.ui.composables.WORKDAYS_MASK
import com.buenotty.blockfy.feature_preferences.ui.composables.appDisplayName
import com.buenotty.blockfy.feature_preferences.ui.composables.daysLabel
import com.buenotty.blockfy.feature_preferences.ui.composables.hoursLabel
import com.buenotty.blockfy.feature_preferences.ui.composables.shortsFeatureLabel
import com.buenotty.blockfy.feature_preferences.ui.composables.toTime
import com.buenotty.blockfy.feature_preferences.ui.composables.trackedAppIcon
import org.koin.androidx.compose.koinViewModel

private const val MAX_MINUTES = 1440
private const val MINUTES_STEP = 5
private val MINUTE_SHORTCUTS = listOf(15, 30, 45, 60, 90, 120)
private const val NIGHT_START = 22 * 60
private const val NIGHT_END = 6 * 60
private const val WORK_START = 9 * 60
private const val WORK_END = 18 * 60

@Composable
fun EditAppScreen(
    appName: String,
    onClose: () -> Unit,
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
        usage = usage,
        locked = settings.isStrictLocked(),
        onSave = { viewModel.updateApp(it) },
        onResetUsage = { viewModel.resetDailyUsage(appName) },
        onClose = onClose
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditAppForm(
    saved: App,
    usage: DailyUsage,
    locked: Boolean,
    onSave: (App) -> Unit,
    onResetUsage: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val wholeOnly = BlockPolicy.isWholeAppOnly(saved.name)
    val savedLimit = BlockPolicy.limitMinutes(saved)

    // The draft is remembered by app name only, so a background settings update (for example the
    // usage counter) can never wipe what the user is typing.
    var enabled by rememberSaveable(saved.name) { mutableStateOf(true) }
    var wholeApp by rememberSaveable(saved.name) { mutableStateOf(saved.wholeApp) }
    var limitMode by rememberSaveable(saved.name) { mutableStateOf(savedLimit > 0) }
    var minutes by rememberSaveable(saved.name) { mutableStateOf(if (savedLimit > 0) savedLimit else 30) }
    var start by rememberSaveable(saved.name) { mutableStateOf(saved.blockedStart) }
    var end by rememberSaveable(saved.name) { mutableStateOf(saved.blockedEnd) }
    var days by rememberSaveable(saved.name) { mutableStateOf(saved.blockedWeekdays) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    var showLoosenConfirm by remember { mutableStateOf(false) }
    var showStrictDialog by remember { mutableStateOf(false) }

    val isWhole = wholeOnly || wholeApp
    val draft = saved.copy(
        blocked = enabled,
        wholeApp = if (wholeOnly) false else wholeApp,
        blockedStart = start,
        blockedEnd = end,
        blockedWeekdays = days,
        dailyLimitMinutes = if (!isWhole && limitMode) minutes else 0,
        appTotalDailyLimitMinutes = if (isWhole && limitMode) minutes else 0
    )
    val dirty = draft != saved
    val minutesValid = !limitMode || minutes in 1..MAX_MINUTES
    val hoursValid = start != end
    // Strict mode still lets the user make a block stricter; it only refuses to loosen one.
    val loosening = BlockPolicy.isLoosening(saved, draft)
    val canSave = dirty && minutesValid && hoursValid && !(locked && loosening)

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
                RuleSummaryCard(draft = draft)

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

                SectionCard(title = stringResource(R.string.edit_section_scope)) {
                    if (wholeOnly) {
                        Text(
                            stringResource(R.string.edit_scope_whole_only_hint, appDisplayName(saved.name)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        TwoChoice(
                            first = stringResource(R.string.edit_scope_only, shortsFeatureLabel(saved.name)),
                            second = stringResource(R.string.app_scope_whole),
                            secondSelected = wholeApp,
                            onSelect = { wholeApp = it }
                        )
                    }
                }

                SectionCard(title = stringResource(R.string.edit_section_rule)) {
                    TwoChoice(
                        first = stringResource(R.string.edit_rule_always),
                        second = stringResource(R.string.edit_rule_limit),
                        secondSelected = limitMode,
                        onSelect = { limitMode = it }
                    )
                    Text(
                        stringResource(if (limitMode) R.string.edit_rule_limit_desc else R.string.edit_rule_always_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (limitMode) {
                        MinutesPicker(
                            minutes = minutes,
                            onChange = { minutes = it },
                            onDone = { focusManager.clearFocus() }
                        )
                    }
                }

                SectionCard(title = stringResource(R.string.edit_section_when)) {
                    Text(
                        stringResource(R.string.edit_hours_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SelectableFilterChip(
                            selected = start == FULL_DAY_START && end == FULL_DAY_END,
                            label = stringResource(R.string.schedule_all_day),
                            onClick = { start = FULL_DAY_START; end = FULL_DAY_END }
                        )
                        SelectableFilterChip(
                            selected = start == NIGHT_START && end == NIGHT_END,
                            label = stringResource(R.string.preset_night),
                            onClick = { start = NIGHT_START; end = NIGHT_END }
                        )
                        SelectableFilterChip(
                            selected = start == WORK_START && end == WORK_END,
                            label = stringResource(R.string.preset_work),
                            onClick = { start = WORK_START; end = WORK_END }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimeField(
                            label = stringResource(R.string.start_time),
                            value = start.toTime(),
                            onClick = { showStartPicker = true },
                            modifier = Modifier.weight(1f)
                        )
                        TimeField(
                            label = stringResource(R.string.end_time),
                            value = end.toTime(),
                            onClick = { showEndPicker = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (!hoursValid) {
                        Text(
                            stringResource(R.string.hours_error_equal),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (start > end) {
                        Text(
                            stringResource(R.string.hours_crosses_midnight),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.weekdays_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SelectableFilterChip(
                            selected = days == ALL_DAYS_MASK,
                            label = stringResource(R.string.weekdays_everyday),
                            onClick = { days = ALL_DAYS_MASK }
                        )
                        SelectableFilterChip(
                            selected = days == WORKDAYS_MASK,
                            label = stringResource(R.string.weekdays_workdays),
                            onClick = { days = WORKDAYS_MASK }
                        )
                        SelectableFilterChip(
                            selected = days == WEEKEND_MASK,
                            label = stringResource(R.string.weekdays_weekend),
                            onClick = { days = WEEKEND_MASK }
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val labels = listOf(
                            R.string.weekday_sun, R.string.weekday_mon, R.string.weekday_tue, R.string.weekday_wed,
                            R.string.weekday_thu, R.string.weekday_fri, R.string.weekday_sat
                        )
                        labels.forEachIndexed { index, label ->
                            val bit = 1 shl index
                            SelectableFilterChip(
                                selected = days and bit != 0,
                                label = stringResource(label),
                                onClick = {
                                    val next = days xor bit
                                    if (next != 0) days = next
                                }
                            )
                        }
                    }
                }

                if (limitMode && saved.blocked) {
                    TodayUsageCard(
                        usedSeconds = BlockPolicy.usedSeconds(draft, usage),
                        limitMinutes = minutes,
                        canReset = !locked,
                        onReset = onResetUsage
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showStartPicker) {
        TimePickerDialog(
            initialTime = start,
            title = stringResource(R.string.start_time),
            onDismiss = { showStartPicker = false },
            onConfirm = { start = it; showStartPicker = false }
        )
    }
    if (showEndPicker) {
        TimePickerDialog(
            initialTime = end,
            title = stringResource(R.string.end_time),
            onDismiss = { showEndPicker = false },
            onConfirm = { end = it; showEndPicker = false }
        )
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
private fun RuleSummaryCard(draft: App) {
    val name = appDisplayName(draft.name)
    val whole = BlockPolicy.isWholeScope(draft)
    val subject = if (whole) {
        stringResource(R.string.subject_whole, name)
    } else {
        stringResource(R.string.subject_shorts, name, shortsFeatureLabel(draft.name))
    }
    val days = daysLabel(draft.blockedWeekdays)
    val hours = hoursLabel(draft.blockedStart, draft.blockedEnd)
    val limit = BlockPolicy.limitMinutes(draft)
    val sentence = when {
        !draft.blocked -> stringResource(R.string.summary_off, name)
        limit > 0 -> stringResource(R.string.summary_limit, subject, limit, days, hours)
        else -> stringResource(R.string.summary_always, subject, days, hours)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.summary_title),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(sentence, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
private fun SectionCard(title: String, content: @Composable () -> Unit) {
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
                content()
            }
        }
    }
}

@Composable
private fun TwoChoice(
    first: String,
    second: String,
    secondSelected: Boolean,
    enabled: Boolean = true,
    onSelect: (Boolean) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = !secondSelected,
            onClick = { onSelect(false) },
            enabled = enabled,
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
        ) { Text(first, maxLines = 1) }
        SegmentedButton(
            selected = secondSelected,
            onClick = { onSelect(true) },
            enabled = enabled,
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
        ) { Text(second, maxLines = 1) }
    }
}

@Composable
private fun TimeField(
    label: String,
    value: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
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
    enabled: Boolean = true,
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
                enabled = enabled && minutes > MINUTES_STEP,
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
                enabled = enabled,
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
                enabled = enabled && minutes < MAX_MINUTES,
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
                    enabled = enabled,
                    label = "$option ${stringResource(R.string.minutes_suffix)}",
                    onClick = { onChange(option) }
                )
            }
        }
    }
}

@Composable
private fun TodayUsageCard(
    usedSeconds: Long,
    limitMinutes: Int,
    canReset: Boolean,
    onReset: () -> Unit
) {
    val limitSeconds = limitMinutes.coerceAtLeast(1) * 60L
    val over = usedSeconds >= limitSeconds
    SectionCard(title = stringResource(R.string.today_title)) {
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
        if (usedSeconds > 0) {
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
}
