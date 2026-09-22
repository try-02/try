package com.sentral.org.ui.screen.pos

import com.sentral.org.data.model.PosDataException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PesanErrorTest {

    @Test
    fun `ProductInactive menyebut nama produk dan arahan tindakan`() {
        val pesan = PosDataException.ProductInactive(7L, "Kopi Susu").pesanPengguna()
        assertEquals(
            "Produk 'Kopi Susu' tidak aktif. Hapus dari keranjang untuk melanjutkan.",
            pesan,
        )
    }

    @Test
    fun `pesan domain dipakai apa adanya karena memang siap tampil`() {
        assertEquals(
            "Keranjang kosong",
            PosDataException.Validation("Keranjang kosong").pesanPengguna(),
        )
        assertEquals(
            "Kasir tidak ditemukan",
            PosDataException.NotFound("Kasir tidak ditemukan").pesanPengguna(),
        )
    }

    @Test
    fun `exception teknis tidak pernah membocorkan detail internal`() {
        val pesan = IllegalStateException("SELECT * FROM pin_hash WHERE ...").pesanPengguna()
        assertEquals("Terjadi kesalahan tak terduga. Silakan coba lagi.", pesan)
        assertFalse(pesan.contains("SELECT"))
        assertFalse(pesan.contains("pin_hash"))
    }
}