package com.buenotty.blockfy

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.buenotty.blockfy.navigation.AppNavigation
import com.buenotty.blockfy.navigation.Screen
import com.buenotty.blockfy.ui.theme.BlockfyTheme

class MainActivity : AppCompatActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            BlockfyTheme {
                run {
                    val navController = rememberNavController()
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            TopAppBar(
                                title = { Text(navController.currentScreenTitle()) },
                                actions = {
                                    if (navController.currentRoute() != Screen.About.route) {
                                        IconButton(onClick = {
                                            navController.navigate(Screen.About.route)
                                        }) {
                                            Icon(
                                                Icons.Rounded.Settings,
                                                contentDescription = stringResource(R.string.title_about)
                                            )
                                        }
                                    }
                                },
                                navigationIcon = {
                                    if (navController.currentRoute() == Screen.About.route) {
                                        IconButton(onClick = { navController.navigateUp() }) {
                                            Icon(
                                                Icons.AutoMirrored.Rounded.ArrowBack,
                                                contentDescription = stringResource(R.string.btn_back)
                                            )
                                        }
                                    }
                                }
                            )
                        },
                        content = { paddingValues ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues)
                            ) {
                                AppNavigation(navController = navController)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavController.currentRoute(): String? {
    return currentBackStackEntryAsState().value?.destination?.route
}

@Composable
private fun NavController.currentScreenTitle(): String {
    return when (currentRoute()) {
        Screen.About.route -> stringResource(R.string.title_about)
        else -> stringResource(R.string.app_name)
    }
}
