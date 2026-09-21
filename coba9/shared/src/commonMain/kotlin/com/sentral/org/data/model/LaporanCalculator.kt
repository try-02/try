package com.sentral.org.data.model

import com.sentral.org.data.dao.PaymentAggregateRaw
import com.sentral.org.data.dao.SalesAggregateRaw
import com.sentral.org.data.dao.TopProductRaw

object LaporanCalculator {

    fun calculate(
        sales: SalesAggregateRaw,
        hppPenjualan: Long,
        totalRetur: Long,
        hppRetur: Long,
        payments: List<PaymentAggregateRaw>,
        topProducts: List<TopProductRaw>,
    ): LaporanPenjualan {
        val omzetPenjualan = sales.omzetPenjualan
        val omzetBersih = (omzetPenjualan - totalRetur).coerceAtLeast(0L)

        val hppBersih = (hppPenjualan - hppRetur).coerceAtLeast(0L)
        val labaKotor = omzetBersih - hppBersih

        val marginPersen = if (omzetBersih > 0L) {
            (labaKotor.toDouble() / omzetBersih.toDouble()) * 100.0
        } else {
            0.0
        }

        val rataRataTransaksi = if (sales.jumlahSelesai > 0) {
            omzetPenjualan / sales.jumlahSelesai
        } else {
            0L
        }

        val totalNominalBayar = payments.sumOf { it.totalNominal }
        val laporanBayar = payments.map { p ->
            val pct = if (totalNominalBayar > 0L) {
                (p.totalNominal.toDouble() / totalNominalBayar.toDouble()) * 100.0
            } else {
                0.0
            }
            LaporanPembayaran(
                metode = p.metode,
                nominal = p.totalNominal,
                persentase = pct,
            )
        }

        val mappedTopProduk = topProducts.map {
            TopProdukLaporan(
                produkId = it.produkId,
                nama = it.namaProduk,
                jumlahTerjualScaled = it.totalJumlahScaled,
                totalNominal = it.totalNominal,
            )
        }

        return LaporanPenjualan(
            omzetPenjualan = omzetPenjualan,
            totalRetur = totalRetur,
            omzetBersih = omzetBersih,
            hppHistoris = hppPenjualan,
            hppRetur = hppRetur,
            hppBersih = hppBersih,
            labaKotor = labaKotor,
            marginPersen = marginPersen,
            jumlahTransaksiSelesai = sales.jumlahSelesai,
            jumlahTransaksiVoid = sales.jumlahVoid,
            rataRataTransaksi = rataRataTransaksi,
            pembayaran = laporanBayar,
            topProduk = mappedTopProduk,
        )
    }
}