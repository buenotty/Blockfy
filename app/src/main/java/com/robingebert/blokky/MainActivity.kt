package com.robingebert.blokky

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.robingebert.blokky.feature_accessibility.AdultBlockAlertScreen
import com.robingebert.blokky.navigation.AppNavigation
import com.robingebert.blokky.navigation.Screen
import com.robingebert.blokky.ui.theme.BlokkyTheme
import com.robingebert.blokky.updater.AppUpdateInfo
import com.robingebert.blokky.updater.UpdateDialog
import com.robingebert.blokky.updater.UpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

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
                    val context = LocalContext.current
                    val currentVersion = remember(context) { UpdateManager.getCurrentVersion(context) }
                    var availableUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
                    var showAutoUpdateDialog by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        withContext(Dispatchers.IO) {
                            UpdateManager.checkForUpdates(currentVersion).onSuccess { info ->
                                if (info.isUpdateAvailable) {
                                    withContext(Dispatchers.Main) {
                                        availableUpdateInfo = info
                                        showAutoUpdateDialog = true
                                    }
                                }
                            }
                        }
                    }

                    val navController = rememberNavController()
                    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                    val isAboutScreen = currentRoute == Screen.About.route

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background,
                        topBar = {
                            TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                    titleContentColor = MaterialTheme.colorScheme.onBackground
                                ),
                                title = {
                                    if (isAboutScreen) {
                                        Text(
                                            text = stringResource(R.string.title_about),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Start
                                        ) {
                                            Image(
                                                painter = painterResource(R.drawable.ic_blockfy_logo),
                                                contentDescription = "Blockfy Logo",
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Row {
                                                Text(
                                                    text = "Block",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = androidx.compose.ui.graphics.Color.White
                                                )
                                                Text(
                                                    text = "fy",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = androidx.compose.ui.graphics.Color(0xFF7C83FD)
                                                )
                                            }
                                        }
                                    }
                                },
                                actions = {
                                    if (!isAboutScreen) {
                                        IconButton(onClick = {
                                            navController.navigate(Screen.About.route)
                                        }) {
                                            Icon(
                                                Icons.Rounded.Info,
                                                contentDescription = stringResource(R.string.title_about),
                                                tint = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                },
                                navigationIcon = {
                                    if (isAboutScreen) {
                                        IconButton(onClick = { navController.navigateUp() }) {
                                            Icon(
                                                Icons.AutoMirrored.Rounded.ArrowBack,
                                                contentDescription = "Back",
                                                tint = MaterialTheme.colorScheme.onBackground
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

                    if (showAutoUpdateDialog && availableUpdateInfo != null) {
                        UpdateDialog(
                            currentVersion = currentVersion,
                            initialInfo = availableUpdateInfo,
                            onDismissRequest = { showAutoUpdateDialog = false }
                        )
                    }
                }
            }
        }
    }
}