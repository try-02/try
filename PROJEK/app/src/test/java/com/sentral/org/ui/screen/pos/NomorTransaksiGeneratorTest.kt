package com.sentral.org.ui.screen.pos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NomorTransaksiGeneratorTest {

    @Test
    fun `mengikuti pola TRX-tanggal-jam-segmen acak`() {
        val nomor = NomorTransaksiGenerator.buat(1_700_000_000_000L)
        assertTrue(
            "pola salah: $nomor",
            nomor.matches(Regex("^TRX-\\d{8}-\\d{6}-\\d{3}$")),
        )
    }

    @Test
    fun `timestamp berbeda menghasilkan nomor berbeda`() {
        val a = NomorTransaksiGenerator.buat(1_700_000_000_000L)
        val b = NomorTransaksiGenerator.buat(1_700_000_001_000L)
        assertNotEquals(a, b)
    }

    @Test
    fun `segmen acak selalu tiga digit`() {
        repeat(50) {
            val nomor = NomorTransaksiGenerator.buat(System.currentTimeMillis())
            val segmen = nomor.substringAfterLast('-')
            assertEquals(3, segmen.length)
            assertTrue(segmen.toInt() in 100..999)
        }
    }
}