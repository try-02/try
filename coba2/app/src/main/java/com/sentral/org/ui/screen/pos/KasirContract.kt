package com.sentral.org.ui.screen.pos

import com.sentral.org.data.entity.KeranjangEntity
import com.sentral.org.data.entity.ProdukEntity

/** Satu baris keranjang utk UI. Harga SELALU live dari master produk. */
data class BarisKeranjangUi(
    val itemId: Long,
    val produkId: Long,
    val nama: String,
    val hargaSatuan: Long,
    val jumlahScaled: Long,
    val totalBaris: Long,
)

data class KasirUiState(
    val produk: List<ProdukEntity> = emptyList(),
    val keranjangTerbuka: List<KeranjangEntity> = emptyList(),
    val keranjangAktifId: Long? = null,
    val baris: List<BarisKeranjangUi> = emptyList(),
    val subtotal: Long = 0,
    val sedangProses: Boolean = false,
) {
    val jumlahJenisItem: Int get() = baris.size
}

/** Kejadian sekali-tampil (snackbar/dialog). Bukan bagian dari state. */
sealed interface KasirEvent {
    data class Pesan(val teks: String, val jenis: Jenis) : KasirEvent {
        enum class Jenis { INFO, SUKSES, GALAT }
    }
    data class CheckoutBerhasil(val nomorTransaksi: String, val kembalian: Long) : KasirEvent
}