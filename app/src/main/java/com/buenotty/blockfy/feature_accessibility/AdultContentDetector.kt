package com.buenotty.blockfy.feature_accessibility

import android.content.Context
import com.buenotty.blockfy.R
import java.text.Normalizer
import java.util.Locale

/** A permanent entry the user added to the adult list: a site, or a search keyword. */
data class AdultEntry(val value: String, val isSite: Boolean)

object AdultContentDetector {

    val BROWSER_PACKAGES = setOf(
        "com.android.chrome",
        "com.sec.android.app.sbrowser",
        "org.mozilla.firefox",
        "com.brave.browser",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.opera.gx",
        "com.duckduckgo.mobile.android",
        "com.vivaldi.browser",
        "com.mi.globalbrowser",
        "com.android.browser",
        "com.kiwibrowser.browser",
        "com.yandex.browser",
        "com.coloros.browser",
        "com.vivo.browser",
        // Beta/dev channels and other common browsers: installing one must not be a way around the shield.
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary",
        "com.brave.browser_beta",
        "com.brave.browser_nightly",
        "com.microsoft.emmx.beta",
        "com.microsoft.emmx.dev",
        "org.mozilla.firefox_beta",
        "org.mozilla.fenix",
        "com.sec.android.app.sbrowser.beta",
        "com.opera.mini.native.beta",
        "com.opera.browser.beta",
        "com.yandex.browser.lite",
        "com.ecosia.android",
        "com.UCMobile.intl",
        "com.uc.browser.en",
        "com.huawei.browser",
        "com.heytap.browser",
        "mark.via.gp",
        "com.cloudmosa.puffin",
        "com.google.android.googlequicksearchbox",
        "com.google.android.apps.searchlite",
        // Browsers that are private by design: with incognito blocking on they are left at once.
        "org.mozilla.focus",
        "org.mozilla.klar",
        "org.torproject.torbrowser",
        "org.torproject.torbrowser_alpha"
    )

    /** Browsers whose every tab is private. Blocking incognito means blocking these as a whole. */
    val ALWAYS_PRIVATE_BROWSERS = setOf(
        "org.mozilla.focus",
        "org.mozilla.klar",
        "org.torproject.torbrowser",
        "org.torproject.torbrowser_alpha"
    )

    private val ADULT_DOMAINS = setOf(
        "aznude",
        "onlyfans",
        "fatalmodel",
        "fatalmodels",
        "fatalfans",
        "privacy.com.br",
        "photoacompanhantes",
        "skokka",
        "vivastreet",
        "garotacomlocal",
        "guiadegps",
        "sexlog",
        "sexprive",
        "prazer24h",
        "fatalclub",
        "rededoprazer",
        "boateazul",
        "sexbr",
        "encontroseroticos",
        "agenciagp",
        "topacompanhantes",
        "cameraprive",
        "sambaporno",
        "brasileirinhas",
        "umodel",
        "caiunanet",
        "vazanudes",
        "tufos",
        "belladasemana",
        "diamondbrazil",
        "sexyclube",
        "gatasdobrasil",
        "pornoflix",
        "pornobrasil",
        "amadorbrasileiro",
        "pornogratis",
        "xvideosred",
        "novinhasdozap",
        "vazadosdatv",
        "pornhub",
        "xvideos",
        "xnxx",
        "xhamster",
        "redtube",
        "youporn",
        "spankbang",
        "eporner",
        "tube8",
        "beeg",
        "tnaflix",
        "heavy-r",
        "motherless",
        "tblop",
        "hqporner",
        "txxx",
        "thumbzilla",
        "daftsex",
        "fuq",
        "vporn",
        "porntrex",
        "porn555",
        "camwhores",
        "fapello",
        "leakgirls",
        "brazzers",
        "naughtyamerica",
        "bangbros",
        "realitykings",
        "erome",
        "rule34",
        "nhentai",
        "hanime",
        "gelbooru",
        "danbooru",
        "nudevista",
        "javhd",
        "javlibrary",
        "missav",
        "jable",
        "tktube",
        "empflix",
        "sunporno",
        "pornone",
        "nuvid",
        "youjizz",
        "pornmd",
        "alohatube",
        "4tube",
        "drtuber",
        "porndig",
        "perfectgirls",
        "anyporn",
        "clips4sale",
        "iwara",
        "xcafe",
        "javfinder",
        "javmost",
        "avgle",
        "hentaigasm",
        "hentaihaven",
        "multporn",
        "luscious",
        "sankakucomplex",
        "tsumino",
        "hitomi.la",
        "8muses",
        "javbus",
        "porngo",
        "sublimeporn",
        "pornbox",
        "tubegalore",
        "coomer.party",
        "coomer.su",
        "kemono.party",
        "kemono.su",
        "simpcity",
        "thothub",
        "bunkr",
        "cyberdrop",
        "nudostar",
        "hotleaks",
        "leakhive",
        "nudogram",
        "fanbus",
        "leaksource",
        "fapcat",
        "fapvid",
        "chaturbate",
        "stripchat",
        "bongacams",
        "livejasmin",
        "camsoda",
        "cam4",
        "myfreecams",
        "manyvids",
        "camster",
        "flirt4free",
        "streamate",
        "imlive",
        "fansly",
        "celebjihad",
        "mrskin",
        "adultwork",
        "eurogirlsescort",
        "rubmaps",
        "eros.com",
        "escortdirectory",
        "simpleescorts",
        "backpage"
    )

