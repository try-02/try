package com.sentral.org.ui.screen.laporan

import androidx.compose.runtime.Immutable
import com.sentral.org.data.model.LaporanPembayaran
import com.sentral.org.data.model.LaporanPenjualan
import com.sentral.org.data.model.TopProdukLaporan

enum class LaporanRange(
    val label: String,
) {
    HARI_INI("Hari Ini"),
    TUJUH_HARI("7 Hari"),
    TIGA_PULUH_HARI("30 Hari"),
}

@Immutable
data class LaporanPenjualanUi(
    val omzetPenjualan: Long,
    val totalRetur: Long,
    val omzetBersih: Long,
    val hppHistoris: Long,
    val hppRetur: Long,
    val hppBersih: Long,
    val labaKotor: Long,
    val marginPersen: Double,
    val jumlahTransaksiSelesai: Int,
    val jumlahTransaksiVoid: Int,
    val rataRataTransaksi: Long,
    val pembayaran: List<LaporanPembayaran>,
    val topProduk: List<TopProdukLaporan>,
)

fun LaporanPenjualan.toUi() =
    LaporanPenjualanUi(
        omzetPenjualan = omzetPenjualan,
        totalRetur = totalRetur,
        omzetBersih = omzetBersih,
        hppHistoris = hppHistoris,
        hppRetur = hppRetur,
        hppBersih = hppBersih,
        labaKotor = labaKotor,
        marginPersen = marginPersen,
        jumlahTransaksiSelesai = jumlahTransaksiSelesai,
        jumlahTransaksiVoid = jumlahTransaksiVoid,
        rataRataTransaksi = rataRataTransaksi,
        pembayaran = pembayaran,
        topProduk = topProduk,
    )

data class LaporanUiState(
    val range: LaporanRange = LaporanRange.HARI_INI,
    val data: LaporanPenjualanUi? = null,
    val sedangMemuat: Boolean = true,
)
