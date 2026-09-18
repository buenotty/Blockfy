package com.buenotty.blockfy

import android.content.Context
import android.telephony.TelephonyManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object AppLocale {
    const val ENGLISH = "en"
    const val PORTUGUESE = "pt-BR"
    const val AUTO = "auto"

    private const val PREFS = "blockfy_prefs"
    private const val KEY = "language_tag"

    fun isBrazil(deviceLocale: Locale, regionHints: List<String> = emptyList()): Boolean {
        if (deviceLocale.country.equals("BR", ignoreCase = true)) return true
        return regionHints.any { it.equals("BR", ignoreCase = true) }
    }

    fun normalize(tag: String): String {
        val normalized = tag.trim().replace('_', '-').lowercase(Locale.ROOT)
        return when {
            normalized.isEmpty() || normalized == AUTO -> AUTO
            normalized == "pt" || normalized.startsWith("pt-") -> PORTUGUESE
            else -> ENGLISH
        }
    }

    fun selectionFromStored(storedTag: String?): String {
        if (storedTag.isNullOrBlank()) return AUTO
        return normalize(storedTag)
    }

    fun resolveTag(
        storedTag: String?,
        deviceLocale: Locale,
        regionHints: List<String> = emptyList()
    ): String {
        val selection = selectionFromStored(storedTag)
        if (selection != AUTO) return selection
        return if (isBrazil(deviceLocale, regionHints)) PORTUGUESE else ENGLISH
    }

    fun deviceLocale(context: Context): Locale {
        val locales = context.resources.configuration.locales
        return if (locales.isEmpty) Locale.getDefault() else locales[0]
    }

    fun deviceRegionHints(context: Context): List<String> {
        val hints = mutableListOf<String>()
        val localeCountry = deviceLocale(context).country
        if (localeCountry.isNotBlank()) hints += localeCountry
        runCatching {
            val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            telephony?.simCountryIso?.takeIf { it.isNotBlank() }?.let { hints += it }
            telephony?.networkCountryIso?.takeIf { it.isNotBlank() }?.let { hints += it }
        }
        return hints
    }

    fun currentTag(context: Context): String {
        return resolveTag(storedTag(context), deviceLocale(context), deviceRegionHints(context))
    }

    fun currentSelection(context: Context): String {
        return selectionFromStored(storedTag(context))
    }

    fun apply(context: Context, tag: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val selection = normalize(tag)
        val stored = if (selection == AUTO) null else selection
        prefs.edit().let { editor ->
            if (stored == null) editor.remove(KEY) else editor.putString(KEY, stored)
            editor.commit()
        }
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(
                resolveTag(stored, deviceLocale(context), deviceRegionHints(context))
            )
        )
    }

    fun applyStored(context: Context) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(currentTag(context))
        )
    }

    private fun storedTag(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
    }
}
