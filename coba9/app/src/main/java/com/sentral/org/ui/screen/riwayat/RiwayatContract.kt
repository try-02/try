package com.sentral.org.ui.screen.riwayat

import android.net.Uri
import com.sentral.org.data.entity.TransaksiDenganDetail
import com.sentral.org.data.model.TransaksiFilter

/** State UI untuk layar Riwayat Transaksi. */
data class RiwayatUiState(
    val filter: TransaksiFilter = TransaksiFilter(),
    val detailTerpilih: TransaksiDenganDetail? = null,
    val sedangMemuatDetail: Boolean = false,
    val sedangReprint: Boolean = false,
    val sedangEkspor: Boolean = false,
)

/** Event satu kali jalan (Side Effect) untuk Snackbar atau aksi eksternal. */
sealed interface RiwayatEvent {
    data class Pesan(val teks: String, val jenis: Jenis = Jenis.INFO) : RiwayatEvent {
        enum class Jenis { INFO, SUKSES, GALAT }
    }
    data class ReprintSukses(val nomorTransaksi: String) : RiwayatEvent
    data class FileSiapDibagikan(
        val uri: Uri,
        val mimeType: String = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    ) : RiwayatEvent
}