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
    fun flagsSafeSearchBeingTurnedOff() {
        assertTrue(AdultContentDetector.isSafeSearchBypass("google.com/search?q=gatos&safe=off"))
        assertTrue(AdultContentDetector.isSafeSearchBypass("google.com/search?q=gatos&tbm=isch&safe=images"))
        assertTrue(AdultContentDetector.isSafeSearchBypass("www.google.com.br/safesearch"))
        assertTrue(AdultContentDetector.isSafeSearchBypass("bing.com/search?q=gatos&adlt=off"))
        assertFalse(AdultContentDetector.isSafeSearchBypass("google.com/search?q=gatos&safe=active"))
        assertFalse(AdultContentDetector.isSafeSearchBypass("google.com/search?q=receita+bolo"))
        assertFalse(AdultContentDetector.isSafeSearchBypass(null))
    }

    @Test
    fun flagsIncognitoMarkersButNotMenuItems() {
        assertTrue(AdultContentDetector.isIncognitoMarker("com.android.chrome:id/incognito_badge", null))
        assertTrue(AdultContentDetector.isIncognitoMarker(null, "Anônima"))
        assertTrue(AdultContentDetector.isIncognitoMarker(null, "InPrivate"))
        assertFalse(AdultContentDetector.isIncognitoMarker("com.android.chrome:id/new_incognito_tab_menu_id", null))
        assertFalse(AdultContentDetector.isIncognitoMarker(null, "Nova guia anônima"))
        assertFalse(AdultContentDetector.isIncognitoMarker(null, null))
    }

    @Test
    fun leavesANormalWindowAloneEvenWhenItTalksAboutPrivateTabs() {
        // A normal Chrome window shows these: the tab-switcher toggle, settings rows, promos, menu entries.
        assertFalse(AdultContentDetector.isIncognitoMarker("com.android.chrome:id/incognito_toggle_tabs", null))
        assertFalse(AdultContentDetector.isIncognitoMarker("com.android.chrome:id/incognito_tab_switcher_button", null))
        assertFalse(AdultContentDetector.isIncognitoMarker("com.android.chrome:id/incognito_lock_setting", null))
        assertFalse(AdultContentDetector.isIncognitoMarker("com.android.chrome:id/incognito_promo_title", null))
        assertFalse(AdultContentDetector.isIncognitoMarker("com.android.chrome:id/incognito_menu_item", null))
        assertFalse(AdultContentDetector.isIncognitoMarker("com.android.chrome:id/newIncognitoTab", null))
        assertFalse(AdultContentDetector.isIncognitoMarker(null, "Abas anônimas"))
        assertFalse(AdultContentDetector.isIncognitoMarker(null, "Incognito tabs"))
        assertFalse(AdultContentDetector.isIncognitoMarker("com.android.chrome:id/url_bar", "Pesquisar ou digitar URL"))
    }

    @Test
    fun stillSpotsAPrivateWindowFromItsOwnIndicators() {
        assertTrue(AdultContentDetector.isIncognitoMarker("com.android.chrome:id/incognito_indicator", null))
        assertTrue(AdultContentDetector.isIncognitoMarker("org.mozilla.firefox:id/private_mode_icon", null))
        assertTrue(AdultContentDetector.isIncognitoMarker(null, "Navegação privada"))
        assertTrue(AdultContentDetector.isIncognitoMarker(null, "Modo secreto"))
    }

    @Test
    fun theWebPageIsNeverScannedForPrivateWindowMarkers() {
        assertTrue(AdultContentDetector.isPageContent("android.webkit.WebView"))
        assertFalse(AdultContentDetector.isPageContent("android.view.ViewGroup"))
        assertFalse(AdultContentDetector.isPageContent(null))
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
