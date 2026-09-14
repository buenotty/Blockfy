package com.robingebert.blokky

import com.robingebert.blokky.datastore.AppSettings
import com.robingebert.blokky.datastore.DailyUsage
import com.robingebert.blokky.feature_accessibility.AdultContentDetector
import com.robingebert.blokky.feature_monitor.BankPackages
import com.robingebert.blokky.feature_monitor.BlockPolicy
import com.robingebert.blokky.feature_monitor.BlockReason
import com.robingebert.blokky.feature_monitor.TrackedPackages
import com.robingebert.blokky.feature_vpn.DnsPackets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BankSafetyArchitectureTest {

    @Test
    fun trackedPackagesDoNotIncludeBanks() {
        BankPackages.ALL.forEach { bank ->
            assertFalse("$bank must never be monitored", TrackedPackages.isTracked(bank))
        }
        assertFalse(TrackedPackages.isTracked("com.nu.production"))
        assertTrue(TrackedPackages.isTracked("com.instagram.android"))
    }

    @Test
    fun scheduleBlocksInstagramInsideWindowWithoutReadingScreen() {
        val settings = AppSettings(
            instagram = AppSettings().instagram.copy(blocked = true, blockedStart = 0, blockedEnd = 1439, dailyLimitMinutes = 0)
        )
        val verdict = BlockPolicy.evaluate(
            packageName = TrackedPackages.INSTAGRAM,
            settings = settings,
            usage = DailyUsage(),
            minuteOfDay = 12 * 60
        )
        assertTrue(verdict.shouldBlock)
        assertEquals(BlockReason.SCHEDULE, verdict.reason)
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
        val underLimit = BlockPolicy.evaluate(
            TrackedPackages.YOUTUBE,
            settings,
            DailyUsage(youtubeSeconds = 9 * 60),
            minuteOfDay = 60
        )
        val overLimit = BlockPolicy.evaluate(
            TrackedPackages.YOUTUBE,
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
    fun dnsBlocksAdultHostsAndIgnoresNormalSites() {
        assertTrue(AdultContentDetector.isBlockedHost("www.pornhub.com"))
        assertTrue(AdultContentDetector.isBlockedHost("m.xvideos.com"))
        assertTrue(AdultContentDetector.isBlockedHost("privacy.com.br"))
        assertFalse(AdultContentDetector.isBlockedHost("www.nubank.com.br"))
        assertFalse(AdultContentDetector.isBlockedHost("www.google.com"))
        assertFalse(AdultContentDetector.isBlockedHost("github.com"))
    }

    @Test
    fun dnsQueryNameAndBlockedResponseAreValid() {
        val query = buildDnsQuery("pornhub.com", qtype = 1)
        assertEquals("pornhub.com", DnsPackets.readQueryName(query))
        assertTrue(DnsPackets.isBlockedHost(DnsPackets.readQueryName(query)))
        val response = DnsPackets.buildBlockedResponse(query)
        assertNotNull(response)
        assertEquals(1, response!![7].toInt() and 0xFF)
    }

    @Test
    fun sourceManifestHasNoBankTriggerPermissions() {
        val manifest = readAppFile("src/main/AndroidManifest.xml").readText()
        assertFalse(manifest.contains("BIND_ACCESSIBILITY_SERVICE"))
        assertFalse(manifest.contains("REQUEST_INSTALL_PACKAGES"))
        assertFalse(manifest.contains("SYSTEM_ALERT_WINDOW"))
        assertFalse(manifest.contains("QUERY_ALL_PACKAGES"))
        assertFalse(manifest.contains("AccessibilityService"))
        assertTrue(manifest.contains("PACKAGE_USAGE_STATS"))
        assertTrue(manifest.contains("AppMonitorService"))
        assertTrue(manifest.contains("AdultBlockVpnService"))
    }

    @Test
    fun accessibilityServiceFilesWereRemoved() {
        assertFalse(readAppFile("src/main/res/xml/accessibility_service_config.xml").exists())
        assertFalse(
            readAppFile("src/main/java/com/robingebert/blokky/feature_accessibility/ReelsBlockAccessibilityService.kt").exists()
        )
    }

    private fun readAppFile(relativeFromApp: String): File {
        val candidates = listOf(
            File(relativeFromApp),
            File("app/$relativeFromApp"),
            File("../app/$relativeFromApp")
        )
        return candidates.firstOrNull { it.exists() || it.parentFile?.exists() == true } ?: File(relativeFromApp)
    }

    private fun buildDnsQuery(host: String, qtype: Int): ByteArray {
        val labels = host.split('.')
        val size = 12 + labels.sumOf { 1 + it.length } + 1 + 4
        val out = ByteArray(size)
        out[0] = 0x12
        out[1] = 0x34
        out[2] = 0x01
        out[5] = 0x01
        var index = 12
        for (label in labels) {
            out[index] = label.length.toByte()
            index++
            val bytes = label.toByteArray(Charsets.US_ASCII)
            System.arraycopy(bytes, 0, out, index, bytes.size)
            index += bytes.size
        }
        out[index] = 0
        index++
        out[index] = ((qtype shr 8) and 0xFF).toByte()
        out[index + 1] = (qtype and 0xFF).toByte()
        out[index + 2] = 0
        out[index + 3] = 1
        return out
    }
}
