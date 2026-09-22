package com.sentral.org.data.service

import com.sentral.org.data.entity.ItemTransaksiEntity
import com.sentral.org.data.entity.PembayaranEntity
import com.sentral.org.data.entity.ProfilTokoEntity
import com.sentral.org.data.entity.TransaksiEntity
import com.sentral.org.data.model.CheckoutResult
import com.sentral.org.data.model.MetodePembayaran
import com.sentral.org.data.model.MoneyMath
import com.sentral.org.data.model.PaymentInfo
import com.sentral.org.data.model.ReceiptData
import com.sentral.org.data.model.ReceiptItem
import com.sentral.org.data.model.StoreInfo
import com.sentral.org.data.model.TransactionInfo

/**
 * Mengubah data transaksi dari DB menjadi ReceiptData yang siap dicetak.
 *
 * TERPISAH dari driver agar format bisa di-test tanpa hardware.
 */
object ReceiptFormatter {
    fun format(
        toko: ProfilTokoEntity?,
        transaksi: TransaksiEntity,
        items: List<ItemTransaksiEntity>,
        payments: List<PembayaranEntity>,
        isReprint: Boolean = false,
    ): ReceiptData {
        val baseFooter = toko?.catatanFooter ?: "Terima kasih"
        val finalFooter =
            if (isReprint) {
                if (baseFooter.isNotBlank()) {
                    "$baseFooter\n[C]<b>*** SALINAN / REPRINT ***</b>"
                } else {
                    "[C]<b>*** SALINAN / REPRINT ***</b>"
                }
            } else {
                baseFooter
            }

        return ReceiptData(
            toko =
                StoreInfo(
                    nama = toko?.namaToko ?: "Toko",
                    alamat = toko?.alamat ?: "",
                    footer = finalFooter,
                    logoUri = toko?.logoUri,
                    cetakQr = false,
                ),
            transaksi =
                TransactionInfo(
                    nomor = transaksi.nomorTransaksi,
                    kasir = transaksi.namaKasir,
                    waktu = transaksi.dibuatPada,
                    subtotal = transaksi.subtotal,
                    diskon = transaksi.diskon,
                    pajak = transaksi.pajak,
                    total = transaksi.total,
                ),
            items =
                items.map { item ->
                    ReceiptItem(
                        nama = item.namaProduk,
                        jumlah = item.jumlah,
                        hargaSatuan = item.hargaSatuan,
                        totalBaris = item.totalBaris,
                    )
                },
            payments =
                payments.map { p ->
                    PaymentInfo(
                        metode = p.metode,
                        jumlah = p.jumlah,
                        diterima = p.diterima,
                        kembalian = p.kembalian,
                    )
                },
            footer = finalFooter,
        )
    }

    /**
     * Format uang untuk struk. Dipisahkan dari formatRupiah di UI agar
     * format struk bisa berbeda (misal tanpa "Rp" prefix untuk hemat kertas).
     */
    fun formatMoney(value: Long): String {
        // Format sederhana tanpa library, cocok untuk struk thermal
        return value.toString()
    }

    fun formatShiftReport(
        toko: ProfilTokoEntity?,
        summary: com.sentral.org.data.model.ShiftSummary,
    ): ReceiptData {
        val jenisLaporan = if (summary.isZReport) "LAPORAN PENUTUPAN (Z REPORT)" else "LAPORAN SEMENTARA (X REPORT)"
        val waktuLaporan = summary.ditutupPada ?: summary.dimulaiPada

        val items =
            mutableListOf(
                ReceiptItem("Modal Awal Kas", 1000, summary.kasAwal, summary.kasAwal),
                ReceiptItem("Penjualan Tunai", 1000, summary.totalPenjualanTunai, summary.totalPenjualanTunai),
                ReceiptItem("Penjualan QRIS", 1000, summary.totalPenjualanNonTunai, summary.totalPenjualanNonTunai),
            )
        if (summary.totalReturTunai > 0) {
            items.add(ReceiptItem("Retur / Refund Kas", 1000, -summary.totalReturTunai, -summary.totalReturTunai))
        }

        val payments =
            if (summary.isZReport && summary.kasAktual != null) {
                listOf(
                    PaymentInfo(
                        metode = MetodePembayaran.CASH,
                        jumlah = summary.kasDiharapkan,
                        diterima = summary.kasAktual,
                        kembalian = summary.selisihKas,
                    ),
                )
            } else {
                emptyList()
            }

        val footerText =
            if (summary.isZReport) {
                "*** SHIFT RESMI DITUTUP ***\nSimpan struk ini untuk rekonsiliasi kas."
            } else {
                "*** BACAAN SEMENTARA (SHIFT MASIH AKTIF) ***"
            }

        return ReceiptData(
            toko =
                StoreInfo(
                    nama = toko?.namaToko ?: "Toko",
                    alamat = toko?.alamat ?: "",
                    footer = footerText,
                    logoUri = toko?.logoUri,
                    cetakQr = false,
                ),
            transaksi =
                TransactionInfo(
                    nomor = if (summary.isZReport) "Z-SHIFT-${summary.shiftId}" else "X-SHIFT-${summary.shiftId}",
                    kasir = summary.kasirNama,
                    waktu = waktuLaporan,
                    subtotal = summary.totalPenjualanTunai + summary.totalPenjualanNonTunai,
                    diskon = 0L,
                    pajak = 0L,
                    total = summary.kasDiharapkan,
                ),
            items = items,
            payments = payments,
            footer = footerText,
        )
    }

