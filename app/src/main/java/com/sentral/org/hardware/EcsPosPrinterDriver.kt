package com.sentral.org.hardware

import android.bluetooth.BluetoothManager
import android.content.Context
import co.touchlab.kermit.Logger
import com.dantsu.escposprinter.EscPosCharsetEncoding
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.tcp.TcpConnection
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException
import com.dantsu.escposprinter.exceptions.EscPosConnectionException
import com.dantsu.escposprinter.exceptions.EscPosEncodingException
import com.dantsu.escposprinter.exceptions.EscPosParserException
import com.sentral.org.data.entity.PrinterEntity
import com.sentral.org.data.model.PrintResult
import com.sentral.org.data.model.PrinterConnectionType
import com.sentral.org.data.model.ReceiptData
import com.sentral.org.data.service.PrinterDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

/**
 * Implementasi PrinterDriver untuk Android menggunakan library DantSu ESC/POS v3.4.0.
 *
 * Kelas ini bertindak sebagai **Orchestrator** yang menyatukan:
 * - Manajemen koneksi (Bluetooth/WiFi)
 * - Pemrosesan logo (via [EscPosLogoProcessor])
 * - Formatting struk (via [EscPosReceiptFormatter])
 *
 * Fitur lengkap:
 * - Bluetooth + TCP (WiFi) connection
 * - Logo toko dengan auto-resize + safety guards (anti-OOM)
 * - Text formatting dengan markup DantSu
 * - QR Code untuk nomor transaksi
 * - Auto feed paper + cut paper
 * - Charset UTF-8 untuk Bahasa Indonesia
 *
 * Graceful degradation: logo gagal = cetak jalan terus tanpa logo.
 */