    private val ADULT_PHRASES = listOf(
        "porn",
        "porno",
        "xxx",
        "nsfw",
        "hentai",
        "onlyfans",
        "only fans",
        "fatal model",
        "fatal models",
        "az nude",
        "sex cam",
        "hardcore sex",
        "erotic video",
        "adult video",
        "porn hub",
        "x videos"
    )

    private val HOST_IN_TEXT = Regex(
        """(?:https?://)?(?:www\.)?([a-z0-9][a-z0-9.-]*\.[a-z]{2,})""",
        RegexOption.IGNORE_CASE
    )

    private val SCHEME = Regex("^[a-z][a-z0-9+.-]*://")
    private val LABEL = Regex("^[a-z0-9]([a-z0-9-]*[a-z0-9])?$")

    /**
     * Reduces what a person types or pastes ("https://www.Site.com/page?x=1") to the bare host
     * ("site.com"), or null when it cannot be a real site. Used before a site is added to the
     * permanent list, so a typo that is not even a host never gets in.
     */
    fun normalizeSite(raw: String): String? {
        var text = raw.trim().lowercase(Locale.ROOT).replace(SCHEME, "")
        text = text.substringBefore('/').substringBefore('?').substringBefore('#')
        text = text.substringAfterLast('@').substringBefore(':').removePrefix("www.")
        val labels = text.split('.')
        val valid = text.length <= 253 && labels.size >= 2 && labels.all { LABEL.matches(it) } &&
            labels.last().length >= 2 && labels.last().all { it in 'a'..'z' }
        return text.takeIf { valid }
    }

    private val KEYWORD = Regex("^[a-z0-9]+( [a-z0-9]+){0,2}$")

    /** A search word or short phrase (up to three words) in its stored form, or null when it is not usable. */
    fun normalizeKeyword(raw: String): String? {
        val text = normalize(raw).trim().replace(Regex("\\s+"), " ")
        return text.takeIf { it.length in 4..40 && KEYWORD.matches(it) }
    }

    /** What the user typed, read as a site when it looks like one and as a search keyword otherwise. */
    fun parseEntry(raw: String): AdultEntry? =
        normalizeSite(raw)?.let { AdultEntry(it, isSite = true) }
            ?: normalizeKeyword(raw)?.let { AdultEntry(it, isSite = false) }

