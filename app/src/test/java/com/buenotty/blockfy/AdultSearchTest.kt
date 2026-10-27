package com.buenotty.blockfy

import com.buenotty.blockfy.feature_accessibility.AdultContentDetector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdultSearchTest {

    @Test
    fun flagsSearchTermsInTheAddressBar() {
        assertTrue(AdultContentDetector.isAdultSearch("google.com/search?q=atriz+nua&sei=abc"))
        assertTrue(AdultContentDetector.isAdultSearch("google.com/search?q=videos%20porno"))
        assertTrue(AdultContentDetector.isAdultSearch("duckduckgo.com/?q=nude+photos"))
    }

    @Test
    fun ignoresNormalSearchesAndLookalikeWords() {
        assertFalse(AdultContentDetector.isAdultSearch("google.com/search?q=receita+bolo"))
        assertFalse(AdultContentDetector.isAdultSearch("google.com/search?q=nuance+music"))
        assertFalse(AdultContentDetector.isAdultSearch("google.com/search?q=denuded+hills"))
        assertFalse(AdultContentDetector.isAdultSearch(null))
    }

    @Test
    fun flagsSearchesForAdultGroups() {
        assertTrue(AdultContentDetector.isAdultSearch("google.com/search?q=grupo+telegram+18"))
        assertTrue(AdultContentDetector.isAdultSearch("google.com/search?q=grupos+de+putaria+telegram"))
        assertTrue(AdultContentDetector.isAdultSearch("google.com/search?q=link+grupo+vazados+whatsapp"))
        assertTrue(AdultContentDetector.isAdultSearch("google.com/search?q=discord+nsfw+servers"))
        assertTrue(AdultContentDetector.isAdultSearch("google.com/search?q=telegram+%2B18+novinhas"))
        assertTrue(AdultContentDetector.isAdultSearch("google.com/search?q=nudes+vazados"))
    }

    @Test
    fun keepsOrdinaryGroupAndTelegramSearches() {
        assertFalse(AdultContentDetector.isAdultSearch("google.com/search?q=grupo+de+estudos+telegram"))
        assertFalse(AdultContentDetector.isAdultSearch("google.com/search?q=como+baixar+telegram"))
        assertFalse(AdultContentDetector.isAdultSearch("google.com/search?q=grupo+de+corrida+whatsapp"))
        assertFalse(AdultContentDetector.isAdultSearch("google.com/search?q=vazados+documentos+governo"))
        assertFalse(AdultContentDetector.isAdultSearch("google.com/search?q=filme+18+anos+cartaz"))
    }
}
