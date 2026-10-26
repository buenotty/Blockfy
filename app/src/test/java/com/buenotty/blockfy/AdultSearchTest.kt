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
}