    /** [extraHosts] are sites the user added; they match the host itself and its subdomains only. */
    fun isBlockedHost(host: String?, extraHosts: Collection<String> = emptyList()): Boolean {
        if (host.isNullOrBlank()) return false
        val hostname = normalize(host).trim().trim('.')
        if (hostname.isEmpty()) return false

        if (extraHosts.any { hostname == it || hostname.endsWith(".$it") }) return true

        for (domain in ADULT_DOMAINS) {
            val needle = normalize(domain).trim('.')
            if (needle.isEmpty()) continue
            if (hostname == needle || hostname.endsWith(".$needle")) {
                return true
            }
            if (!needle.contains('.')) {
                val labels = hostname.split('.')
                if (labels.any { it == needle }) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Matches adult hosts and phrases as whole tokens, never as a substring
     * inside another word. "jerome" must not match the site "erome".
     */
    fun isAdultContent(rawText: String?, extraHosts: Collection<String> = emptyList()): Boolean {
        if (rawText.isNullOrBlank()) return false
        val normalized = normalize(rawText)

        HOST_IN_TEXT.findAll(normalized).forEach { match ->
            if (isBlockedHost(match.groupValues[1], extraHosts)) return true
        }

        for (domain in ADULT_DOMAINS) {
            val needle = normalize(domain)
            if (needle.length < 4) continue
            if (containsToken(normalized, needle)) return true
        }

        for (phrase in ADULT_PHRASES) {
            if (containsToken(normalized, normalize(phrase))) return true
        }
        return false
    }

    /** Words that mean adult content on their own, in Portuguese and English. */
    private val STRONG_SEARCH_TERMS = setOf(
        "porn", "porno", "pornos", "pornografia", "pornhub", "xvideos", "xnxx", "xhamster", "redtube",
        "xxx", "nude", "nudes", "naked", "nua", "nuas", "pelada", "peladas", "nudez", "hentai",
        "putaria", "putarias", "sexo explicito", "video porno", "videos porno", "sex tape", "onlyfans",
        "erotico", "erotica", "punheta", "siririca", "safadeza", "safadezas", "porn groups",
        "nsfw", "leaks nudes", "nudes vazados", "nudes vazadas", "pack nudes", "packs nudes",
        "conteudo adulto", "conteudo +18", "conteudo 18+", "vazados nudes", "videos vazados"
    )

    /** Words that point at a group, channel or invite hunt. Alone they are harmless. */
    private val GROUP_CONTEXT_TERMS = setOf(
        "telegram", "tg", "whatsapp", "zap", "zapzap", "discord", "grupo", "grupos", "canal", "canais",
        "link", "links", "convite", "invite", "group", "groups", "channel", "channels", "comunidade",
        "comunidades", "server", "servidor"
    )

    /** Words that are fine in many places but mean adult content next to a group word. */
    private val GROUP_ADULT_TERMS = setOf(
        "18", "+18", "18+", "putaria", "vazados", "vazadas", "vazado", "vazada", "novinha", "novinhas",
        "safada", "safadas", "safadinha", "safadinhas", "gostosa", "gostosas", "swing", "corno", "cornos",
        "cuckold", "packs", "pack", "leaks", "leaked", "leak", "hentai", "onlyfans", "privacy",
        "casadas", "casada", "ninfeta", "ninfetas", "sexo", "sexy", "adulto", "adultos", "adult",
        "nsfw", "porn", "porno", "nudes", "nude", "erotico", "erotica"
    )

    /**
     * Search words in the text shown in the address bar. Whole words only, so "nuance" or
     * "unaware" never match. A group word plus an adult word also matches, which catches searches
     * for adult Telegram, WhatsApp or Discord groups. Only used on the browser's address bar.
     */
    fun isAdultSearch(rawText: String?, extraKeywords: Collection<String> = emptyList()): Boolean {
        if (rawText.isNullOrBlank()) return false
        val normalized = normalize(percentDecode(rawText))
            .replace('_', ' ').replace('-', ' ')
        // In a URL query a plus sign is a space, but a typed "+18" keeps its sign before the number.
        val spaced = Regex("\\+(?!\\d)").replace(normalized, " ")
        val unleeted = unleet(spaced)
        for (text in listOf(spaced, unleeted).distinct()) {
            if (matchesSearchWords(text, extraKeywords)) return true
        }
        return isSpelledOutAdultSearch(searchQueryOf(unleeted), extraKeywords)
    }

    private fun matchesSearchWords(spaced: String, extraKeywords: Collection<String>): Boolean {
        if (STRONG_SEARCH_TERMS.any { containsToken(spaced, normalize(it)) }) return true
        if (extraKeywords.any { containsToken(spaced, it) }) return true
        val hasGroupWord = GROUP_CONTEXT_TERMS.any { containsToken(spaced, it) }
        if (hasGroupWord && GROUP_ADULT_TERMS.any { containsToken(spaced, normalize(it)) }) return true
        val hasLeakWord = LEAK_TERMS.any { containsToken(spaced, it) }
        return hasLeakWord && LEAK_ADULT_TERMS.any { containsToken(spaced, it) }
    }

    /** "Leaked" alone is news. Next to one of [LEAK_ADULT_TERMS] it is a hunt for intimate photos. */
    private val LEAK_TERMS = setOf(
        "vazado", "vazados", "vazada", "vazadas", "vazou", "vazaram", "vazamento", "leak", "leaks", "leaked"
    )
    private val LEAK_ADULT_TERMS = setOf(
        "nude", "nudes", "nua", "nuas", "pelada", "peladas", "intimo", "intimos", "intima", "intimas",
        "sem roupa", "pack", "packs", "privacy", "onlyfans", "18", "+18", "18+", "sexo", "sex", "video intimo",
        "fotos intimas", "foto intima", "erome", "fansly", "close certo", "caiu na net"
    )

    /**
     * Single words built from the adult lists, used to catch a word typed in pieces
     * ("ero me", "p o r n", "nu des") or with letters swapped for digits ("p0rn").
     */
    private val SPLIT_NEEDLES: Set<String> by lazy {
        val words = (ADULT_DOMAINS.filter { '.' !in it && '-' !in it } +
            STRONG_SEARCH_TERMS.map { it.replace(" ", "") } +
            ADULT_PHRASES.map { it.replace(" ", "") } +
            EXTRA_SPLIT_WORDS)
            .map { normalize(it) }
            .filter { it.all { c -> c in 'a'..'z' || c in '0'..'9' } }
        words.filter { it.length >= 4 && it !in SPLIT_EXCLUDED && !(it.any(Char::isDigit) && it.length < 6) }.toSet()
    }

    /** Words that only make sense spelled out letter by letter ("x x x", "n u a"). */
    private val SPELLED_NEEDLES: Set<String> by lazy {
        SPLIT_NEEDLES + setOf("xxx", "nua", "sex", "sexo", "nuas", "nude", "porn")
    }

    /** Explicit words that are not in the other lists because alone they read fine elsewhere. */
    private val EXTRA_SPLIT_WORDS = setOf(
        "pornografia", "putaria", "putarias", "buceta", "boceta", "xoxota", "xereca", "boquete", "peladinha",
        "peladona", "gostosapelada", "nudesvazados", "vazanudes", "semroupa", "pussy", "blowjob", "boobs", "tits",
        "cumshot", "gangbang", "creampie", "camgirl", "camgirls", "sexting", "nudez"
    )

    /** Joined pieces that are ordinary phrases ("im live", "miss av"); never read as a split site name. */
    private val SPLIT_EXCLUDED = setOf("imlive", "streamate", "umodel", "missav", "beeg", "tufos", "camster", "fanbus")

    /** Distinctive names that are never part of an ordinary word, so a word that merely contains one is blocked. */
    private val CONTAINED_NEEDLES = listOf(
        "porn", "hentai", "xvideo", "xnxx", "xhamster", "onlyfans", "pornhub", "brazzers", "chaturbate", "redtube",
        "youporn", "spankbang", "stripchat", "bongacams", "livejasmin", "fapello", "nudostar", "vazanude"
    )

    /** Short words that join with the next one into an innocent word ("por" + "no"). A piece made only of these is skipped. */
    private val STOPWORDS = setOf(
        "a", "o", "e", "as", "os", "de", "da", "do", "das", "dos", "em", "no", "na", "nos", "nas", "por", "um", "uma",
        "se", "me", "te", "que", "com", "sem", "pra", "para", "the", "of", "in", "on", "to", "an", "is", "it", "at",
        "or", "for", "and", "my", "you", "el", "la", "los", "las", "y"
    )

    private val SEARCH_PARAM = Regex("""[?&#](?:q|p|query|text|search_query|wd|k)=([^&#]*)""")

    /** The search words of a results URL, or the whole text when the address bar shows only the query. */
    private fun searchQueryOf(text: String): String {
        val params = SEARCH_PARAM.findAll(text).map { it.groupValues[1] }.toList()
        return if (params.isNotEmpty()) params.joinToString(" ") else text
    }

    /**
     * Words typed in pieces so no single piece looks adult: "ero me vaz ado", "p o r n o", "nu.des".
     * Consecutive pieces are joined and the result must BE an adult word, never just contain one,
     * so "por novas" is not "porno" and "jerome" is not "erome".
     */
    private fun isSpelledOutAdultSearch(query: String, extraKeywords: Collection<String>): Boolean {
        val tokens = query.split(NON_ALNUM).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return false
        val extras = extraKeywords.map { it.replace(" ", "") }.filter { it.length >= 4 }.toSet()

        for (token in tokens) {
            val squeezed = squeeze(token)
            if (CONTAINED_NEEDLES.any { it in token || it in squeezed }) return true
            if (token.length > 4 && prefixNeedles.any { (raw, sq) -> token.startsWith(raw) || squeezed.startsWith(sq) }) {
                return true
            }
            if (squeezed != token && (squeezed in SPLIT_NEEDLES || squeezed in squeezedNeedles)) return true
        }

        for (start in tokens.indices) {
            val joined = StringBuilder()
            var allStopwords = true
            var allSingleLetters = true
            for (end in start until minOf(tokens.size, start + MAX_SPLIT_PIECES)) {
                val piece = tokens[end]
                joined.append(piece)
                if (piece !in STOPWORDS) allStopwords = false
                if (piece.length != 1) allSingleLetters = false
                if (joined.length > MAX_SPLIT_LENGTH) break
                if (end == start || allStopwords) continue
                val word = joined.toString()
                val squeezed = squeeze(word)
                if (word in SPLIT_NEEDLES || squeezed in squeezedNeedles || word in extras) return true
                if (allSingleLetters && (word in SPELLED_NEEDLES || CONTAINED_NEEDLES.any { it in word })) return true
            }
        }
        return false
    }

    /** A doubled letter that squeezes into a short ordinary word ("boobs" -> "bobs") is left out. */
    private val squeezedNeedles: Set<String> by lazy {
        SPLIT_NEEDLES.map { squeeze(it) to it }.filter { (sq, raw) -> sq == raw || sq.length >= 5 }.map { it.first }.toSet()
    }

    /** Words that start with one of these are ordinary too ("peladeiro", "pussycat", "nakedness"). */
    private val PREFIX_EXCLUDED = setOf("pelada", "pussy", "naked", "luscious", "boobs", "nudez")

    /** A whole word that merely starts with one of these ("eromevazado", "nudesvazadas") is blocked. */
    private val prefixNeedles: List<Pair<String, String>> by lazy {
        SPLIT_NEEDLES.filter { it.length >= 5 && it !in PREFIX_EXCLUDED }.map { it to squeeze(it) }
    }

    private const val MAX_SPLIT_PIECES = 12
    private const val MAX_SPLIT_LENGTH = 24
    private val NON_ALNUM = Regex("[^a-z0-9]+")

    /** "poooorn" -> "porn": a letter repeated to dodge the list counts once. */
    private fun squeeze(word: String): String {
        val out = StringBuilder(word.length)
        for (c in word) if (out.isEmpty() || out.last() != c) out.append(c)
        return out.toString()
    }

    private val LEET = mapOf('0' to 'o', '1' to 'i', '3' to 'e', '4' to 'a', '5' to 's', '7' to 't', '@' to 'a', '$' to 's')
    private val MIXED_WORD = Regex("[a-z0-9@$]+")

    /** "p0rn", "nud3s", "s3x0": digits and symbols used as letters, only inside words that also have letters. */
    private fun unleet(text: String): String = MIXED_WORD.replace(text) { m ->
        val w = m.value
        if (w.any { it in 'a'..'z' } && w.any { it in LEET }) w.map { LEET[it] ?: it }.joinToString("") else w
    }

    /** %xx escapes, keeping "+" as it is (it may be a space or the sign of "+18", decided later). */
    private fun percentDecode(text: String): String = try {
        java.net.URLDecoder.decode(text.replace("+", "%2B").replace(Regex("%(?![0-9a-fA-F]{2})"), "%25"), "UTF-8")
    } catch (_: Exception) {
        text
    }

    private val SAFE_SEARCH_OFF = Regex(
        """[?&](safe=(off|images)|adlt=off|kp=-2|safesearch=off)(&|$)|(google|bing)\.[a-z.]+/(safesearch|preferences|account/general)"""
    )

    /** Address-bar text that turns SafeSearch off or opens its settings page. */
    fun isSafeSearchBypass(rawText: String?): Boolean {
        if (rawText.isNullOrBlank()) return false
        return SAFE_SEARCH_OFF.containsMatchIn(normalize(rawText))
    }

    private val INCOGNITO_LABELS = setOf(
        "incognito", "anonimo", "anonima", "aba anonima", "guia anonima", "navegacao anonima",
        "modo anonimo", "private browsing", "navegacao privada", "inprivate", "secret mode", "modo secreto"
    )

    /**
     * Words in a view id that make it a button, menu entry, tab-switcher toggle or settings row
     * ABOUT private tabs, which a normal window shows too, rather than a sign that this window is private.
     */
    private val INCOGNITO_ID_NOISE = setOf(
        "menu", "toggle", "tab", "tabs", "switch", "switcher", "button", "btn", "setting", "settings",
        "pref", "preference", "learn", "lock", "reauth", "promo", "dialog", "title", "summary", "close",
        "list", "item", "row", "text", "label", "action", "option", "options", "more", "description"
    )

    /**
     * A private-window marker among the browser's own controls: a view id that names it, or a
     * description that is exactly the label. Anything that only offers private tabs ("New incognito
     * tab", the tab-switcher toggle, a settings row) never matches, so a normal window is left alone.
     */
    fun isIncognitoMarker(viewId: String?, description: CharSequence?, text: CharSequence? = null): Boolean {
        val id = viewId?.substringAfter(":id/")?.lowercase(Locale.ROOT)
        // The page a private tab opens on ("You've gone Incognito"): only ever drawn inside a private tab.
        if (id != null && INCOGNITO_PAGE_IDS.any { id.startsWith(it) }) return true
        if (text != null && isIncognitoHeadline(text)) return true
        if (id != null && !id.startsWith("new") &&
            ("incognito" in id || "inprivate" in id || "private_mode" in id) &&
            id.split('_').none { it in INCOGNITO_ID_NOISE }
        ) {
            return true
        }
        return description != null && normalize(description.toString()).trim() in INCOGNITO_LABELS
    }

    /** View ids of the start page of a private tab, which a normal window never shows. */
    private val INCOGNITO_PAGE_IDS = listOf(
        "new_tab_incognito", "incognito_ntp", "incognito_description", "incognito_new_tab_page",
        "private_browsing_header", "private_browsing_description", "private_mode_home", "secret_mode_"
    )

    /** Headlines of the start page of a private tab, in Portuguese and English. */
    private val INCOGNITO_HEADLINES = listOf(
        "you've gone incognito", "youve gone incognito", "voce esta navegando como anonimo",
        "voce esta no modo anonimo", "voce esta navegando no modo anonimo", "voce esta em uma guia anonima",
        "voce esta em uma aba anonima", "navegando como anonimo", "this is a private window",
        "esta e uma janela privada", "you're browsing privately", "voce esta navegando de forma privada",
        "inprivate browsing", "navegacao inprivate", "you are in secret mode", "voce esta no modo secreto",
        "modo de navegacao anonima"
    )

    fun isIncognitoHeadline(text: CharSequence): Boolean {
        val t = normalize(text.toString()).replace('’', '\'').trim()
        if (t.length > 120) return false
        return INCOGNITO_HEADLINES.any { t == it || t.startsWith("$it ") || t.startsWith("$it.") || t.startsWith("$it!") }
    }

    /** Words a browser uses in the notification it keeps while private tabs are open. */
    private val INCOGNITO_NOTIFICATION_WORDS = listOf(
        "incognito", "anonima", "anonimas", "anonimo", "anonimos", "inprivate", "private tab", "private tabs",
        "guia privada", "guias privadas", "aba privada", "abas privadas", "private browsing", "navegacao privada",
        "secret mode", "modo secreto", "private window", "janela privada", "janelas privadas", "private session"
    )

    /**
     * Chrome, Edge, Brave, Firefox and others post a lasting notification ("Close all Incognito tabs",
     * "Fechar todas as guias anônimas") while a private tab exists. This is the one sign every one of
     * them gives, whatever its screens look like, so it is the main way a private tab is spotted.
     */
    fun isIncognitoNotification(texts: Collection<CharSequence?>): Boolean {
        val joined = texts.filterNotNull().joinToString(" ") { normalize(it.toString()) }
        if (joined.isBlank()) return false
        return INCOGNITO_NOTIFICATION_WORDS.any { containsToken(joined, it) }
    }

    /** A notification button that ends the private session ("Close all", "Fechar tudo"). */
    fun isCloseAction(title: CharSequence?): Boolean {
        val t = title?.let { normalize(it.toString()) } ?: return false
        return listOf("close", "fechar", "encerrar", "sair", "exit", "end", "apagar", "erase", "clear")
            .any { containsToken(t, it) }
    }

    /** The web page itself. What a site shows is never read, so the private-window scan does not go inside. */
    fun isPageContent(className: CharSequence?): Boolean = className?.toString() == "android.webkit.WebView"

    fun getRandomWarning(context: Context): String {
        val warnings = context.resources.getStringArray(R.array.adult_block_warnings)
        return warnings.random()
    }

    internal fun containsToken(haystack: String, needle: String): Boolean {
        if (needle.isBlank()) return false
        val escaped = Regex.escape(needle)
        val pattern = Regex("(^|[^a-z0-9])$escaped($|[^a-z0-9])")
        return pattern.containsMatchIn(haystack)
    }

    /** Cyrillic and Greek letters that look like Latin ones, used to write "рorn" so it slips past a list. */
    private val HOMOGLYPHS = mapOf(
        'а' to 'a', 'е' to 'e', 'о' to 'o', 'р' to 'p', 'с' to 'c', 'у' to 'y', 'х' to 'x', 'і' to 'i',
        'ј' to 'j', 'ѕ' to 's', 'к' to 'k', 'м' to 'm', 'т' to 't', 'н' to 'h', 'в' to 'b',
        'α' to 'a', 'ε' to 'e', 'ο' to 'o', 'ρ' to 'p', 'ν' to 'v', 'χ' to 'x', 'ι' to 'i', 'κ' to 'k', 'τ' to 't'
    )
    private val INVISIBLE = Regex("[\\u00AD\\u200B-\\u200F\\u2060\\uFEFF]")
    private val DIACRITICS = "\\p{InCombiningDiacriticalMarks}+".toRegex()

    /**
     * Lower case, no accents, no invisible characters, full-width and look-alike letters folded
     * to plain Latin ones, so "ｐｏｒｎ", "p\u200Bo\u200Brn" and "рorn" all read as "porn".
     */
    private fun normalize(input: String): String {
        val lower = Normalizer.normalize(input.replace(INVISIBLE, ""), Normalizer.Form.NFKD)
            .lowercase(Locale.ROOT)
            .replace(DIACRITICS, "")
        return if (lower.none { it in HOMOGLYPHS }) lower else lower.map { HOMOGLYPHS[it] ?: it }.joinToString("")
    }
}
