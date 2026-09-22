package com.sentral.org.hardware

import android.content.Context
import com.dantsu.escposprinter.EscPosCharsetEncoding
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.EscPosPrinterCommands
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException
import com.dantsu.escposprinter.exceptions.EscPosConnectionException
import com.dantsu.escposprinter.exceptions.EscPosEncodingException
import com.dantsu.escposprinter.exceptions.EscPosParserException
import com.sentral.org.data.entity.PrinterEntity
import com.sentral.org.data.model.PrintResult
import com.sentral.org.data.model.ReceiptData
import com.sentral.org.data.service.PrinterDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Implementasi PrinterDriver untuk Android menggunakan library DantSu ESC/POS v3.4.0.
 */
class EscPosPrinterDriver(
    private val context: Context,
    private val printerConfig: PrinterEntity,
) : PrinterDriver {

    override val name: String = "ESC/POS ${printerConfig.tipeKoneksi}"

    companion object {
        private const val TAG = "EscPosPrinterDriver"
        private val log = Logger.withTag(TAG)

        private const val PRINTER_DPI = 203
        private const val PRINTER_WIDTH_MM = 80f
        private const val FEED_PAPER_MM = 10f

        private val CHARSET_UTF8 = EscPosCharsetEncoding("UTF-8", 28)
    }

    private data class PrinterHandle(
        val printer: EscPosPrinter,
        val connection: DeviceConnection,
    )

    private val logoLoader = LogoLoader(context, log)
    private val receiptBuilder = ReceiptBuilder()
    private val connectionFactory = ConnectionFactory(context)

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
            val result = withTimeoutOrNull(30000L) {
                printInternal(receipt)
            }
            result ?: PrintResult.Failure(
                message = "Print timeout (>30 detik)",
                isRetryable = true,
            )
        }

    private suspend fun printInternal(receipt: ReceiptData): PrintResult {
        var handle: PrinterHandle? = null

        try {
            log.i { "🖨️ print() called for receipt: ${receipt.transaksi.nomor}" }

            handle = buildPrinterHandle()
                ?: return PrintResult.Failure(
                    "Tidak dapat terhubung ke printer (koneksi tidak dikonfigurasi)",
                    isRetryable = true,
                )

            log.i { "🔌 Connected to printer: ${printerConfig.nama}" }

            printLogo(handle, receipt.toko.logoUri)

            val formattedText = receiptBuilder.build(receipt, handle.printer.printerNbrCharactersPerLine)
            handle.printer.printFormattedTextAndCut(formattedText, FEED_PAPER_MM)

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

    private suspend fun printLogo(handle: PrinterHandle, logoUri: String?) {
        if (logoUri.isNullOrBlank()) return

        val logoBytes = logoLoader.loadAndConvertLogo(logoUri) ?: return

        try {
            handle.connection.write(logoBytes)
            handle.connection.write(byteArrayOf(0x0A))
            handle.connection.send()
        } catch (e: EscPosConnectionException) {
            log.e(e) { "Failed to send logo: ${e.message}" }
        }
    }

    private fun buildPrinterHandle(): PrinterHandle? {
        val connection = connectionFactory.buildConnection(printerConfig) ?: return null
        val printer = EscPosPrinter(
            connection,
            PRINTER_DPI,
            PRINTER_WIDTH_MM,
            printerConfig.karakterPerBaris,
            CHARSET_UTF8,
        )
        return PrinterHandle(printer, connection)
    }

    override suspend fun disconnect() {
        // Koneksi dikelola per-job, tidak ada state persisten
    }

    private fun isRetryableError(e: Exception): Boolean =
        e is IOException ||
            e is java.net.SocketTimeoutException ||
            e is EscPosConnectionException
}