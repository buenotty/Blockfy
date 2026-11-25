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

    @Test
    fun catchesWordsTypedInPiecesToDodgeTheList() {
        // The reported bypass: a site name split into syllables, searched on Google Images.
        assertTrue(AdultContentDetector.isAdultSearch("ero me vaz ado"))
        assertTrue(AdultContentDetector.isAdultSearch("google.com/search?q=ero+me+vaz+ado&udm=2"))
        assertTrue(AdultContentDetector.isAdultSearch("eromevazado"))
        assertTrue(AdultContentDetector.isAdultSearch("p o r n o"))
        assertTrue(AdultContentDetector.isAdultSearch("x x x"))
        assertTrue(AdultContentDetector.isAdultSearch("n.u.d.e.s"))
        assertTrue(AdultContentDetector.isAdultSearch("nu des da atriz"))
        assertTrue(AdultContentDetector.isAdultSearch("x videos brasil"))
        assertTrue(AdultContentDetector.isAdultSearch("only fans vazado"))
        assertTrue(AdultContentDetector.isAdultSearch("pornhubbrasil"))
        assertTrue(AdultContentDetector.isAdultSearch("x-v-i-d-e-o-s"))
    }

    @Test
    fun catchesLeetspeakRepeatedLettersAndLookalikeCharacters() {
        assertTrue(AdultContentDetector.isAdultSearch("p0rn"))
        assertTrue(AdultContentDetector.isAdultSearch("nud3s vazados"))
        assertTrue(AdultContentDetector.isAdultSearch("poooorn"))
        assertTrue(AdultContentDetector.isAdultSearch("\u0440orn")) // Cyrillic er
        assertTrue(AdultContentDetector.isAdultSearch("\uFF50\uFF4F\uFF52\uFF4E")) // full-width
        assertTrue(AdultContentDetector.isAdultSearch("p\u200Bo\u200Br\u200Bn")) // zero-width spaces
        assertTrue(AdultContentDetector.isAdultSearch("google.com/search?q=n%C3%BAdes"))
    }

    @Test
    fun catchesLeakHuntsForIntimatePhotos() {
        assertTrue(AdultContentDetector.isAdultSearch("fotos intimas vazadas da cantora"))
        assertTrue(AdultContentDetector.isAdultSearch("atriz vazou sem roupa"))
        assertFalse(AdultContentDetector.isAdultSearch("dados vazados do banco"))
    }

    @Test
    fun joiningPiecesDoesNotCatchOrdinarySearches() {
        assertFalse(AdultContentDetector.isAdultSearch("jerome powell juros"))
        assertFalse(AdultContentDetector.isAdultSearch("por novas regras"))
        assertFalse(AdultContentDetector.isAdultSearch("por no mas"))
        assertFalse(AdultContentDetector.isAdultSearch("bob's burgers"))
        assertFalse(AdultContentDetector.isAdultSearch("pussycat dolls"))
        assertFalse(AdultContentDetector.isAdultSearch("im live on twitch"))
        assertFalse(AdultContentDetector.isAdultSearch("xxl camiseta"))
        assertFalse(AdultContentDetector.isAdultSearch("canal de receitas"))
        assertFalse(AdultContentDetector.isAdultSearch("previsao do tempo sao paulo"))
        assertFalse(AdultContentDetector.isAdultSearch("m.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertFalse(AdultContentDetector.isAdultSearch("google.com/search?q=100%+algodao"))
    }

    @Test
    fun spotsTheBrowsersPrivateTabNotification() {
        assertTrue(AdultContentDetector.isIncognitoNotification(listOf("Chrome", "Fechar todas as guias anônimas")))
        assertTrue(AdultContentDetector.isIncognitoNotification(listOf("Close all Incognito tabs")))
        assertTrue(AdultContentDetector.isIncognitoNotification(listOf("Firefox (Private)", "Close private tabs")))
        assertTrue(AdultContentDetector.isIncognitoNotification(listOf("Close all InPrivate tabs")))
        assertFalse(AdultContentDetector.isIncognitoNotification(listOf("Download complete", "foto.jpg")))
        assertFalse(AdultContentDetector.isIncognitoNotification(listOf(null)))
        assertTrue(AdultContentDetector.isCloseAction("Fechar tudo"))
        assertTrue(AdultContentDetector.isCloseAction("Close all"))
        assertFalse(AdultContentDetector.isCloseAction("Abrir"))
    }

    @Test
    fun spotsThePrivateTabStartPage() {
        assertTrue(AdultContentDetector.isIncognitoMarker("com.android.chrome:id/new_tab_incognito_container", null))
        assertTrue(AdultContentDetector.isIncognitoMarker(null, null, "You\u2019ve gone Incognito"))
        assertTrue(AdultContentDetector.isIncognitoMarker(null, null, "Você está navegando como anônimo"))
        assertFalse(AdultContentDetector.isIncognitoMarker(null, null, "Nova guia anônima"))
        assertTrue(AdultContentDetector.ALWAYS_PRIVATE_BROWSERS.all { it in AdultContentDetector.BROWSER_PACKAGES })
    }
}
