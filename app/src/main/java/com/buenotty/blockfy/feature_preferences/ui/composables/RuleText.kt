package com.buenotty.blockfy.feature_preferences.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.buenotty.blockfy.R
import java.util.Locale

/** Text helpers shared by the app cards and the edit screen, so both always say the same thing. */

const val ALL_DAYS_MASK = 127
const val WORKDAYS_MASK = 0b0111110
const val WEEKEND_MASK = 0b1000001
const val FULL_DAY_START = 0
const val FULL_DAY_END = 1439

fun Int.toTime(): String = String.format(Locale.ROOT, "%02d:%02d", this / 60, this % 60)

@Composable
fun appDisplayName(name: String): String =
    if (name == "X") stringResource(R.string.x_app) else name

@Composable
fun shortsFeatureLabel(appName: String): String =
    if (appName == "YouTube") stringResource(R.string.app_scope_shorts) else stringResource(R.string.app_scope_reels)

@Composable
fun daysLabel(mask: Int): String = when (mask) {
    ALL_DAYS_MASK -> stringResource(R.string.weekdays_everyday)
    WORKDAYS_MASK -> stringResource(R.string.weekdays_workdays)
    WEEKEND_MASK -> stringResource(R.string.weekdays_weekend)
    else -> {
        val names = listOf(
            stringResource(R.string.weekday_sun),
            stringResource(R.string.weekday_mon),
            stringResource(R.string.weekday_tue),
            stringResource(R.string.weekday_wed),
            stringResource(R.string.weekday_thu),
            stringResource(R.string.weekday_fri),
            stringResource(R.string.weekday_sat)
        )
        names.filterIndexed { index, _ -> mask and (1 shl index) != 0 }.joinToString(", ")
    }
}

@Composable
fun hoursLabel(start: Int, end: Int): String =
    if (start == FULL_DAY_START && end == FULL_DAY_END) {
        stringResource(R.string.schedule_all_day)
    } else {
        stringResource(R.string.hours_range, start.toTime(), end.toTime())
    }

@Composable
fun limitLabel(limitMinutes: Int): String =
    if (limitMinutes > 0) stringResource(R.string.app_rule_limit, limitMinutes) else stringResource(R.string.app_rule_blocked)
