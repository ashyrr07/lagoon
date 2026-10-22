package com.lagoon.island.sources.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressSnapshotTest {
    @Test
    fun `fraction from progress and max`() {
        assertEquals(0.25f, ProgressSnapshot(25, 100, false).fraction!!, 0.0001f)
        assertEquals(0.5f, ProgressSnapshot(150, 300, false).fraction!!, 0.0001f)
    }

    @Test
    fun `indeterminate or empty bounds give no fraction`() {
        assertNull(ProgressSnapshot(25, 100, true).fraction)
        assertNull(ProgressSnapshot(0, 0, false).fraction)
    }

    @Test
    fun `fraction is clamped`() {
        assertEquals(1f, ProgressSnapshot(400, 300, false).fraction!!, 0.0001f)
        assertEquals(0f, ProgressSnapshot(-5, 300, false).fraction!!, 0.0001f)
    }
}
