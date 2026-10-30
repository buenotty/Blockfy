package com.buenotty.blockfy.feature_monitor

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.buenotty.blockfy.R
import com.buenotty.blockfy.datastore.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.DateFormat
import java.util.Date

/**
 * Quick Settings tile: one tap starts a focus hour that blocks every tracked app. The system
 * runs the tile only while the panel is open, so it costs no battery. Focus cannot be ended
 * early (see DataStoreManager), so tapping while it runs only says when it ends.
 */
class FocusTileService : TileService(), KoinComponent {

    private val dataStore: DataStoreManager by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch { render(dataStore.appSettingsFlow.first().focusUntilEpoch) }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val until = dataStore.appSettingsFlow.first().focusUntilEpoch
            if (until > System.currentTimeMillis()) {
                Toast.makeText(applicationContext, getString(R.string.tile_focus_until, formatTime(until)), Toast.LENGTH_LONG).show()
                return@launch
            }
            val end = System.currentTimeMillis() + FOCUS_MILLIS
            dataStore.updateSettings { it.copy(focusUntilEpoch = end) }
            render(end)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun render(until: Long) {
        val tile = qsTile ?: return
        val active = until > System.currentTimeMillis()
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_focus_label)
        tile.subtitle = if (active) getString(R.string.tile_focus_until, formatTime(until)) else getString(R.string.tile_focus_idle)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_stat_blockfy)
        tile.updateTile()
    }

    private fun formatTime(millis: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))

    private companion object {
        const val FOCUS_MILLIS = 60L * 60L * 1000L
    }
}
