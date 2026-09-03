package com.sentral.org.hardware

import android.content.Context
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.tcp.TcpConnection
import com.dantsu.escposprinter.connection.usb.UsbConnection
import com.dantsu.escposprinter.EscPosPrinter
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
 * CATATAN: Semua operasi I/O di-dispatch ke Dispatchers.IO agar tidak 
 * membloktor main thread.
 */
class EscPosPrinterDriver(
    private val context: Context,
    private val printer: PrinterEntity,
) : PrinterDriver {

    override val name: String = "ESC/POS ${printer.tipeKoneksi}"

    override suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val escPrinter = connect() ?: return@withContext false
            escPrinter.use { it.printText("TEST\n") }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun print(receipt: ReceiptData): PrintResult = withContext(Dispatchers.IO) {
        try {
            val escPrinter = connect() 
                ?: return@withContext PrintResult.Failure(
                    "Gagal koneksi ke printer", 
                    isRetryable = true
                )
            
            escPrinter.use { printer ->
                printHeader(printer, receipt)
                printItems(printer, receipt)
                printTotals(printer, receipt)
                printPayments(printer, receipt)
                printFooter(printer, receipt)
                
                // Feed 3 baris agar struk mudah disobek
                printer.feedPaper(3)
                printer.cutPaper()
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
        // DantSu library pakai use{} pattern, koneksi otomatis tertutup
    }

    // ---------- Private helpers ----------

    private fun connect(): EscPosPrinter? {
        return when (PrinterConnectionType.valueOf(printer.tipeKoneksi)) {
            PrinterConnectionType.BLUETOOTH -> {
                val address = printer.alamatBluetooth ?: return null
                val connection = BluetoothConnection(
                    android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                        .getRemoteDevice(address)
                )
                EscPosPrinter(connection, 203, 48f, printer.karakterPerBaris)
            }
            PrinterConnectionType.WIFI -> {
                val address = printer.alamatWifi ?: return null
                val port = printer.portWifi ?: 9100
                val connection = TcpConnection(address, port)
                EscPosPrinter(connection, 203, 48f, printer.karakterPerBaris)
            }
            PrinterConnectionType.USB -> {
                // USB butuh UsbManager dan permission, lebih kompleks
                // TODO: implement USB connection
                null
            }
        }
    }

    private fun printHeader(printer: EscPosPrinter, receipt: ReceiptData) {
        printer.apply {
            printText(receipt.toko.nama + "\n")
            printText(receipt.toko.alamat + "\n")
            printText("--------------------------------\n")
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val waktu = dateFormat.format(Date(receipt.transaksi.waktu))
            
            printText("No: ${receipt.transaksi.nomor}\n")
            printText("Kasir: ${receipt.transaksi.kasir}\n")
            printText("Waktu: $waktu\n")
            printText("--------------------------------\n")
        }
    }

    private fun printItems(printer: EscPosPrinter, receipt: ReceiptData) {
        receipt.items.forEach { item ->
            val qty = item.jumlah / 1000  // QUANTITY_SCALE
            val lineTotal = ReceiptFormatter.formatMoney(item.totalBaris)
            
            printer.apply {
                printText(item.nama + "\n")
                printText("  $qty x ${ReceiptFormatter.formatMoney(item.hargaSatuan)}")
                printText(" " * (printer.karakterPerBaris - 10 - lineTotal.length))
                printText(lineTotal + "\n")
            }
        }
    }

    private fun printTotals(printer: EscPosPrinter, receipt: ReceiptData) {
        printer.apply {
            printText("--------------------------------\n")
            printAlignedLine("Subtotal", ReceiptFormatter.formatMoney(receipt.transaksi.subtotal))
            
            if (receipt.transaksi.diskon > 0) {
                printAlignedLine("Diskon", "-${ReceiptFormatter.formatMoney(receipt.transaksi.diskon)}")
            }
            if (receipt.transaksi.pajak > 0) {
                printAlignedLine("Pajak", ReceiptFormatter.formatMoney(receipt.transaksi.pajak))
            }
            
            printText("--------------------------------\n")
            printAlignedLine("TOTAL", ReceiptFormatter.formatMoney(receipt.transaksi.total), bold = true)
        }
    }

    private fun printPayments(printer: EscPosPrinter, receipt: ReceiptData) {
        printer.apply {
            printText("\n")
            receipt.payments.forEach { payment ->
                val metode = when (payment.metode) {
                    MetodePembayaran.CASH -> "TUNAI"
                    MetodePembayaran.QRIS -> "QRIS"
                }
                printAlignedLine(metode, ReceiptFormatter.formatMoney(payment.jumlah))
                
                if (payment.diterima != null) {
                    printAlignedLine("Diterima", ReceiptFormatter.formatMoney(payment.diterima))
                }
                if (payment.kembalian != null && payment.kembalian > 0) {
                    printAlignedLine("Kembali", ReceiptFormatter.formatMoney(payment.kembalian))
                }
            }
        }
    }

    private fun printFooter(printer: EscPosPrinter, receipt: ReceiptData) {
        printer.apply {
            printText("\n")
            printText(receipt.footer + "\n")
        }
    }

    private fun EscPosPrinter.printAlignedLine(label: String, value: String, bold: Boolean = false) {
        val padding = karakterPerBaris - label.length - value.length
        val spaces = if (padding > 0) " ".repeat(padding) else " "
        if (bold) {
            printText(label + spaces + value + "\n")
        } else {
            printText(label + spaces + value + "\n")
        }
    }

    private fun isRetryableError(e: Exception): Boolean {
        // Error koneksi biasanya retryable, error format data tidak
        return e is java.io.IOException || e is java.net.SocketTimeoutException
    }
}