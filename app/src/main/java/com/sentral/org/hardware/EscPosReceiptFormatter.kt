package com.sentral.org.hardware

import com.sentral.org.data.model.MetodePembayaran
import com.sentral.org.data.model.ReceiptData
import com.sentral.org.data.model.formatQuantity
import com.sentral.org.data.service.ReceiptFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formatter khusus untuk membangun string markup DantSu ESC/POS dari [ReceiptData].
 *
 * Dirancang sebagai stateless `object` agar logika formatting menjadi *pure function*,
 * mudah diuji (unit test string output), dan tidak bergantung pada `Context` Android.
 *
 * Markup DantSu yang didukung:
 * - `[L]`, `[C]`, `[R]`: Alignment Left, Center, Right
 * - `<b>...</b>`: Bold
 * - `<font size='big'>...</font>`: Font besar
 * - `<u>...</u>`: Underline (biasanya untuk garis pemisah)
 * - `<qrcode size='6'>...</qrcode>`: QR Code
 */
object EscPosReceiptFormatter {
    private const val DATE_FORMAT_PATTERN = "dd/MM/yyyy HH:mm"

    /**
     * Membangun string markup ESC/POS lengkap dari data struk.
     *
     * @param receipt Data struk yang akan diformat.
     * @param charsPerLine Jumlah karakter per baris (tergantung lebar kertas printer).
     * @return String markup siap cetak.
     */
    fun buildFormattedReceipt(
        receipt: ReceiptData,
        charsPerLine: Int,
    ): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.getDefault())
        val waktu = dateFormat.format(Date(receipt.transaksi.waktu))

        sb.append(buildHeader(receipt.toko.nama, receipt.toko.alamat, waktu))
        sb.append(buildTransactionInfo(receipt.transaksi.nomor, receipt.transaksi.kasir, charsPerLine))
        sb.append(buildItemsSection(receipt.items, charsPerLine))
        sb.append(buildTotalsSection(receipt.transaksi.subtotal, receipt.transaksi.diskon, receipt.transaksi.pajak, receipt.transaksi.total, charsPerLine))
        sb.append(buildPaymentsSection(receipt.payments))
        sb.append(buildQrCodeSection(receipt.toko.cetakQr, receipt.transaksi.nomor))
        sb.append(buildFooter(receipt.footer))

        return sb.toString()
    }

    private fun buildHeader(
        namaToko: String,
        alamatToko: String,
        waktu: String,
    ): String {
        val sb = StringBuilder()
        sb
            .append("[C]<b><font size='big'>")
            .append(escapeDantSuText(namaToko))
            .append("</font></b>\n")

        if (alamatToko.isNotBlank()) {
            sb
                .append("[C]<font size='small'>")
                .append(escapeDantSuText(alamatToko))
                .append("</font>\n")
        }

        sb.append("[C]").append(waktu).append("\n")
        sb.append("[L]\n")
        return sb.toString()
    }

    private fun buildTransactionInfo(
        nomorTransaksi: String,
        namaKasir: String,
        charsPerLine: Int,
    ): String {
        val sb = StringBuilder()
        sb.append("[L]No: <b>").append(nomorTransaksi).append("</b>\n")
        sb.append("[L]Kasir: ").append(escapeDantSuText(namaKasir)).append("\n")
        sb.append("[L]<u>").append("-".repeat(charsPerLine)).append("</u>\n")
        return sb.toString()
    }

    private fun buildItemsSection(
        items: List<com.sentral.org.data.entity.ItemTransaksiEntity>,
        charsPerLine: Int,
    ): String {
        val sb = StringBuilder()
        items.forEach { item ->
            val qtyStr = formatQuantity(item.jumlah)
            val priceStr = ReceiptFormatter.formatMoney(item.hargaSatuan)
            val lineTotal = ReceiptFormatter.formatMoney(item.totalBaris)

            sb.append("[L]<b>").append(escapeDantSuText(truncate(item.nama, charsPerLine))).append("</b>\n")
            sb
                .append("[L]")
                .append(qtyStr)
                .append(" x ")
                .append(priceStr)
                .append("[R]")
                .append(lineTotal)
                .append("\n")
        }
        sb.append("[L]<u>").append("-".repeat(charsPerLine)).append("</u>\n")
        return sb.toString()
    }

    private fun buildTotalsSection(
        subtotal: Long,
        diskon: Long,
        pajak: Long,
        total: Long,
        charsPerLine: Int,
    ): String {
        val sb = StringBuilder()
        sb.append(alignedLine("Subtotal", ReceiptFormatter.formatMoney(subtotal)))

        if (diskon > 0) {
            sb.append(alignedLine("Diskon", "-" + ReceiptFormatter.formatMoney(diskon)))
        }
        if (pajak > 0) {
            sb.append(alignedLine("Pajak", ReceiptFormatter.formatMoney(pajak)))
        }

        sb.append("[L]<u>").append("-".repeat(charsPerLine)).append("</u>\n")
        sb
            .append("[L]<b><font size='big'>TOTAL</font></b>")
            .append("[R]<b><font size='big'>")
            .append(ReceiptFormatter.formatMoney(total))
            .append("</font></b>\n")
        sb.append("[L]\n")
        return sb.toString()
    }

    private fun buildPaymentsSection(payments: List<com.sentral.org.data.entity.PembayaranEntity>): String {
        val sb = StringBuilder()
        payments.forEach { payment ->
            val metode =
                when (payment.metode) {
                    MetodePembayaran.CASH -> "TUNAI"
                    MetodePembayaran.QRIS -> "QRIS"
                }
            sb.append(alignedLineBold(metode, ReceiptFormatter.formatMoney(payment.jumlah)))

            val diterima = payment.diterima
            if (diterima != null) {
                sb.append(alignedLine("Diterima", ReceiptFormatter.formatMoney(diterima)))
            }
            val kembalian = payment.kembalian ?: 0L
            if (kembalian > 0) {
                sb.append(alignedLineBold("Kembali", ReceiptFormatter.formatMoney(kembalian)))
            }
        }
        sb.append("[L]\n")
        return sb.toString()
    }

    private fun buildQrCodeSection(
        cetakQr: Boolean,
        qrData: String,
    ): String {
        if (!cetakQr) return ""
        val sb = StringBuilder()
        sb
            .append("[C]<qrcode size='6'>")
            .append(escapeDantSuText(qrData))
            .append("</qrcode>\n")
        return sb.toString()
    }

    private fun buildFooter(footerText: String): String {
        val sb = StringBuilder()
        sb.append("[C]<b>Terima kasih atas kunjungan Anda!</b>\n")
        if (footerText.isNotBlank() && footerText != "Terima kasih") {
            sb.append("[C]<font size='small'>").append(escapeDantSuText(footerText)).append("</font>\n")
        }
        return sb.toString()
    }

    /**
     * Meng-escape karakter khusus yang memiliki makna dalam markup DantSu.
     */
    private fun escapeDantSuText(text: String): String =
        text
            .replace("[", "&#91;")
            .replace("]", "&#93;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

    /**
     * Memotong teks jika melebihi lebar maksimum, menambahkan ".." di akhir.
     */
    private fun truncate(
        text: String,
        maxWidth: Int,
    ): String = if (text.length > maxWidth) text.take(maxWidth - 2) + ".." else text

    /**
     * Membentuk baris dengan label di kiri dan value di kanan.
     */
    private fun alignedLine(
        label: String,
        value: String,
    ): String = "[L]$label[R]$value\n"

    /**
     * Membentuk baris bold dengan label di kiri dan value di kanan.
     */
    private fun alignedLineBold(
        label: String,
        value: String,
    ): String = "[L]<b>$label</b>[R]<b>$value</b>\n"
}
