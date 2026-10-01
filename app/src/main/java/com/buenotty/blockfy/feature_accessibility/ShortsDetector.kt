package com.buenotty.blockfy.feature_accessibility

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Decides whether a Reels/Shorts screen is on display. Apps rename their view ids between
 * releases, so three signals are tried, cheapest first:
 *  1. exact ids we know,
 *  2. any visible view whose id contains a known fragment (survives small renames),
 *  3. the bottom tab for Reels/Shorts being the selected one.
 */
object ShortsDetector {

    private val EXACT_IDS = mapOf(
        "Instagram" to listOf(
            "com.instagram.android:id/clips_swipe_refresh_container",
            "com.instagram.android:id/clips_viewer_view_pager"
        ),
        "YouTube" to listOf(
            "com.google.android.youtube:id/reel_watch_fragment_root",
            "com.google.android.youtube:id/reel_player_page_container",
            "com.google.android.youtube:id/reel_recycler"
        ),
        "Facebook" to listOf(
            "com.facebook.katana:id/fb_shorts_container",
            "com.facebook.katana:id/reels_viewer"
        )
    )

    private val ID_FRAGMENTS = mapOf(
        "Instagram" to listOf("clips_viewer", "clips_swipe_refresh"),
        "YouTube" to listOf("reel_watch", "reel_player_page", "reel_recycler", "shorts_player"),
        "Facebook" to listOf("reels_viewer", "shorts_container")
    )

    private const val INSTAGRAM_REELS_TAB = "com.instagram.android:id/clips_tab"
    private const val YOUTUBE_PIVOT_BAR = "com.google.android.youtube:id/pivot_bar"
    private const val MAX_SCANNED_NODES = 300

    fun isVisible(appName: String, root: AccessibilityNodeInfo, deepScan: Boolean): Boolean {
        val exact = EXACT_IDS[appName] ?: return false
        if (exact.any { hasVisibleId(root, it) }) return true
        if (!deepScan) return false
        if (appName == "Instagram" && isSelectedById(root, INSTAGRAM_REELS_TAB)) return true
        if (appName == "YouTube" && isShortsTabSelected(root)) return true
        val fragments = ID_FRAGMENTS[appName] ?: return false
        return scanForFragment(root, fragments, intArrayOf(MAX_SCANNED_NODES))
    }

    private fun hasVisibleId(root: AccessibilityNodeInfo, id: String): Boolean = try {
        val nodes = root.findAccessibilityNodeInfosByViewId(id)
        val visible = !nodes.isNullOrEmpty() && nodes.any { it.isVisibleToUser }
        nodes?.forEach { it.recycle() }
        visible
    } catch (_: Exception) {
        false
    }

    private fun isSelectedById(root: AccessibilityNodeInfo, id: String): Boolean = try {
        val nodes = root.findAccessibilityNodeInfosByViewId(id)
        val selected = !nodes.isNullOrEmpty() && nodes.any { it.isSelected }
        nodes?.forEach { it.recycle() }
        selected
    } catch (_: Exception) {
        false
    }

    private fun isShortsTabSelected(root: AccessibilityNodeInfo): Boolean = try {
        val bars = root.findAccessibilityNodeInfosByViewId(YOUTUBE_PIVOT_BAR)
        var found = false
        bars?.forEach { bar ->
            for (i in 0 until bar.childCount) {
                val tab = bar.getChild(i) ?: continue
                val label = (tab.contentDescription ?: tab.text)?.toString().orEmpty()
                if (tab.isSelected && label.startsWith("Shorts", ignoreCase = true)) found = true
                tab.recycle()
            }
            bar.recycle()
        }
        found
    } catch (_: Exception) {
        false
    }

    private fun scanForFragment(node: AccessibilityNodeInfo, fragments: List<String>, budget: IntArray): Boolean {
        if (budget[0] <= 0) return false
        budget[0]--
        if (node.isVisibleToUser) {
            val id = node.viewIdResourceName
            if (id != null && fragments.any { id.contains(it) }) return true
        }
        for (i in 0 until node.childCount) {
            if (budget[0] <= 0) return false
            val child = node.getChild(i) ?: continue
            try {
                if (scanForFragment(child, fragments, budget)) return true
            } finally {
                child.recycle()
            }
        }
        return false
    }
}
