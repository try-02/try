package com.sentral.org.ui.screen.riwayat

import com.sentral.org.data.entity.TransaksiDenganDetail
import com.sentral.org.data.model.TransaksiFilter

/** State UI untuk layar Riwayat Transaksi. */
data class RiwayatUiState(
    val filter: TransaksiFilter = TransaksiFilter(),
    val detailTerpilih: TransaksiDenganDetail? = null,
    val sedangMemuatDetail: Boolean = false,
    val sedangReprint: Boolean = false,
)

/** Event satu kali jalan (Side Effect) untuk Snackbar atau feedback visual. */
sealed interface RiwayatEvent {
    data class Pesan(val teks: String, val jenis: Jenis = Jenis.INFO) : RiwayatEvent {
        enum class Jenis { INFO, SUKSES, GALAT }
    }
    data class ReprintSukses(val nomorTransaksi: String) : RiwayatEvent
}