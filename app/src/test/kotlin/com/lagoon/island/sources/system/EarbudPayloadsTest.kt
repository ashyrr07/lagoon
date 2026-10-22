package com.lagoon.island.sources.system

import com.lagoon.island.engine.ActivityContent.Bluetooth.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EarbudPayloadsTest {
    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    @Test
    fun `airpods pro frame, primary pod is left`() {
        // 07 19 01 0E20 | status 0x2B (bit 0x20 set) | pods 0x98 | charging 0x4, case 0xA
        val frame = bytes(0x07, 0x19, 0x01, 0x0E, 0x20, 0x2B, 0x98, 0x4A, 0x00, 0x00, 0x00)
        val levels = AirPodsPayload.parse(frame)!!
        assertEquals(80, levels.left)
        assertEquals(90, levels.right)
        assertEquals(100, levels.case)
        assertTrue(levels.caseCharging)
        assertFalse(levels.leftCharging)
        assertFalse(levels.rightCharging)
        assertEquals(0x0E20, levels.model)
    }

    @Test
    fun `airpods frame flipped swaps the nibbles and charging bits`() {
        val frame = bytes(0x07, 0x19, 0x01, 0x0F, 0x20, 0x0B, 0x98, 0x15, 0x00, 0x00, 0x00)
        val levels = AirPodsPayload.parse(frame)!!
        assertEquals(90, levels.left)
        assertEquals(80, levels.right)
        assertEquals(50, levels.case)
        assertTrue(levels.leftCharging)
        assertFalse(levels.rightCharging)
        assertFalse(levels.caseCharging)
    }

    @Test
    fun `airpods unknown nibbles are null and never charging`() {
        val frame = bytes(0x07, 0x19, 0x01, 0x0E, 0x20, 0x2B, 0xF8, 0x7F, 0x00, 0x00, 0x00)
        val levels = AirPodsPayload.parse(frame)!!
        assertEquals(80, levels.left)
        assertNull(levels.right)
        assertNull(levels.case)
        assertFalse(levels.caseCharging)
        assertTrue(levels.hasAny)
    }

    @Test
    fun `airpods rejects other apple frames and short data`() {
        assertNull(AirPodsPayload.parse(bytes(0x10, 0x05, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06)))
        assertNull(AirPodsPayload.parse(bytes(0x07, 0x19, 0x01)))
        assertNull(AirPodsPayload.parse(null))
    }

    @Test
    fun `fast pair battery field after account key filter and salt`() {
        val frame = bytes(
            0x00, // flags
            0x60, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, // account key filter, length 6, type 0
            0x11, 0xC7, // salt, length 1
            0x33, 90, 0xD2, 0x7F, // battery (show UI), length 3: left 90, right 82 charging, case unknown
        )
        val levels = FastPairPayload.parseBattery(frame)!!
        assertEquals(90, levels.left)
        assertEquals(82, levels.right)
        assertNull(levels.case)
        assertFalse(levels.leftCharging)
        assertTrue(levels.rightCharging)
    }

    @Test
    fun `fast pair hide-ui battery type also parses`() {
        val frame = bytes(0x00, 0x34, 0x64, 0x64, 0xE4)
        val levels = FastPairPayload.parseBattery(frame)!!
        assertEquals(100, levels.left)
        assertEquals(100, levels.right)
        assertEquals(100, levels.case)
        assertTrue(levels.caseCharging)
    }

    @Test
    fun `fast pair without battery or truncated gives nothing`() {
        assertNull(FastPairPayload.parseBattery(bytes(0x00, 0x60, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66)))
        assertNull(FastPairPayload.parseBattery(bytes(0x00, 0x33, 0x64)))
        assertNull(FastPairPayload.parseBattery(bytes(0x12, 0x34, 0x56))) // pairing-mode model id
    }

    @Test
    fun `iphoneaccev battery key`() {
        assertEquals(70, IphoneAccev.parseBatteryPercent(listOf(2, 1, 6, 2, 0)))
        assertEquals(100, IphoneAccev.parseBatteryPercent(listOf("1", "1", "9")))
        assertNull(IphoneAccev.parseBatteryPercent(listOf(1, 2, 1)))
        assertNull(IphoneAccev.parseBatteryPercent(emptyList()))
        assertNull(IphoneAccev.parseBatteryPercent(null))
    }

    @Test
    fun `device kinds from class and name`() {
        assertEquals(Kind.Earbuds, BluetoothKinds.kind(0x0404, "Galaxy Buds2 Pro"))
        assertEquals(Kind.Earbuds, BluetoothKinds.kind(0x0404, null))
        assertEquals(Kind.Headphones, BluetoothKinds.kind(0x0418, "WH-1000XM5"))
        assertEquals(Kind.Headphones, BluetoothKinds.kind(0x0404, "AirPods Max"))
        assertEquals(Kind.Earbuds, BluetoothKinds.kind(0x0404, "AirPods Pro"))
        assertEquals(Kind.Watch, BluetoothKinds.kind(0x0704, "Galaxy Watch6"))
        assertEquals(Kind.Watch, BluetoothKinds.kind(0x0700, null))
        assertEquals(Kind.Speaker, BluetoothKinds.kind(0x0414, "JBL Flip 6"))
        assertEquals(Kind.Other, BluetoothKinds.kind(0x0104, "My Laptop"))
        assertEquals(Kind.Other, BluetoothKinds.kind(null, null))
        assertTrue(BluetoothKinds.isAudio(Kind.Earbuds))
        assertFalse(BluetoothKinds.isAudio(Kind.Watch))
    }
}