    fun formatVoid(
        toko: ProfilTokoEntity?,
        transaksi: TransaksiEntity,
        items: List<ItemTransaksiEntity>,
        kasirPelaksana: String,
        alasan: String,
        waktuVoid: Long,
    ): ReceiptData {
        val footerText =
            "*** TRANSAKSI DIBATALKAN (VOID) ***\n" +
                "Alasan: $alasan\n" +
                "Kasir Void: $kasirPelaksana\n" +
                "Semua stok telah dikembalikan ke sistem."

        return ReceiptData(
            toko =
                StoreInfo(
                    nama = toko?.namaToko ?: "Toko",
                    alamat = toko?.alamat ?: "",
                    footer = footerText,
                    logoUri = toko?.logoUri,
                    cetakQr = false,
                ),
            transaksi =
                TransactionInfo(
                    nomor = "VOID-${transaksi.nomorTransaksi}",
                    kasir = transaksi.namaKasir,
                    waktu = waktuVoid,
                    subtotal = transaksi.subtotal,
                    diskon = transaksi.diskon,
                    pajak = transaksi.pajak,
                    total = transaksi.total,
                ),
            items =
                items.map { item ->
                    ReceiptItem(
                        nama = "[VOID] ${item.namaProduk}",
                        jumlah = item.jumlah,
                        hargaSatuan = item.hargaSatuan,
                        totalBaris = item.totalBaris,
                    )
                },
            payments =
                listOf(
                    PaymentInfo(
                        metode = MetodePembayaran.CASH,
                        jumlah = transaksi.total,
                        diterima = null,
                        kembalian = null,
                    ),
                ),
            footer = footerText,
        )
    }

    fun formatReturn(
        toko: ProfilTokoEntity?,
        transaksi: TransaksiEntity,
        itemsRetur: List<Pair<ItemTransaksiEntity, Long>>, // item to returned scaled qty
        totalRefund: Long,
        metodeRefund: MetodePembayaran,
        kasirPelaksana: String,
        waktuRetur: Long,
    ): ReceiptData {
        val footerText =
            "*** STRUK PENGEMBALIAN BARANG (RETUR) ***\n" +
                "Ref Transaksi: ${transaksi.nomorTransaksi}\n" +
                "Kasir Retur: $kasirPelaksana\n" +
                "Barang yang diretur telah disesuaikan di gudang."

        return ReceiptData(
            toko =
                StoreInfo(
                    nama = toko?.namaToko ?: "Toko",
                    alamat = toko?.alamat ?: "",
                    footer = footerText,
                    logoUri = toko?.logoUri,
                    cetakQr = false,
                ),
            transaksi =
                TransactionInfo(
                    nomor = "RETUR-${transaksi.nomorTransaksi}",
                    kasir = kasirPelaksana,
                    waktu = waktuRetur,
                    subtotal = totalRefund,
                    diskon = 0L,
                    pajak = 0L,
                    total = totalRefund,
                ),
            items =
                itemsRetur.map { (item, qty) ->
                    ReceiptItem(
                        nama = "[RETUR] ${item.namaProduk}",
                        jumlah = qty,
                        hargaSatuan = item.hargaSatuan,
                        totalBaris = MoneyMath.lineTotal(item.hargaSatuan, qty),
                    )
                },
            payments =
                listOf(
                    PaymentInfo(
                        metode = metodeRefund,
                        jumlah = totalRefund,
                        diterima = null,
                        kembalian = null,
                    ),
                ),
            footer = footerText,
        )
    }
}
