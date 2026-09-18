package com.buenotty.blockfy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class AppLocaleTest {

    @Test
    fun brazilLocaleDefaultsToPortugueseWhenNothingIsStored() {
        val tag = AppLocale.resolveTag(
            storedTag = null,
            deviceLocale = Locale("pt", "BR")
        )
        assertEquals(AppLocale.PORTUGUESE, tag)
        assertEquals("pt-BR", tag)
    }

    @Test
    fun unitedStatesDefaultsToEnglishWhenNothingIsStored() {
        val tag = AppLocale.resolveTag(
            storedTag = null,
            deviceLocale = Locale.US
        )
        assertEquals(AppLocale.ENGLISH, tag)
    }

    @Test
    fun portugalPortugueseDefaultsToEnglishUntilBrazilIsDetected() {
        val tag = AppLocale.resolveTag(
            storedTag = null,
            deviceLocale = Locale("pt", "PT")
        )
        assertEquals(AppLocale.ENGLISH, tag)
    }

    @Test
    fun simCardBrazilWinsOverEnglishPhoneLanguage() {
        val tag = AppLocale.resolveTag(
            storedTag = null,
            deviceLocale = Locale.US,
            regionHints = listOf("br")
        )
        assertEquals(AppLocale.PORTUGUESE, tag)
    }

    @Test
    fun storedEnglishOverridesBrazilDetection() {
        val tag = AppLocale.resolveTag(
            storedTag = "en",
            deviceLocale = Locale("pt", "BR"),
            regionHints = listOf("BR")
        )
        assertEquals(AppLocale.ENGLISH, tag)
    }

    @Test
    fun storedPortugueseIsNormalizedToPtBr() {
        assertEquals(AppLocale.PORTUGUESE, AppLocale.resolveTag("pt", Locale.US))
        assertEquals(AppLocale.PORTUGUESE, AppLocale.resolveTag("pt_BR", Locale.US))
        assertEquals(AppLocale.PORTUGUESE, AppLocale.resolveTag("pt-br", Locale.US))
    }

    @Test
    fun automaticSelectionKeepsDetectingInsteadOfFreezingEnglish() {
        val tag = AppLocale.resolveTag(
            storedTag = AppLocale.AUTO,
            deviceLocale = Locale("pt", "BR")
        )
        assertEquals(AppLocale.PORTUGUESE, tag)
    }

    @Test
    fun selectionIsAutomaticWhenTheUserHasNotChosen() {
        assertEquals(AppLocale.AUTO, AppLocale.selectionFromStored(null))
        assertEquals(AppLocale.AUTO, AppLocale.selectionFromStored(""))
        assertEquals(AppLocale.AUTO, AppLocale.selectionFromStored(AppLocale.AUTO))
        assertEquals(AppLocale.ENGLISH, AppLocale.selectionFromStored("en"))
        assertEquals(AppLocale.PORTUGUESE, AppLocale.selectionFromStored("pt"))
    }

    @Test
    fun brazilHelperAcceptsCountryAndHints() {
        assertTrue(AppLocale.isBrazil(Locale("pt", "BR"), emptyList()))
        assertTrue(AppLocale.isBrazil(Locale.US, listOf("BR")))
        assertFalse(AppLocale.isBrazil(Locale.US, emptyList()))
        assertFalse(AppLocale.isBrazil(Locale("pt", "PT"), emptyList()))
    }
}
