package com.buenotty.blockfy.updater

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

object UpdateManager {

    fun getCurrentVersion(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0)?.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    fun openPlayStoreOrSource(context: Context) {
        val play = Intent(
            Intent.ACTION_VIEW,
            "market://details?id=${context.packageName}".toUri()
        )
        val web = Intent(
            Intent.ACTION_VIEW,
            "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()
        )
        val github = Intent(
            Intent.ACTION_VIEW,
            "https://github.com/buenotty/Blockfy/releases/latest".toUri()
        )
        try {
            context.startActivity(play)
        } catch (_: Exception) {
            try {
                context.startActivity(web)
            } catch (_: Exception) {
                context.startActivity(github)
            }
        }
    }
}
