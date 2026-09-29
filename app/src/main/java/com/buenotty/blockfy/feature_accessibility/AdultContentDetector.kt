package com.buenotty.blockfy.feature_accessibility

import android.content.Context
import com.buenotty.blockfy.R
import java.text.Normalizer
import java.util.Locale

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
        "com.vivo.browser"
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

    fun isBlockedHost(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val hostname = normalize(host).trim().trim('.')
        if (hostname.isEmpty()) return false

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
    fun isAdultContent(rawText: String?): Boolean {
        if (rawText.isNullOrBlank()) return false
        val normalized = normalize(rawText)

        HOST_IN_TEXT.findAll(normalized).forEach { match ->
            if (isBlockedHost(match.groupValues[1])) return true
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

    private fun normalize(input: String): String {
        val lower = input.lowercase(Locale.ROOT)
        return Normalizer.normalize(lower, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }
}
