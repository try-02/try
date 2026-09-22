package com.sentral.org.hardware

import com.sentral.org.data.model.MetodePembayaran
import com.sentral.org.data.model.ReceiptData
import com.sentral.org.data.service.ReceiptFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class ReceiptBuilder {

    fun build(receipt: ReceiptData, charsPerLine: Int): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val waktu = dateFormat.format(Date(receipt.transaksi.waktu))

        sb.append(buildHeader(receipt.toko, charsPerLine))
        sb.append("[C]$waktu\n")
        sb.append("[L]\n")

        sb.append(buildTransactionInfo(receipt.transaksi, charsPerLine))
        sb.append(buildItems(receipt.items, charsPerLine))
        sb.append(buildTotals(receipt.transaksi))
        sb.append(buildPayments(receipt.payments))
        sb.append("[L]\n")

        if (receipt.toko.cetakQr) {
            sb.append(buildQrCode(receipt.transaksi.nomor))
        }

        sb.append(buildFooter(receipt.toko, receipt.footer))
        return sb.toString()
    }

    private fun buildHeader(toko: com.sentral.org.data.entity.ProfilTokoEntity, charsPerLine: Int): String {
        val sb = StringBuilder()
        sb.append("[C]<b><font size='big'>")
            .append(escape(toko.nama))
            .append("</font></b>\n")
        if (toko.alamat.isNotBlank()) {
            sb.append("[C]<font size='small'>")
                .append(escape(toko.alamat))
                .append("</font>\n")
        }
        return sb.toString()
    }

    private fun buildTransactionInfo(transaksi: com.sentral.org.data.entity.TransaksiEntity, charsPerLine: Int): String {
        return StringBuilder()
            .append("[L]No: <b>").append(transaksi.nomor).append("</b>\n")
            .append("[L]Kasir: ").append(escape(transaksi.kasir)).append("\n")
            .append("[L]<u>").append("-".repeat(charsPerLine)).append("</u>\n")
            .toString()
    }

    private fun buildItems(items: List<com.sentral.org.data.model.ReceiptItem>, charsPerLine: Int): String {
        val sb = StringBuilder()
        items.forEach { item ->
            val qtyStr = com.sentral.org.data.model.formatQuantity(item.jumlah)
            val priceStr = ReceiptFormatter.formatMoney(item.hargaSatuan)
            val lineTotal = ReceiptFormatter.formatMoney(item.totalBaris)

            sb.append("[L]<b>").append(escape(truncate(item.nama, charsPerLine))).append("</b>\n")
            sb.append("[L]")
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

    private fun buildTotals(transaksi: com.sentral.org.data.entity.TransaksiEntity): String {
        val sb = StringBuilder()
        sb.append(alignedLine("Subtotal", ReceiptFormatter.formatMoney(transaksi.subtotal)))

        if (transaksi.diskon > 0) {
            sb.append(alignedLine("Diskon", "-" + ReceiptFormatter.formatMoney(transaksi.diskon)))
        }
        if (transaksi.pajak > 0) {
            sb.append(alignedLine("Pajak", ReceiptFormatter.formatMoney(transaksi.pajak)))
        }

        sb.append("[L]<u>").append("-".repeat(32)).append("</u>\n")
        sb.append("[L]<b><font size='big'>TOTAL</font></b>")
            .append("[R]<b><font size='big'>")
            .append(ReceiptFormatter.formatMoney(transaksi.total))
            .append("</font></b>\n")
        sb.append("[L]\n")
        return sb.toString()
    }

    private fun buildPayments(payments: List<com.sentral.org.data.model.ReceiptPayment>): String {
        val sb = StringBuilder()
        payments.forEach { payment ->
            val metode = when (payment.metode) {
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
        return sb.toString()
    }

    private fun buildQrCode(qrData: String): String {
        return StringBuilder()
            .append("[C]<qrcode size='6'>")
            .append(escape(qrData))
            .append("</qrcode>\n")
            .toString()
    }

    private fun buildFooter(toko: com.sentral.org.data.entity.ProfilTokoEntity, footer: String): String {
        val sb = StringBuilder()
        sb.append("[C]<b>Terima kasih atas kunjungan Anda!</b>\n")
        if (footer.isNotBlank() && footer != "Terima kasih") {
            sb.append("[C]<font size='small'>").append(escape(footer)).append("</font>\n")
        }
        return sb.toString()
    }

    private fun escape(text: String): String =
        text.replace("[", "&#91;")
            .replace("]", "&#93;")
            .replace("<", "<")
            .replace(">", ">")

    private fun truncate(text: String, maxWidth: Int): String =
        if (text.length > maxWidth) text.take(maxWidth - 2) + ".." else text

    private fun alignedLine(label: String, value: String): String = "[L]$label[R]$value\n"

    private fun alignedLineBold(label: String, value: String): String = "[L]<b>$label</b>[R]<b>$value</b>\n"
}