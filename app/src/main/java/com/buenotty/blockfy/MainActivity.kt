package com.buenotty.blockfy

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.buenotty.blockfy.feature_accessibility.AdultBlockAlertScreen
import com.buenotty.blockfy.navigation.AppNavigation
import com.buenotty.blockfy.navigation.Screen
import com.buenotty.blockfy.ui.theme.BlokkyTheme

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ADULT_BLOCK_WARNING = "extra_adult_block_warning"
    }

    private var adultBlockWarning by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_ADULT_BLOCK_WARNING)?.let {
            adultBlockWarning = it
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
        adultBlockWarning = intent.getStringExtra(EXTRA_ADULT_BLOCK_WARNING)

        setContent {
            BlokkyTheme {
                val warning = adultBlockWarning
                if (warning != null) {
                    AdultBlockAlertScreen(
                        quote = warning,
                        onDismiss = {
                            adultBlockWarning = null
                            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(homeIntent)
                            finish()
                        }
                    )
                } else {
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
                                                Icons.Rounded.Info,
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
