package com.sentral.org.data.model

data class LaporanPenjualan(
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

data class LaporanPembayaran(
    val metode: MetodePembayaran,
    val nominal: Long,
    val persentase: Double,
)

data class TopProdukLaporan(
    val produkId: Long,
    val nama: String,
    val jumlahTerjualScaled: Long,
    val totalNominal: Long,
)
