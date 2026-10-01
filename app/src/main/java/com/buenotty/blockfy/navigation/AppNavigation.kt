package com.buenotty.blockfy.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.buenotty.blockfy.feature_preferences.ui.BlocksScreen
import com.buenotty.blockfy.feature_preferences.ui.ConceptDetailScreen
import com.buenotty.blockfy.feature_preferences.ui.ConceptTopic
import com.buenotty.blockfy.feature_preferences.ui.ConceptsHomeScreen
import com.buenotty.blockfy.feature_preferences.ui.EditAppScreen
import com.buenotty.blockfy.feature_settings.AboutScreen

@Composable
fun AppNavigation(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.Blocks.route,
        modifier = modifier
    ) {
        composable(Screen.Blocks.route) {
            BlocksScreen(onEditApp = { navController.navigate(Screen.EditApp.create(it)) })
        }
        composable(Screen.Concepts.route) {
            ConceptsHomeScreen(onOpenTopic = { navController.navigate(Screen.ConceptTopic.create(it.route)) })
        }
        composable(Screen.Settings.route) { AboutScreen() }
        composable(
            route = Screen.EditApp.route,
            arguments = listOf(navArgument(Screen.EditApp.ARG) { type = NavType.StringType })
        ) { entry ->
            EditAppScreen(
                appName = entry.arguments?.getString(Screen.EditApp.ARG).orEmpty(),
                onClose = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.ConceptTopic.route,
            arguments = listOf(navArgument(Screen.ConceptTopic.ARG) { type = NavType.StringType })
        ) { entry ->
            ConceptDetailScreen(
                topic = ConceptTopic.fromRoute(entry.arguments?.getString(Screen.ConceptTopic.ARG)),
                onBack = { navController.popBackStack() }
            )
        }
    }
}
