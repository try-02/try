package com.sentral.org.ui.screen.riwayat

import android.net.Uri
import com.sentral.org.data.entity.ItemTransaksiEntity
import com.sentral.org.data.entity.TransaksiDenganDetail
import com.sentral.org.data.model.MetodePembayaran
import com.sentral.org.data.model.TransaksiFilter
import com.sentral.org.data.model.TujuanStokPengembalian

/** Model baris item untuk form input retur */
data class ItemReturUi(
    val item: ItemTransaksiEntity,
    val sisaQtyScaled: Long,
    val qtyPilihanScaled: Long = 0L,
    val tujuan: TujuanStokPengembalian = TujuanStokPengembalian.NORMAL,
)

/** State UI untuk layar Riwayat Transaksi. */
data class RiwayatUiState(
    val filter: TransaksiFilter = TransaksiFilter(),
    val detailTerpilih: TransaksiDenganDetail? = null,
    val sedangMemuatDetail: Boolean = false,
    val sedangReprint: Boolean = false,
    val sedangEkspor: Boolean = false,
    val sedangMemprosesAksi: Boolean = false,

    // Dialog Void
    val dialogVoidTerbuka: Boolean = false,
    val alasanVoidInput: String = "",

    // Sheet Retur
    val dialogReturTerbuka: Boolean = false,
    val daftarItemRetur: List<ItemReturUi> = emptyList(),
    val metodeRefundRetur: MetodePembayaran = MetodePembayaran.CASH,
    val catatanReturInput: String = "",
)

/** Event satu kali jalan (Side Effect) untuk Snackbar atau aksi eksternal. */
sealed interface RiwayatEvent {
    data class Pesan(val teks: String, val jenis: Jenis = Jenis.INFO) : RiwayatEvent {
        enum class Jenis { INFO, SUKSES, GALAT }
    }
    data class ReprintSukses(val nomorTransaksi: String) : RiwayatEvent
    data class VoidSukses(val nomorTransaksi: String) : RiwayatEvent
    data class ReturSukses(val nomorTransaksi: String, val totalRefund: Long) : RiwayatEvent
    data class FileSiapDibagikan(
        val uri: Uri,
        val mimeType: String = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    ) : RiwayatEvent
}