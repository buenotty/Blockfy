package com.buenotty.blockfy

import com.buenotty.blockfy.feature_accessibility.AdultContentDetector
import com.buenotty.blockfy.fixtures.BankPackages
import com.buenotty.blockfy.feature_monitor.TrackedPackages
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ManifestAndDetectorTest {

    @Test
    fun trackedPackagesDoNotIncludeBanks() {
        BankPackages.ALL.forEach { bank ->
            assertFalse("$bank must never be monitored", TrackedPackages.isTracked(bank))
        }
        assertFalse(TrackedPackages.isTracked("com.nu.production"))
        assertTrue(TrackedPackages.isTracked("com.instagram.android"))
    }

    @Test
    fun adultHostListBlocksAdultHostsAndIgnoresNormalSites() {
        assertTrue(AdultContentDetector.isBlockedHost("www.pornhub.com"))
        assertTrue(AdultContentDetector.isBlockedHost("m.xvideos.com"))
        assertTrue(AdultContentDetector.isBlockedHost("privacy.com.br"))
        assertFalse(AdultContentDetector.isBlockedHost("www.nubank.com.br"))
        assertFalse(AdultContentDetector.isBlockedHost("www.google.com"))
        assertFalse(AdultContentDetector.isBlockedHost("github.com"))
    }

    @Test
    fun sourceManifestKeepsBankSafeAccessibilityWithoutDropperPermissions() {
        val manifest = readAppFile("src/main/AndroidManifest.xml").readText()
        val config = readAppFile("src/main/res/xml/accessibility_service_config.xml").readText()
        assertTrue(manifest.contains("BIND_ACCESSIBILITY_SERVICE"))
        assertTrue(manifest.contains("ReelsBlockAccessibilityService"))
        assertFalse(manifest.contains("REQUEST_INSTALL_PACKAGES"))
        assertFalse(manifest.contains("FileProvider"))
        assertFalse(manifest.contains("FILE_PROVIDER_PATHS"))
        assertFalse(
            File("app/src/main/res/xml/file_paths.xml").exists() ||
                File("src/main/res/xml/file_paths.xml").exists()
        )
        assertFalse(manifest.contains("SYSTEM_ALERT_WINDOW"))
        assertFalse(manifest.contains("QUERY_ALL_PACKAGES"))
        assertFalse(
            "Usage Access is not required for Reels and lets banking SDKs see every foreground app",
            manifest.contains("PACKAGE_USAGE_STATS")
        )
        assertFalse(manifest.contains("AppMonitorService"))
        assertFalse("full-screen intents are revoked for non call/alarm apps", manifest.contains("USE_FULL_SCREEN_INTENT"))
        assertFalse("Blockfy is not a declared accessibility tool", config.contains("isAccessibilityTool"))
        assertFalse(manifest.contains("VpnService") || manifest.contains("BIND_VPN_SERVICE"))
        assertTrue(config.contains("com.instagram.android"))
        assertTrue(config.contains("flagReportViewIds"))
        assertFalse(config.contains("flagRetrieveInteractiveWindows"))
        assertFalse("typed text is never read", config.contains("typeViewTextChanged"))
        assertTrue(
            "without this flag Android hides the Reels containers the service looks for",
            config.contains("flagIncludeNotImportantViews")
        )
        assertFalse(config.contains("telegram") || config.contains("reddit"))
        assertTrue(config.contains("canPerformGestures=\"false\""))
        BankPackages.ALL.forEach { bank ->
            assertFalse("a11y config must not include $bank", config.contains(bank))
        }
    }

    @Test
    fun accessibilityServiceStaysSandboxedToSocialPackages() {
        assertTrue(readAppFile("src/main/res/xml/accessibility_service_config.xml").exists())
        assertTrue(
            readAppFile("src/main/java/com/buenotty/blockfy/feature_accessibility/ReelsBlockAccessibilityService.kt").exists()
        )
        val service = readAppFile("src/main/java/com/buenotty/blockfy/feature_accessibility/ReelsBlockAccessibilityService.kt").readText()
        assertFalse(service.contains("TYPE_ACCESSIBILITY_OVERLAY"))
        assertFalse(service.contains("REQUEST_INSTALL_PACKAGES"))
        assertFalse(
            "reading the active window scrapes whichever app is open, including Nubank",
            service.contains("rootInActiveWindow")
        )
        assertTrue(service.contains("event.source"))
        assertTrue("the service must only listen to the packages ListeningScope allows", service.contains("ListeningScope"))
    }

    @Test
    fun adultMatcherDoesNotTreatJeromeAsErome() {
        assertFalse(AdultContentDetector.isAdultContent("jerome"))
        assertFalse(AdultContentDetector.isAdultContent("Hello Jerome, see you tomorrow"))
        assertFalse(AdultContentDetector.containsToken("jerome", "erome"))
        assertTrue(AdultContentDetector.containsToken("erome.com", "erome"))
        assertTrue(AdultContentDetector.isBlockedHost("erome.com"))
        assertTrue(AdultContentDetector.isBlockedHost("www.erome.com"))
        assertTrue(AdultContentDetector.isAdultContent("https://www.erome.com/user/demo"))
        assertTrue(AdultContentDetector.isAdultContent("watch porn tonight"))
        assertFalse(AdultContentDetector.isAdultContent("nubank.com.br"))
        assertFalse(AdultContentDetector.isAdultContent("github.com"))
        assertFalse(AdultContentDetector.isBlockedHost("jerome.com"))
    }

    private fun readAppFile(relativeFromApp: String): File {
        val candidates = listOf(
            File(relativeFromApp),
            File("app/$relativeFromApp"),
            File("../app/$relativeFromApp")
        )
        return candidates.firstOrNull { it.exists() || it.parentFile?.exists() == true } ?: File(relativeFromApp)
    }
}
