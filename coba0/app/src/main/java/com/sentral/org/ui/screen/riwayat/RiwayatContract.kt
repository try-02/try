package com.sentral.org.ui.screen.riwayat

import android.net.Uri
import com.sentral.org.data.entity.TransaksiDenganDetail
import com.sentral.org.data.entity.TransaksiEntity
import com.sentral.org.data.model.MetodePembayaran
import com.sentral.org.data.model.StatusTransaksi
import com.sentral.org.data.model.TransaksiFilter
import com.sentral.org.data.model.TujuanStokPengembalian

/** Model kartu riwayat transaksi (LazyColumn item) */
data class TransaksiItemUi(
    val id: Long,
    val nomorTransaksi: String,
    val namaKasir: String,
    val dibuatPada: Long,
    val total: Long,
    val status: StatusTransaksi,
)

fun TransaksiEntity.toUiModel() =
    TransaksiItemUi(
        id = id,
        nomorTransaksi = nomorTransaksi,
        namaKasir = namaKasir,
        dibuatPada = dibuatPada,
        total = total,
        status = status,
    )

/** Model item rincian transaksi untuk Sheet Detail */
data class ItemTransaksiDetailUi(
    val id: Long,
    val namaProduk: String,
    val jumlah: Long,
    val hargaSatuan: Long,
    val diskonItem: Long,
    val totalBaris: Long,
)

/** Model agregat transaksi untuk Sheet Detail */
data class TransaksiDetailUi(
    val id: Long,
    val nomorTransaksi: String,
    val namaKasir: String,
    val dibuatPada: Long,
    val total: Long,
    val status: StatusTransaksi,
    val alasanPembatalan: String?,
    val items: List<ItemTransaksiDetailUi>,
)

fun TransaksiDenganDetail.toUiModel() =
    TransaksiDetailUi(
        id = transaksi.id,
        nomorTransaksi = transaksi.nomorTransaksi,
        namaKasir = transaksi.namaKasir,
        dibuatPada = transaksi.dibuatPada,
        total = transaksi.total,
        status = transaksi.status,
        alasanPembatalan = transaksi.alasanPembatalan,
        items =
            items.map {
                ItemTransaksiDetailUi(
                    id = it.id,
                    namaProduk = it.namaProduk,
                    jumlah = it.jumlah,
                    hargaSatuan = it.hargaSatuan,
                    diskonItem = it.diskonItem,
                    totalBaris = it.totalBaris,
                )
            },
    )

/** Model baris item untuk form input retur */
data class ItemReturUi(
    val itemId: Long,
    val namaProduk: String,
    val sisaQtyScaled: Long,
    val qtyPilihanScaled: Long = 0L,
    val tujuan: TujuanStokPengembalian = TujuanStokPengembalian.NORMAL,
)

/** State UI untuk layar Riwayat Transaksi. */
data class RiwayatUiState(
    val filter: TransaksiFilter = TransaksiFilter(),
    val detailTerpilih: TransaksiDetailUi? = null,
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
    data class Pesan(
        val teks: String,
        val jenis: Jenis = Jenis.INFO,
    ) : RiwayatEvent {
        enum class Jenis { INFO, SUKSES, GALAT }
    }

    data class ReprintSukses(
        val nomorTransaksi: String,
    ) : RiwayatEvent

    data class VoidSukses(
        val nomorTransaksi: String,
    ) : RiwayatEvent

    data class ReturSukses(
        val nomorTransaksi: String,
        val totalRefund: Long,
    ) : RiwayatEvent

    data class FileSiapDibagikan(
        val uri: Uri,
        val mimeType: String = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    ) : RiwayatEvent
}