class EscPosPrinterDriver(
    private val context: Context,
    private val printerConfig: PrinterEntity,
) : PrinterDriver {
    override val name: String = "ESC/POS ${printerConfig.tipeKoneksi}"

    companion object {
        private const val TAG = "EscPosPrinterDriver"
        private val log = Logger.withTag(TAG)

        // ===== Printer constants =====
        private const val PRINTER_DPI = 203
        private const val PRINTER_WIDTH_MM = 80f
        private const val FEED_PAPER_MM = 10f
        private const val TCP_TIMEOUT_MS = 5000

        private val CHARSET_UTF8 = EscPosCharsetEncoding("UTF-8", 28)
    }

    /**
     * Pasangan printer + connection. Connection disimpan terpisah untuk
     * memungkinkan print logo via raw bytes (EscPosPrinter tidak expose API printImage).
     */
    private data class PrinterHandle(
        val printer: EscPosPrinter,
        val connection: DeviceConnection,
    )

    override suspend fun testConnection(): Boolean =
        withContext(Dispatchers.IO) {
            var handle: PrinterHandle? = null
            try {
                handle = buildPrinterHandle() ?: return@withContext false
                handle.printer.printFormattedText("[C]TEST CONNECTION\n")
                true
            } catch (e: EscPosConnectionException) {
                log.e(e) { "testConnection failed: ${e.message}" }
                false
            } catch (e: EscPosEncodingException) {
                log.e(e) { "testConnection failed: ${e.message}" }
                false
            } catch (e: EscPosParserException) {
                log.e(e) { "testConnection failed: ${e.message}" }
                false
            } finally {
                handle?.printer?.disconnectPrinter()
            }
        }

    override suspend fun print(receipt: ReceiptData): PrintResult =
        withContext(Dispatchers.IO) {
            // Timeout 30 detik untuk keseluruhan proses print
            val result =
                withTimeoutOrNull(30000L) {
                    printInternal(receipt)
                }

            result ?: PrintResult.Failure(
                message = "Print timeout (>30 detik)",
                isRetryable = true,
            )
        }

    private suspend fun printLogo(
        handle: PrinterHandle,
        logoUri: String?,
    ) {
        if (logoUri.isNullOrBlank()) return

        val logoBytes = EscPosLogoProcessor.loadAndConvertLogo(context, logoUri) ?: return

        try {
            handle.connection.write(logoBytes)
            handle.connection.write(byteArrayOf(0x0A))
            handle.connection.send()
        } catch (e: EscPosConnectionException) {
            log.e(e) {
                "Failed to send logo: ${e.message}"
            }
        }
    }

    private suspend fun printInternal(receipt: ReceiptData): PrintResult {
        var handle: PrinterHandle? = null

        try {
            log.i {
                "🖨️ print() called for receipt: ${receipt.transaksi.nomor}"
            }

            handle = buildPrinterHandle()
                ?: return PrintResult.Failure(
                    "Tidak dapat terhubung ke printer (koneksi tidak dikonfigurasi)",
                    isRetryable = true,
                )

            log.i {
                "🔌 Connected to printer: ${printerConfig.nama}"
            }

            // ===== TAHAP 1: Print logo =====
            printLogo(
                handle = handle,
                logoUri = receipt.toko.logoUri,
            )

            // ===== TAHAP 2: Print teks =====
            val formattedText =
                EscPosReceiptFormatter.buildFormattedReceipt(
                    receipt,
                    handle.printer.printerNbrCharactersPerLine,
                )

            handle.printer.printFormattedTextAndCut(
                formattedText,
                FEED_PAPER_MM,
            )

            return PrintResult.Success
        } catch (e: EscPosConnectionException) {
            return PrintResult.Failure(
                message = "Koneksi printer terputus: ${e.message}",
                isRetryable = true,
            )
        } catch (e: EscPosEncodingException) {
            return PrintResult.Failure(
                message = "Gagal encode teks struk: ${e.message}",
                isRetryable = false,
            )
        } catch (e: EscPosParserException) {
            return PrintResult.Failure(
                message = "Format struk tidak valid: ${e.message}",
                isRetryable = false,
            )
        } catch (e: EscPosBarcodeException) {
            return PrintResult.Failure(
                message = "Gagal render QR/barcode: ${e.message}",
                isRetryable = false,
            )
        } catch (e: Exception) {
            return PrintResult.Failure(
                message = e.message ?: "Error cetak tidak diketahui",
                isRetryable = isRetryableError(e),
            )
        } finally {
            log.i { "🔌 Disconnecting printer" }
            handle?.printer?.disconnectPrinter()
        }
    }

    override suspend fun disconnect() {
        // Koneksi dikelola per-job, tidak ada state persisten
    }

    // ---------- Factory Connection ----------

    /**
     * Bangun PrinterHandle (printer + connection). Constructor EscPosPrinter otomatis connect.
     * Return null jika konfigurasi koneksi tidak lengkap.
     */
    private fun buildPrinterHandle(): PrinterHandle? {
        val connectionType = safeConnectionType(printerConfig.tipeKoneksi) ?: return null
        val connection: DeviceConnection =
            when (connectionType) {
                PrinterConnectionType.BLUETOOTH -> {
                    val address = printerConfig.alamatBluetooth ?: return null
                    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return null
                    val adapter = bluetoothManager.adapter ?: return null
                    val device = adapter.getRemoteDevice(address)
                    BluetoothConnection(device)
                }

                PrinterConnectionType.WIFI -> {
                    val address = printerConfig.alamatWifi ?: return null
                    val port = printerConfig.portWifi ?: 9100
                    TcpConnection(address, port, TCP_TIMEOUT_MS)
                }

                PrinterConnectionType.USB -> {
                    // TODO: USB permission flow di Tahap 3 (Settings Screen)
                    return null
                }
            }
        val printer =
            EscPosPrinter(
                connection,
                PRINTER_DPI,
                PRINTER_WIDTH_MM,
                printerConfig.karakterPerBaris,
                CHARSET_UTF8,
            )
        return PrinterHandle(printer, connection)
    }

    private fun safeConnectionType(value: String): PrinterConnectionType? = PrinterConnectionType.entries.firstOrNull { it.name == value }

    private fun isRetryableError(e: Exception): Boolean =
        e is IOException ||
            e is java.net.SocketTimeoutException ||
            e is EscPosConnectionException
}
