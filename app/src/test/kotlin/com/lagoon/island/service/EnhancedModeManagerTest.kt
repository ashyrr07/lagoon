package com.lagoon.island.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnhancedModeManagerTest {
    private val pkg = "com.lagoon.island"
    private val cls = "com.lagoon.island.service.IslandAccessibilityService"

    @Test
    fun `full form entry is found among others`() {
        val value = "com.other/com.other.Svc:$pkg/$cls:com.third/.Talk"
        assertTrue(EnhancedModeManager.isServiceListed(value, pkg, cls))
    }

    @Test
    fun `short form entry matches`() {
        assertTrue(EnhancedModeManager.isServiceListed("$pkg/.service.IslandAccessibilityService", pkg, cls))
    }

    @Test
    fun `match is case insensitive and tolerant of whitespace`() {
        assertTrue(EnhancedModeManager.isServiceListed(" ${pkg.uppercase()}/$cls ", pkg, cls))
    }

    @Test
    fun `debug application id with main-package class name matches full form only`() {
        val debugPkg = "com.lagoon.island.debug"
        assertTrue(EnhancedModeManager.isServiceListed("$debugPkg/$cls", debugPkg, cls))
        assertFalse(EnhancedModeManager.isServiceListed("$pkg/$cls", debugPkg, cls))
    }

    @Test
    fun `empty, null and unrelated values are not enabled`() {
        assertFalse(EnhancedModeManager.isServiceListed(null, pkg, cls))
        assertFalse(EnhancedModeManager.isServiceListed("", pkg, cls))
        assertFalse(EnhancedModeManager.isServiceListed("com.other/com.other.Svc", pkg, cls))
        assertFalse(EnhancedModeManager.isServiceListed("$pkg/$cls.Other", pkg, cls))
    }
}
