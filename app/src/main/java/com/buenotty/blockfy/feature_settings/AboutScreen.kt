package com.buenotty.blockfy.feature_settings

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.SettingsEthernet
import androidx.compose.material.icons.rounded.Shop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.buenotty.blockfy.R
import com.buenotty.blockfy.updater.UpdateManager
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import qrcode.QRCode
import java.io.ByteArrayOutputStream

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    var showQrCodeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    painterResource(R.drawable.ic_policy),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = stringResource(R.string.version_label, getVersionName(context)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                IconButton(onClick = { showQrCodeDialog = true }) {
                    Icon(
                        Icons.Rounded.QrCode2,
                        contentDescription = stringResource(R.string.share_app_title),
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(15.dp)),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HeroCard(icon = Icons.Rounded.Shop, title = stringResource(R.string.check_updates_btn)) {
                UpdateManager.openPlayStoreOrSource(context)
            }
            HeroCard(icon = Icons.Rounded.BugReport, title = stringResource(R.string.report_bug_btn)) {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, "https://github.com/buenotty/Blockfy/issues".toUri())
                )
            }
            HeroCard(icon = Icons.Rounded.Balance, title = stringResource(R.string.licenses_btn)) {
                context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
            }
            HeroCard(icon = Icons.Rounded.SettingsEthernet, title = stringResource(R.string.source_code_btn)) {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, "https://github.com/buenotty/Blockfy".toUri())
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Text(
                text = stringResource(R.string.about_story),
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.authorship_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Samuel Bueno — ${stringResource(R.string.fork_author_role)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Robin Gebert — ${stringResource(R.string.original_author_role)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.privacy_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(R.string.privacy_desc))
            }
        }
    }

    if (showQrCodeDialog) {
        QrCodeDialog { showQrCodeDialog = false }
    }
}

fun getVersionName(context: Context): String {
    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return pInfo?.versionName ?: "Unknown"
}

@Composable
fun HeroCard(icon: ImageVector, title: String, onClick: () -> Unit = {}) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(15.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Text(text = title, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun QrCodeDialog(onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val logoBitmap =
        ResourcesCompat.getDrawable(context.resources, R.drawable.ic_policy, context.theme)?.toBitmap()
    val stream = ByteArrayOutputStream()
    logoBitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)
    val logoQRCode = QRCode.ofRoundedSquares()
        .withBackgroundColor(android.graphics.Color.TRANSPARENT)
        .withColor(if (isSystemInDarkTheme()) android.graphics.Color.WHITE else android.graphics.Color.BLACK)
        .build("https://play.google.com/store/apps/details?id=${context.packageName}")
    val logoQRCodePngData = logoQRCode.renderToBytes()

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.share_app_title), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    bitmap = BitmapFactory.decodeByteArray(
                        logoQRCodePngData,
                        0,
                        logoQRCodePngData.size
                    ).asImageBitmap(),
                    contentDescription = stringResource(R.string.share_app_title),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(R.string.share_app_thanks), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.btn_close))
                }
            }
        }
    }
}

@Preview
@Composable
fun AboutScreenPreview() {
    AboutScreen()
}
