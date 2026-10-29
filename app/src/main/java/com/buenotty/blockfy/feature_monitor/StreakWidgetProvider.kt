package com.buenotty.blockfy.feature_monitor

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.buenotty.blockfy.MainActivity
import com.buenotty.blockfy.R
import com.buenotty.blockfy.datastore.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Home-screen widget with the current clean-day streak. It updates when the app is left and about
 * every 30 minutes (the system minimum), reading only the history already on the phone, so it
 * costs next to no battery.
 */
class StreakWidgetProvider : AppWidgetProvider(), KoinComponent {

    private val dataStore: DataStoreManager by inject()

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val streak = StreakCalculator.compute(
                    dataStore.historyFlow.first().days,
                    dataStore.dailyUsageFlow.first(),
                    dataStore.appSettingsFlow.first()
                )
                val open = PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                for (id in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_streak)
                    views.setTextViewText(R.id.widget_streak_number, streak.current.toString())
                    views.setOnClickPendingIntent(R.id.widget_streak_root, open)
                    manager.updateAppWidget(id, views)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** Asks every placed widget to redraw; a no-op when none is placed. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, StreakWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                context.sendBroadcast(
                    Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                        .setClass(context, StreakWidgetProvider::class.java)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                )
            }
        }
    }
}
