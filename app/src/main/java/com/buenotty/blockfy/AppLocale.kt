package com.buenotty.blockfy

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object AppLocale {
    const val ENGLISH = "en"
    const val PORTUGUESE = "pt"

    private const val PREFS = "blockfy_prefs"
    private const val KEY = "language_tag"

    fun currentTag(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, ENGLISH) ?: ENGLISH
    }

    fun apply(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, tag)
            .apply()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    fun applyStored(context: Context) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(currentTag(context))
        )
    }
}
