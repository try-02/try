package com.sentral.org.data.service

import com.sentral.org.data.entity.ItemTransaksiEntity
import com.sentral.org.data.entity.PembayaranEntity
import com.sentral.org.data.entity.ProfilTokoEntity
import com.sentral.org.data.entity.TransaksiEntity
import com.sentral.org.data.model.CheckoutResult
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
    ): ReceiptData {
        return ReceiptData(
            toko = StoreInfo(
                nama = toko?.namaToko ?: "Toko",
                alamat = toko?.alamat ?: "",
                footer = toko?.catatanFooter ?: "Terima kasih",
            ),
            transaksi = TransactionInfo(
                nomor = transaksi.nomorTransaksi,
                kasir = transaksi.namaKasir,
                waktu = transaksi.dibuatPada,
                subtotal = transaksi.subtotal,
                diskon = transaksi.diskon,
                pajak = transaksi.pajak,
                total = transaksi.total,
            ),
            items = items.map { item ->
                ReceiptItem(
                    nama = item.namaProduk,
                    jumlah = item.jumlah,
                    hargaSatuan = item.hargaSatuan,
                    totalBaris = item.totalBaris,
                )
            },
            payments = payments.map { p ->
                PaymentInfo(
                    metode = p.metode,
                    jumlah = p.jumlah,
                    diterima = p.diterima,
                    kembalian = p.kembalian,
                )
            },
            footer = toko?.catatanFooter ?: "Terima kasih",
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
}