package com.buenotty.blockfy.feature_accessibility

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory view of what the accessibility service is doing, for the Diagnostics card in
 * Settings. Nothing here is stored or sent anywhere. It holds app package names and view ids
 * (never text from the screen), and view ids are only collected while the card is open.
 */
object ServiceDiagnostics {

    data class Snapshot(
        val connected: Boolean = false,
        val lastPackage: String = "",
        val lastEventAtMillis: Long = 0L,
        val shortsVisible: Boolean = false,
        val lastDecision: String = "",
        val seenViewIds: List<String> = emptyList(),
        /** Packages the service is currently subscribed to. */
        val listening: List<String> = emptyList()
    )

    private val state = MutableStateFlow(Snapshot())
    val snapshot: StateFlow<Snapshot> = state.asStateFlow()

    @Volatile
    var collectingViewIds: Boolean = false

    fun setConnected(connected: Boolean) = state.update { it.copy(connected = connected) }

    fun onEvent(pkg: String, shortsVisible: Boolean, decision: String) = state.update {
        it.copy(
            lastPackage = pkg,
            lastEventAtMillis = System.currentTimeMillis(),
            shortsVisible = shortsVisible,
            lastDecision = decision
        )
    }

    fun onScope(packages: List<String>) = state.update { it.copy(listening = packages) }

    fun onViewIds(ids: List<String>) = state.update { it.copy(seenViewIds = ids) }
}
