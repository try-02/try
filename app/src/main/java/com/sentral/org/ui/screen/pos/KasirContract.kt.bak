package com.sentral.org.ui.screen.pos

import androidx.compose.runtime.Immutable
import com.sentral.org.data.entity.KeranjangEntity
import com.sentral.org.data.entity.ProdukEntity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Satu baris keranjang utk UI. Harga SELALU live dari master produk.
 *
 * `@Immutable` karena seluruh properinya `val` primitif/String. Jangan tambah `var`,
 * `List`, `Map`, atau lambda di sini tanpa mencabut annotation ini dulu — kalau
 * stabilitasnya jadi tidak benar, compiler akan men-skip recomposisi dan UI jadi basi.
 */
@Immutable
data class BarisKeranjangUi(
    val itemId: Long,
    val produkId: Long,
    val nama: String,
    val hargaSatuan: Long,
    val jumlahScaled: Long,
    val totalBaris: Long,
)

/**
 * Satu baris produk utk panel katalog POS, lengkap dgn stok yg sudah di-join.
 *
 * Stok sengaja ikut di sini (bukan `Map<Long, Long>` terpisah) supaya item komposisi cuma
 * menerima `Long?` — map inventaris penuh tidak pernah masuk ke composable, sehingga
 * perubahan stok satu produk tidak memaksa seluruh list dievaluasi ulang.
 */
@Immutable
data class ProdukPanelUi(
    val id: Long,
    val nama: String,
    val harga: Long,
    val kategori: String,
    val stok: Long?,
)

data class KasirUiState(
    val produk: List<ProdukEntity> = emptyList(),
    val keranjangTerbuka: ImmutableList<KeranjangEntity> = persistentListOf(),
    val keranjangAktifId: Long? = null,
    val baris: List<BarisKeranjangUi> = emptyList(),
    val subtotal: Long = 0,
    val sedangProses: Boolean = false,
) {
    val jumlahJenisItem: Int get() = baris.size
}

/** Kejadian sekali-tampil (snackbar/dialog). Bukan bagian dari state. */
sealed interface KasirEvent {
    data class Pesan(
        val teks: String,
        val jenis: Jenis,
    ) : KasirEvent {
        enum class Jenis { INFO, SUKSES, GALAT }
    }

    /**
     * Baris keranjang dihapus (swipe) — bawa produkId + jumlahScaled asli
     * supaya tombol UNDO mengembalikan quantity yang tepat (bukan cuma 1 unit).
     */
    data class HapusBaris(
        val nama: String,
        val produkId: Long,
        val jumlahScaled: Long,
    ) : KasirEvent

    data class CheckoutBerhasil(
        val nomorTransaksi: String,
        val kembalian: Long,
    ) : KasirEvent
}
