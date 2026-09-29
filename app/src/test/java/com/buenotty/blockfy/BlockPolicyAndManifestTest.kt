package com.buenotty.blockfy

import com.buenotty.blockfy.datastore.AppSettings
import com.buenotty.blockfy.datastore.DailyUsage
import com.buenotty.blockfy.feature_accessibility.AdultContentDetector
import com.buenotty.blockfy.fixtures.BankPackages
import com.buenotty.blockfy.feature_monitor.BlockPolicy
import com.buenotty.blockfy.feature_monitor.BlockReason
import com.buenotty.blockfy.feature_monitor.TrackedPackages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BlockPolicyAndManifestTest {

    @Test
    fun trackedPackagesDoNotIncludeBanks() {
        BankPackages.ALL.forEach { bank ->
            assertFalse("$bank must never be monitored", TrackedPackages.isTracked(bank))
        }
        assertFalse(TrackedPackages.isTracked("com.nu.production"))
        assertTrue(TrackedPackages.isTracked("com.instagram.android"))
    }

    @Test
    fun scheduleBlocksInstagramReelsNotTheWholeApp() {
        val settings = AppSettings(
            instagram = AppSettings().instagram.copy(blocked = true, blockedStart = 0, blockedEnd = 1439, dailyLimitMinutes = 0)
        )
        val wholeApp = BlockPolicy.evaluate(
            packageName = TrackedPackages.INSTAGRAM,
            settings = settings,
            usage = DailyUsage(),
            minuteOfDay = 12 * 60
        )
        val shorts = BlockPolicy.evaluateShorts(
            appName = "Instagram",
            settings = settings,
            usage = DailyUsage(),
            minuteOfDay = 12 * 60
        )
        assertFalse(wholeApp.shouldBlock)
        assertTrue(shorts.shouldBlock)
        assertEquals(BlockReason.SCHEDULE, shorts.reason)
    }

    @Test
    fun bankPackageNeverProducesABlockVerdict() {
        val settings = AppSettings(
            instagram = AppSettings().instagram.copy(blocked = true, dailyLimitMinutes = 0)
        )
        val verdict = BlockPolicy.evaluate(
            packageName = "com.nu.production",
            settings = settings,
            usage = DailyUsage(),
            minuteOfDay = 100
        )
        assertFalse(verdict.shouldBlock)
        assertEquals(BlockReason.NONE, verdict.reason)
    }

    @Test
    fun dailyLimitUsesUsageStatsCounters() {
        val settings = AppSettings(
            youtube = AppSettings().youtube.copy(blocked = true, dailyLimitMinutes = 10)
        )
        val underLimit = BlockPolicy.evaluateShorts(
            "YouTube",
            settings,
            DailyUsage(youtubeSeconds = 9 * 60),
            minuteOfDay = 60
        )
        val overLimit = BlockPolicy.evaluateShorts(
            "YouTube",
            settings,
            DailyUsage(youtubeSeconds = 10 * 60),
            minuteOfDay = 60
        )
        assertFalse(underLimit.shouldBlock)
        assertEquals(BlockReason.DAILY_LIMIT, overLimit.reason)
    }

    @Test
    fun totalAppLimitBlocksEvenWhenFeatureToggleIsOff() {
        val settings = AppSettings(
            tiktok = AppSettings().tiktok.copy(blocked = false, appTotalDailyLimitMinutes = 15)
        )
        val verdict = BlockPolicy.evaluate(
            TrackedPackages.TIKTOK,
            settings,
            DailyUsage(tiktokTotalSeconds = 15 * 60),
            minuteOfDay = 60
        )
        assertEquals(BlockReason.TOTAL_LIMIT, verdict.reason)
    }

    @Test
    fun overnightScheduleWindowWrapsMidnight() {
        assertTrue(BlockPolicy.isWithinInterval(22 * 60, 6 * 60, 23 * 60))
        assertTrue(BlockPolicy.isWithinInterval(22 * 60, 6 * 60, 30))
        assertFalse(BlockPolicy.isWithinInterval(22 * 60, 6 * 60, 12 * 60))
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
        assertTrue(service.contains("SOCIAL_PACKAGES"))
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

    @Test
    fun weekdayMaskSelectsTheRightDays() {
        val utc = java.util.TimeZone.getTimeZone("UTC")
        val monday = 1_767_571_200_000L // 2026-01-05 00:00 UTC, a Monday
        val mondayOnly = 1 shl 1
        assertTrue(BlockPolicy.isActiveWeekday(mondayOnly, monday, utc))
        assertFalse(BlockPolicy.isActiveWeekday(mondayOnly, monday + 86_400_000L, utc))
        assertTrue(BlockPolicy.isActiveWeekday(127, monday + 86_400_000L, utc))
    }

    @Test
    fun wholeAppOnlyAppsAreBlockedByScheduleAndNeverByShortsRules() {
        val settings = AppSettings(
            tiktok = AppSettings().tiktok.copy(blocked = true, blockedStart = 0, blockedEnd = 1439, appTotalDailyLimitMinutes = 0)
        )
        val whole = BlockPolicy.evaluate(TrackedPackages.TIKTOK, settings, DailyUsage(), minuteOfDay = 600)
        val shorts = BlockPolicy.evaluateShorts("TikTok", settings, DailyUsage(), minuteOfDay = 600)
        assertEquals(BlockReason.SCHEDULE, whole.reason)
        assertFalse(shorts.shouldBlock)
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
