package com.buenotty.blockfy

import com.buenotty.blockfy.feature_accessibility.ServiceHealth
import com.buenotty.blockfy.feature_accessibility.isAggressiveBatteryVendor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceHealthTest {

    @Test
    fun `listed but not bound is stalled, the state a killed service leaves behind`() {
        assertEquals(ServiceHealth.STALLED, ServiceHealth.of(listed = true, bound = false))
    }

    @Test
    fun `listed and bound is running`() {
        assertEquals(ServiceHealth.RUNNING, ServiceHealth.of(listed = true, bound = true))
    }

    @Test
    fun `not listed is off whatever the bound flag says`() {
        assertEquals(ServiceHealth.OFF, ServiceHealth.of(listed = false, bound = false))
        assertEquals(ServiceHealth.OFF, ServiceHealth.of(listed = false, bound = true))
    }

    @Test
    fun `xiaomi family is flagged, pixel is not`() {
        assertTrue(isAggressiveBatteryVendor("Xiaomi"))
        assertTrue(isAggressiveBatteryVendor("POCO"))
        assertFalse(isAggressiveBatteryVendor("Google"))
    }
}
