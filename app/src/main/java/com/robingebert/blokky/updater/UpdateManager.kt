package com.robingebert.blokky.updater

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val tagName: String,
    val versionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val fileName: String,
    val apkSize: Long,
    val isUpdateAvailable: Boolean
)

object UpdateManager {

    private const val GITHUB_API_URL = "https://api.github.com/repos/buenotty/Blockfy/releases/latest"

    fun getCurrentVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo?.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    suspend fun checkForUpdates(currentVersion: String): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(GITHUB_API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("User-Agent", "Blockfy-App")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP Error: ${connection.responseCode}")
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)

            val tagName = json.getString("tag_name")
            val remoteVersion = tagName.trimStart('v', 'V')
            val title = json.optString("name", tagName)
            val body = json.optString("body", "")

            val assets = json.getJSONArray("assets")
            var downloadUrl = ""
            var fileName = "Blockfy-latest.apk"
            var size = 0L

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val assetName = asset.getString("name")
                if (assetName.endsWith(".apk", ignoreCase = true)) {
                    downloadUrl = asset.getString("browser_download_url")
                    fileName = assetName
                    size = asset.optLong("size", 0L)
                    break
                }
            }

            if (downloadUrl.isEmpty()) {
                throw Exception("No APK found in the latest release")
            }

            AppUpdateInfo(
                tagName = tagName,
                versionName = remoteVersion,
                releaseTitle = title,
                releaseNotes = body,
                downloadUrl = downloadUrl,
                fileName = fileName,
                apkSize = size,
                isUpdateAvailable = isNewerVersion(remoteVersion, currentVersion)
            )
        }
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        val remoteParts = remote.split('.').mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }
        val localParts = local.split('.').mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
