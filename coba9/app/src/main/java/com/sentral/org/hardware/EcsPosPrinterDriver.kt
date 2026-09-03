package com.sentral.org.hardware

import android.content.Context
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.tcp.TcpConnection
import com.sentral.org.data.entity.PrinterEntity
import com.sentral.org.data.model.MetodePembayaran
import com.sentral.org.data.model.PrintResult
import com.sentral.org.data.model.PrinterConnectionType
import com.sentral.org.data.model.ReceiptData
import com.sentral.org.data.service.PrinterDriver
import com.sentral.org.data.service.ReceiptFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementasi PrinterDriver untuk Android menggunakan library DantSu ESC/POS.
 *
 * CATATAN API DantSu 3.x:
 * - Method cetak utama: printFormattedText(String)
 * - Cleanup: disconnect() (bukan Closeable.use{})
 * - Semua operasi I/O di-dispatch ke Dispatchers.IO agar tidak memblokir main thread.
 */
class EscPosPrinterDriver(
    private val context: Context,
    private val printerConfig: PrinterEntity,
) : PrinterDriver {

    override val name: String = "ESC/POS ${printerConfig.tipeKoneksi}"

    override suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val printer = connect() ?: return@withContext false
            try {
                printer.printFormattedText("TEST\n")
                true
            } finally {
                printer.disconnect()
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun print(receipt: ReceiptData): PrintResult = withContext(Dispatchers.IO) {
        try {
            val printer = connect()
                ?: return@withContext PrintResult.Failure(
                    "Gagal koneksi ke printer",
                    isRetryable = true,
                )

            try {
                val charsPerLine = printerConfig.karakterPerBaris

                printHeader(printer, receipt, charsPerLine)
                printItems(printer, receipt, charsPerLine)
                printTotals(printer, receipt, charsPerLine)
                printPayments(printer, receipt, charsPerLine)
                printFooter(printer, receipt)

                // Feed beberapa baris agar struk mudah disobek
                printer.printFormattedText("\n\n\n")
            } finally {
                printer.disconnect()
            }

            PrintResult.Success
        } catch (e: Exception) {
            PrintResult.Failure(
                message = e.message ?: "Error cetak tidak diketahui",
                isRetryable = isRetryableError(e),
            )
        }
    }

    override suspend fun disconnect() {
        // Koneksi dikelola per-job (buka-cetak-tutup), jadi tidak ada 
        // koneksi persisten yang perlu ditutup di sini.
    }

    // ---------- Private helpers ----------

    private fun connect(): EscPosPrinter? {
        val connectionType = safeConnectionType(printerConfig.tipeKoneksi) ?: return null
        val dpi = 203
        val widthMM = 48f
        val charsPerLine = printerConfig.karakterPerBaris

        return when (connectionType) {
            PrinterConnectionType.BLUETOOTH -> {
                val address = printerConfig.alamatBluetooth ?: return null
                val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter() ?: return null
                val device = adapter.getRemoteDevice(address)
                val connection = BluetoothConnection(device)
                EscPosPrinter(connection, dpi, widthMM, charsPerLine)
            }
            PrinterConnectionType.WIFI -> {
                val address = printerConfig.alamatWifi ?: return null
                val port = printerConfig.portWifi ?: 9100
                val connection = TcpConnection(address, port)
                EscPosPrinter(connection, dpi, widthMM, charsPerLine)
            }
            PrinterConnectionType.USB -> {
                // USB butuh UsbManager + permission, lebih kompleks.
                // TODO: implement USB connection di tahap selanjutnya.
                null
            }
        }
    }

    private fun safeConnectionType(value: String): PrinterConnectionType? {
        return PrinterConnectionType.entries.firstOrNull { it.name == value }
    }

    private fun printHeader(printer: EscPosPrinter, receipt: ReceiptData, charsPerLine: Int) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val waktu = dateFormat.format(Date(receipt.transaksi.waktu))

        val header = buildString {
            append(receipt.toko.nama).append("\n")
            if (receipt.toko.alamat.isNotBlank()) {
                append(receipt.toko.alamat).append("\n")
            }
            append("-".repeat(charsPerLine)).append("\n")
            append("No: ").append(receipt.transaksi.nomor).append("\n")
            append("Kasir: ").append(receipt.transaksi.kasir).append("\n")
            append("Waktu: ").append(waktu).append("\n")
            append("-".repeat(charsPerLine)).append("\n")
        }
        printer.printFormattedText(header)
    }

    private fun printItems(printer: EscPosPrinter, receipt: ReceiptData, charsPerLine: Int) {
        val sb = StringBuilder()
        receipt.items.forEach { item ->
            val qty = item.jumlah / 1000  // QUANTITY_SCALE
            val lineTotal = ReceiptFormatter.formatMoney(item.totalBaris)
            val priceStr = ReceiptFormatter.formatMoney(item.hargaSatuan)

            sb.append(item.nama).append("\n")

            val detail = "  $qty x $priceStr"
            val padding = (charsPerLine - detail.length - lineTotal.length).coerceAtLeast(1)
            sb.append(detail)
            sb.append(" ".repeat(padding))
            sb.append(lineTotal).append("\n")
        }
        printer.printFormattedText(sb.toString())
    }

    private fun printTotals(printer: EscPosPrinter, receipt: ReceiptData, charsPerLine: Int) {
        val sb = StringBuilder()
        sb.append("-".repeat(charsPerLine)).append("\n")
        sb.append(alignedLine("Subtotal", ReceiptFormatter.formatMoney(receipt.transaksi.subtotal), charsPerLine))

        if (receipt.transaksi.diskon > 0) {
            sb.append(alignedLine("Diskon", "-${ReceiptFormatter.formatMoney(receipt.transaksi.diskon)}", charsPerLine))
        }
        if (receipt.transaksi.pajak > 0) {
            sb.append(alignedLine("Pajak", ReceiptFormatter.formatMoney(receipt.transaksi.pajak), charsPerLine))
        }

        sb.append("-".repeat(charsPerLine)).append("\n")
        sb.append(alignedLine("TOTAL", ReceiptFormatter.formatMoney(receipt.transaksi.total), charsPerLine))

        printer.printFormattedText(sb.toString())
    }

    private fun printPayments(printer: EscPosPrinter, receipt: ReceiptData, charsPerLine: Int) {
        val sb = StringBuilder()
        sb.append("\n")

        receipt.payments.forEach { payment ->
            val metode = when (payment.metode) {
                MetodePembayaran.CASH -> "TUNAI"
                MetodePembayaran.QRIS -> "QRIS"
            }
            sb.append(alignedLine(metode, ReceiptFormatter.formatMoney(payment.jumlah), charsPerLine))

            // Assign ke local val untuk hindari smart cast issue lintas module
            val diterima = payment.diterima
            if (diterima != null) {
                sb.append(alignedLine("Diterima", ReceiptFormatter.formatMoney(diterima), charsPerLine))
            }

            val kembalian = payment.kembalian ?: 0L
            if (kembalian > 0) {
                sb.append(alignedLine("Kembali", ReceiptFormatter.formatMoney(kembalian), charsPerLine))
            }
        }

        printer.printFormattedText(sb.toString())
    }

    private fun printFooter(printer: EscPosPrinter, receipt: ReceiptData) {
        printer.printFormattedText("\n${receipt.footer}\n")
    }

    /**
     * Buat baris rata kiri-kanan: "Label          Value"
     */
    private fun alignedLine(label: String, value: String, charsPerLine: Int): String {
        val padding = (charsPerLine - label.length - value.length).coerceAtLeast(1)
        return label + " ".repeat(padding) + value + "\n"
    }

    private fun isRetryableError(e: Exception): Boolean {
        // Error koneksi biasanya retryable, error format data tidak
        return e is java.io.IOException || e is java.net.SocketTimeoutException
    }
}