package com.buenotty.blockfy

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.buenotty.blockfy.feature_preferences.ui.composables.isAccessibilityGranted
import com.buenotty.blockfy.feature_setup.SetupScreen
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.buenotty.blockfy.feature_onboarding.NoticeScreen
import com.buenotty.blockfy.feature_preferences.OverviewViewModel
import com.buenotty.blockfy.navigation.AppNavigation
import com.buenotty.blockfy.navigation.Screen
import com.buenotty.blockfy.ui.theme.BlockfyTheme
import org.koin.androidx.compose.koinViewModel

private data class TopLevelTab(val screen: Screen, val label: Int, val icon: ImageVector)

private val TABS = listOf(
    TopLevelTab(Screen.Blocks, R.string.nav_blocks, Icons.Rounded.Shield),
    TopLevelTab(Screen.Concepts, R.string.nav_concepts, Icons.AutoMirrored.Rounded.MenuBook),
    TopLevelTab(Screen.Settings, R.string.nav_settings, Icons.Rounded.Settings)
)

class MainActivity : AppCompatActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BlockfyTheme {
                val viewModel: OverviewViewModel = koinViewModel()
                val loaded by viewModel.isLoaded.collectAsState()
                val settings by viewModel.appSettings.collectAsState()
                val context = LocalContext.current
                var accessibilityOn by remember { mutableStateOf(context.isAccessibilityGranted()) }
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    accessibilityOn = context.isAccessibilityGranted()
                }

                // Remember that Accessibility was on; finding it off later, with blocks set up, means the
                // user switched it off, which throws today's streak away.
                LaunchedEffect(accessibilityOn, loaded, settings.setupDone) {
                    if (loaded && settings.setupDone) {
                        if (accessibilityOn) viewModel.noteAccessibilityGranted() else viewModel.noteAccessibilityMissing()
                    }
                }

                when {
                    // Wait for the stored settings so the notice never flashes for people who
                    // already accepted it.
                    !loaded -> Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                    !settings.onboardingDone -> NoticeScreen(onAccept = viewModel::setOnboardingDone)
                    // Accessibility and background running come first. The same screen comes back if
                    // Accessibility is ever found switched off.
                    !settings.setupDone || !accessibilityOn -> SetupScreen(onFinish = viewModel::setSetupDone)
                    else -> {
                        LaunchedEffect(Unit) { askForNotificationsOnce() }
                        MainScaffold()
                    }
                }
            }
        }
    }

    private fun askForNotificationsOnce() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationManagerCompat.from(this).areNotificationsEnabled()
        ) {
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val currentRoute = navController.currentRoute()
    val currentTab = TABS.firstOrNull { it.screen.route == currentRoute }

    // Detail pages (edit app, read a concept) bring their own top bar and hide the tabs.
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (currentTab != null) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            stringResource(
                                if (currentTab.screen == Screen.Blocks) R.string.app_name else currentTab.label
                            )
                        )
                    }
                )
            }
        },
        bottomBar = {
            if (currentTab != null) {
                NavigationBar {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = tab.screen.route == currentRoute,
                            onClick = { navController.openTab(tab.screen) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        AppNavigation(
            navController = navController,
            modifier = Modifier.padding(padding)
        )
    }
}

private fun NavController.openTab(screen: Screen) {
    navigate(screen.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun NavController.currentRoute(): String? {
    val entry by currentBackStackEntryAsState()
    return entry?.destination?.route
}
