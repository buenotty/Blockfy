package com.robingebert.blokky.feature_accessibility

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

    val URL_VIEW_IDS = listOf(
        "com.android.chrome:id/url_bar",
        "com.sec.android.app.sbrowser:id/location_bar_edit_text",
        "org.mozilla.firefox:id/toolbar_url",
        "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
        "com.brave.browser:id/url_bar",
        "com.microsoft.emmx:id/url_bar",
        "com.opera.browser:id/url_field",
        "com.opera.mini.native:id/url_field",
        "com.opera.gx:id/url_field",
        "com.duckduckgo.mobile.android:id/omnibarTextInput",
        "com.vivaldi.browser:id/url_bar",
        "com.mi.globalbrowser:id/url"
    )

    private val ADULT_DOMAINS = setOf(
        // User requested specifically
        "aznude",
        "onlyfans",
        "fatalmodel",
        "fatalmodels",
        "fatalfans",

        // Brazilian escort, dating & adult sites
        "privacy.com.br",
        "photoacompanhantes",
        "skokka",
        "vivastreet",
        "garotacomlocal",
        "guiadegps",
        "sexlog",
        "sexprive",
        "acompanhantes",
        "prazer24h",
        "fatalclub",
        "rededoprazer",
        "boateazul",
        "swingers",
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

        // Major adult tube & video networks
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
        "hentai",
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

        // Leaks, telegram dumps & VIP rips
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

        // Cam & live sex sites
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

        // International escort & adult platforms
        "fansly",
        "celebjihad",
        "mrskin",
        "adultwork",
        "eurogirlsescort",
        "rubmaps",
        "eros.com",
        "escortdirectory",
        "locanto",
        "simpleescorts",
        "sluts",
        "backpage"
    )

    private val ADULT_KEYWORDS = listOf(
        "porn",
        "porno",
        "xxx",
        "sexo",
        "hentai",
        "putaria",
        "acompanhante",
        "novinha safada",
        "nudez",
        "xrated",
        "erotic video",
        "hardcore sex",
        "blowjob",
        "creampie",
        "milf porn",
        "anal sex",
        "teen porn",
        "shemale porn",
        "trans porn",
        "incesto",
        "buceta",
        "boquete",
        "vaza foto",
        "conteudo adulto",
        "nudes vazados",
        "az nude",
        "only fans",
        "fatal model",
        "fatal models",
        "fatal fans",
        "pack vazado",
        "vazados telegram",
        "camera privê",
        "sex cam",
        "caiu na net",
        "brasileirinhas",
        "camera prive",
        "cameraprive",
        "samba porno",
        "sambaporno",
        "vaza nudes",
        "vazanudes",
        "garota de programa",
        "acompanhante sp",
        "acompanhante rj",
        "acompanhantes sp",
        "acompanhantes rj",
        "novinha",
        "novinhas",
        "coroa safada",
        "casal amador",
        "sexo amador",
        "amador brasileiro",
        "flagra intimo",
        "suruba",
        "menage",
        "travesti",
        "trans brasileira",
        "chupando",
        "gemendo",
        "punheta",
        "masturbacao",
        "massagem erotica",
        "privacy vazado",
        "onlyfans vazado",
        "vazados do telegram",
        "site porno",
        "videos porno",
        "filme porno",
        "hentai legendado",
        "animes hentai",
        "hentaitube",
        "casa de swing",
        "nsfw",
        "gonewild",
        "leaks telegram",
        "🔞"
    )

    val AGGRESSIVE_WARNINGS = listOf(
        "Você quer destruir sua vida?",
        "Não vou deixar você errar de novo.",
        "Isso é pior que drogas, saia fora disso.",
        "Pare agora! Isso está destruindo sua mente e sua energia.",
        "Você é mais forte que esse impulso barato. Volte para o mundo real.",
        "Sua versão do futuro está se envergonhando dessa escolha. Levante a cabeça."
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

    fun isAdultContent(rawText: String?): Boolean {
        if (rawText.isNullOrBlank()) return false

        val normalized = normalize(rawText)
        val compact = normalized.replace("[^a-z0-9]".toRegex(), "")

        // 1. Direct domain check
        for (domain in ADULT_DOMAINS) {
            val compactDomain = domain.replace("[^a-z0-9]".toRegex(), "")
            if (compact.contains(compactDomain) || normalized.contains(domain)) {
                return true
            }
        }

        // 2. Keyword check
        for (keyword in ADULT_KEYWORDS) {
            val normalizedKeyword = normalize(keyword)
            if (normalized.contains(normalizedKeyword)) {
                return true
            }
            // For space-separated keywords (like "fatal model" or "only fans" or "az nude")
            val compactKeyword = normalizedKeyword.replace("[^a-z0-9]".toRegex(), "")
            if (compactKeyword.length >= 6 && compact.contains(compactKeyword)) {
                return true
            }
        }

        return false
    }

    private fun normalize(input: String): String {
        val lower = input.lowercase(Locale.ROOT)
        return Normalizer.normalize(lower, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    fun getRandomWarning(): String {
        return AGGRESSIVE_WARNINGS.random()
    }
}
