package com.buenotty.blockfy.feature_accessibility

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Looks for clear adult text on the screen of a tracked app (Telegram, X and the like), once the
 * user turned the in-app check on. It reads only the visible text of the window in front of it,
 * in memory: nothing is stored, logged or sent, and no screenshot is taken. The tracked apps are
 * the only windows it is ever handed, never a bank or anything else.
 */
object ScreenTextScanner {

    private const val MAX_NODES = 150
    private const val MAX_TEXT_LENGTH = 300

    fun hasAdultText(
        root: AccessibilityNodeInfo,
        extraKeywords: Collection<String>,
        extraHosts: Collection<String>
    ): Boolean = scan(root, extraKeywords, extraHosts, intArrayOf(MAX_NODES))

    private fun scan(
        node: AccessibilityNodeInfo,
        keywords: Collection<String>,
        hosts: Collection<String>,
        budget: IntArray
    ): Boolean {
        if (budget[0] <= 0) return false
        budget[0]--
        if (node.isVisibleToUser && !node.isPassword) {
            val text = node.text?.toString() ?: node.contentDescription?.toString()
            if (text != null && text.length <= MAX_TEXT_LENGTH && AdultContentDetector.isAdultText(text, keywords, hosts)) {
                return true
            }
        }
        for (i in 0 until node.childCount) {
            if (budget[0] <= 0) return false
            val child = node.getChild(i) ?: continue
            try {
                if (scan(child, keywords, hosts, budget)) return true
            } finally {
                child.recycle()
            }
        }
        return false
    }
}
