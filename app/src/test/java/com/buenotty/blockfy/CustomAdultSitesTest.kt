package com.buenotty.blockfy

import com.buenotty.blockfy.datastore.AppSettings
import com.buenotty.blockfy.feature_accessibility.AdultContentDetector
import com.buenotty.blockfy.feature_accessibility.AdultEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomAdultSitesTest {

    @Test
    fun `normalizeSite reduces what people paste to a bare host`() {
        assertEquals("exemplo.com", AdultContentDetector.normalizeSite("exemplo.com"))
        assertEquals("exemplo.com", AdultContentDetector.normalizeSite("  https://www.Exemplo.com/path?q=1#x "))
        assertEquals("sub.exemplo.com.br", AdultContentDetector.normalizeSite("http://sub.exemplo.com.br:8080/"))
    }

    @Test
    fun `normalizeSite rejects anything that is not a plausible site`() {
        assertNull(AdultContentDetector.normalizeSite(""))
        assertNull(AdultContentDetector.normalizeSite("exemplo"))
        assertNull(AdultContentDetector.normalizeSite("two words.com"))
        assertNull(AdultContentDetector.normalizeSite("a.b"))
        assertNull(AdultContentDetector.normalizeSite("exemplo..com"))
        assertNull(AdultContentDetector.normalizeSite("-exemplo.com"))
    }

    @Test
    fun `a custom site blocks itself and its subdomains but not lookalikes`() {
        val extra = listOf("meusitexyz.com")
        assertFalse(AdultContentDetector.isAdultContent("meusitexyz.com"))
        assertTrue(AdultContentDetector.isAdultContent("meusitexyz.com", extra))
        assertTrue(AdultContentDetector.isAdultContent("https://www.meusitexyz.com/video/1", extra))
        assertTrue(AdultContentDetector.isAdultContent("m.meusitexyz.com", extra))
        assertFalse(AdultContentDetector.isAdultContent("notmeusitexyz.com", extra))
        assertFalse(AdultContentDetector.isAdultContent("meusitexyz.com.evil.org", extra))
    }

    @Test
    fun `parseEntry tells a site from a keyword and rejects the rest`() {
        assertEquals(AdultEntry("exemplo.com", true), AdultContentDetector.parseEntry("https://www.exemplo.com/a"))
        assertEquals(AdultEntry("termo secreto", false), AdultContentDetector.parseEntry("  Térmo  Secreto "))
        assertNull(AdultContentDetector.parseEntry("ab"))
        assertNull(AdultContentDetector.parseEntry("meu site.com"))
        assertNull(AdultContentDetector.parseEntry("um dois tres quatro"))
    }

    @Test
    fun `a custom keyword flags searches that contain it as a whole word`() {
        val extra = listOf("termosecreto")
        assertFalse(AdultContentDetector.isAdultSearch("google.com/search?q=termosecreto"))
        assertTrue(AdultContentDetector.isAdultSearch("google.com/search?q=termosecreto", extra))
        assertTrue(AdultContentDetector.isAdultSearch("google.com/search?q=ver+termosecreto+hoje", extra))
        assertFalse(AdultContentDetector.isAdultSearch("google.com/search?q=umtermosecretomaior", extra))
    }

    @Test
    fun `settings keep the custom list across a serialisation round trip with old data`() {
        // A settings file written before the field existed must still load.
        assertEquals(emptyList<String>(), AppSettings().customAdultSites)
    }
}
