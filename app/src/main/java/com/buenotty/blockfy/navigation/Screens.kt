package com.buenotty.blockfy.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Blocks : Screen("blocks")
    data object Concepts : Screen("concepts")
    data object Settings : Screen("settings")

    data object EditApp : Screen("edit/{app}") {
        const val ARG = "app"
        fun create(appName: String) = "edit/${Uri.encode(appName)}"
    }

    data object ConceptTopic : Screen("concept/{topic}") {
        const val ARG = "topic"
        fun create(topicRoute: String) = "concept/$topicRoute"
    }
}
