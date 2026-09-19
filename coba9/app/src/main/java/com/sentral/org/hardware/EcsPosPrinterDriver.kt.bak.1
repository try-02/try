package com.sentral.org.hardware

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.dantsu.escposprinter.EscPosCharsetEncoding
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.EscPosPrinterCommands
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.tcp.TcpConnection
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException
import com.dantsu.escposprinter.exceptions.EscPosConnectionException
import com.dantsu.escposprinter.exceptions.EscPosEncodingException
import com.dantsu.escposprinter.exceptions.EscPosParserException
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
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Implementasi PrinterDriver untuk Android menggunakan library DantSu ESC/POS v3.4.0.
 *
 * FITUR LENGKAP:
 * - Bluetooth + TCP (WiFi) connection
 * - Logo toko dengan auto-resize + safety guards (anti-OOM)
 * - Text formatting dengan markup DantSu
 * - QR Code untuk nomor transaksi
 * - Auto feed paper + cut paper
 * - Charset UTF-8 untuk Bahasa Indonesia
 *
 * KEAMANAN MEMORY (Logo):
 * - Cek ukuran file sebelum decode (>10MB = skip)
 * - Cek dimensi sebelum decode (>4096px = skip)
 * - Bitmap selalu di-recycle setelah convert ke bytes
 * - InputStream auto-close via use{} block
 * - Timeout 5 detik untuk load logo
 * - Graceful degradation: logo gagal = cetak jalan terus tanpa logo
 */
class EscPosPrinterDriver(
    private val context: Context,
    private val printerConfig: PrinterEntity,
) : PrinterDriver {

    override val name: String = "ESC/POS ${printerConfig.tipeKoneksi}"

    companion object {
        private const val TAG = "EscPosPrinterDriver"

        // ===== Printer constants =====
        private const val PRINTER_DPI = 203
        private const val PRINTER_WIDTH_MM = 80f
        private const val FEED_PAPER_MM = 10f
        private const val TCP_TIMEOUT_MS = 5000

        // ===== Logo safety constants =====
        // Target width di pixels: 48mm × 203 DPI / 25.4 ≈ 384px
        private const val TARGET_LOGO_WIDTH = 384
        private const val TARGET_LOGO_HEIGHT = 128  // Max height DantSu support
        // Source gambar max: 4096×4096 (16MP). Di atas ini, skip.
        private const val MAX_SOURCE_DIMENSION = 4096
        // File size max: 10MB. Di atas ini, skip.
        private const val MAX_LOGO_FILE_SIZE_BYTES = 10L * 1024 * 1024
        // Timeout load logo: 5 detik
        private const val LOGO_LOAD_TIMEOUT_MS = 3000L

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

    override suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        var handle: PrinterHandle? = null
        try {
            handle = buildPrinterHandle() ?: return@withContext false
            handle.printer.printFormattedText("[C]TEST CONNECTION\n")
            true
        } catch (e: Exception) {
            Log.e(TAG, "testConnection failed: ${e.message}") // ini log w loh
            false
        } finally {
            handle?.printer?.disconnectPrinter()
        }
    }
    override suspend fun print(receipt: ReceiptData): PrintResult = withContext(Dispatchers.IO) {
        // Timeout 30 detik untuk keseluruhan proses print
        val result = withTimeoutOrNull(30000L) {
            printInternal(receipt)
        }
        
        result ?: PrintResult.Failure(
            message = "Print timeout (>30 detik)",
            isRetryable = true,
        )
    }
    
    /**
     * Internal print logic (dipisah untuk timeout wrapper).
     */
    private suspend fun printInternal(receipt: ReceiptData): PrintResult {
        var handle: PrinterHandle? = null
        try {
            Log.e(TAG, "🖨️ print() called for receipt: ${receipt.transaksi.nomor}")
            
            handle = buildPrinterHandle()
                ?: return PrintResult.Failure(
                    "Tidak dapat terhubung ke printer (koneksi tidak dikonfigurasi)",
                    isRetryable = true,
                )
            
            Log.e(TAG, "🔌 Connected to printer: ${printerConfig.nama}")

            // ===== TAHAP 1: Print logo (kalau ada) via raw bytes ke connection =====
            // Dilakukan SEBELUM printFormattedText agar logo muncul di paling atas struk.
            // EscPosPrinter.printFormattedText akan call reset() yang reset state printer,
            // tapi tidak menghapus bytes yang sudah tercetak di kertas → aman.
            val logoUri = receipt.toko.logoUri
            if (!logoUri.isNullOrBlank()) {
                val logoBytes = loadAndConvertLogo(logoUri)
                if (logoBytes != null) {
                    try {
                        // Kirim logo + 1 line break dalam satu batch untuk kurangi overhead Bluetooth
                        handle.connection.write(logoBytes)
                        handle.connection.write(byteArrayOf(0x0A))
                        handle.connection.send()
                    } catch (e: EscPosConnectionException) {
                        Log.e(TAG, "Failed to send logo: ${e.message}")
                    }
                }
            }

            // ===== TAHAP 2: Print teks struk (auto feed + cut di akhir) =====
            val formattedText = buildFormattedReceipt(receipt, handle.printer.printerNbrCharactersPerLine)
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
            Log.e(TAG, "🔌 Disconnecting printer")
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
        val connection: DeviceConnection = when (connectionType) {
            PrinterConnectionType.BLUETOOTH -> {
                val address = printerConfig.alamatBluetooth ?: return null
                val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter() ?: return null
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
        val printer = EscPosPrinter(
            connection,
            PRINTER_DPI,
            PRINTER_WIDTH_MM,
            printerConfig.karakterPerBaris,
            CHARSET_UTF8,
        )
        return PrinterHandle(printer, connection)
    }

    private fun safeConnectionType(value: String): PrinterConnectionType? {
        return PrinterConnectionType.entries.firstOrNull { it.name == value }
    }

    // ---------- Logo Loading (Memory-Safe) ----------

    /**
     * Load logo dari URI, resize, convert ke ESC/POS bytes, recycle bitmap.
     * Return null jika gagal di tahap manapun (file besar, corrupt, timeout, dll).
     *
     * SAFETY GUARDS:
     * 1. Timeout 5 detik untuk keseluruhan proses
     * 2. Cek file size sebelum decode (>10MB = skip)
     * 3. Cek dimensi sebelum decode (>4096px = skip)
     * 4. Bitmap di-recycle setelah dipakai
     * 5. InputStream auto-close via use{}
     */
    private suspend fun loadAndConvertLogo(uriString: String): ByteArray? {
        return withTimeoutOrNull(LOGO_LOAD_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(uriString)

                    // ===== GUARD 1: Cek file size (jika tersedia) =====
                    val fileSize = getFileSize(uri)
                    if (fileSize != null && fileSize > MAX_LOGO_FILE_SIZE_BYTES) {
                        Log.e(TAG, "Logo file too large: $fileSize bytes (max: $MAX_LOGO_FILE_SIZE_BYTES)") // ini log w loh
                        return@withContext null
                    }

                    // ===== GUARD 2: Decode bounds saja untuk cek dimensi tanpa load pixel =====
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, bounds)
                    } ?: return@withContext null

                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                        Log.e(TAG, "Logo dimensions invalid: ${bounds.outWidth}x${bounds.outHeight}") // ini log w loh
                        return@withContext null
                    }

                    if (bounds.outWidth > MAX_SOURCE_DIMENSION || bounds.outHeight > MAX_SOURCE_DIMENSION) {
                        Log.e(TAG, "Logo too large: ${bounds.outWidth}x${bounds.outHeight} (max: $MAX_SOURCE_DIMENSION)") // ini log w loh
                        return@withContext null
                    }

                    // ===== GUARD 3: Hitung sample size untuk hemat memory saat decode =====
                    val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, TARGET_LOGO_WIDTH)
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }

                    val original: Bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, decodeOptions)
                    } ?: return@withContext null

                    // ===== GUARD 4: Resize ke target (aspect ratio preserved) =====
                    val aspectRatio = original.height.toFloat() / original.width.toFloat()
                    val targetHeight = (TARGET_LOGO_WIDTH * aspectRatio)
                        .roundToInt()
                        .coerceIn(1, TARGET_LOGO_HEIGHT)

                    val resized = Bitmap.createScaledBitmap(
                        original,
                        TARGET_LOGO_WIDTH,
                        targetHeight,
                        true,  // filter = true untuk quality lebih baik
                    )

                    // ===== GUARD 5: Recycle original kalau resized adalah bitmap baru =====
                    if (resized !== original) {
                        original.recycle()
                    }

                    // ===== Convert ke ESC/POS bytes (monochrome, gradient=false) =====
                    val bytes = EscPosPrinterCommands.bitmapToBytes(resized, false)

                    // ===== GUARD 6: Recycle resized setelah convert =====
                    resized.recycle()

                    bytes
                } catch (e: OutOfMemoryError) {
                    Log.e(TAG, "OOM loading logo: ${e.message}")
                    null
                } catch (e: SecurityException) {
                    Log.e(TAG, "No permission to read logo: ${e.message}") // ini log w loh
                    null
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load logo: ${e.message}") // ini log w loh
                    null
                }
            }
        }
    }

    /**
     * Hitung ukuran file dari URI. Return null jika tidak bisa ditentukan (misal content:// tanpa size).
     */
    private fun getFileSize(uri: Uri): Long? {
        return try {
            when (uri.scheme) {
                "file" -> uri.path?.let { java.io.File(it).length().takeIf { it > 0 } }
                "content" -> {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                            if (sizeIndex >= 0) cursor.getLong(sizeIndex) else null
                        } else null
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cannot get file size for $uri: ${e.message}") // ini log w loh
            null
        }
    }

    /**
     * Hitung sample size untuk BitmapFactory.decodeStream.
     * Sample size = 2^n yang membuat dimensi hasil >= target.
     */
    private fun calculateSampleSize(width: Int, height: Int, targetWidth: Int): Int {
        var sampleSize = 1
        if (width > targetWidth) {
            val halfWidth = width / 2
            while ((halfWidth / sampleSize) >= targetWidth) {
                sampleSize *= 2
            }
        }
        return sampleSize.coerceAtLeast(1)
    }

    // ---------- Receipt Builder ----------

    private fun buildFormattedReceipt(receipt: ReceiptData, charsPerLine: Int): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val waktu = dateFormat.format(Date(receipt.transaksi.waktu))

        // ===== HEADER TOKO (CENTER, BOLD, BIG) =====
        sb.append("[C]<b><font size='big'>")
            .append(escapeDantSuText(receipt.toko.nama))
            .append("</font></b>\n")

        if (receipt.toko.alamat.isNotBlank()) {
            sb.append("[C]<font size='small'>")
                .append(escapeDantSuText(receipt.toko.alamat))
                .append("</font>\n")
        }

        sb.append("[C]$waktu\n")
        sb.append("[L]\n")

        // ===== INFO TRANSAKSI =====
        sb.append("[L]No: <b>").append(receipt.transaksi.nomor).append("</b>\n")
        sb.append("[L]Kasir: ").append(escapeDantSuText(receipt.transaksi.kasir)).append("\n")
        sb.append("[L]<u>").append("-".repeat(charsPerLine)).append("</u>\n")

        // ===== ITEMS =====
        receipt.items.forEach { item ->
            val qty = item.jumlah / 1000
            val priceStr = ReceiptFormatter.formatMoney(item.hargaSatuan)
            val lineTotal = ReceiptFormatter.formatMoney(item.totalBaris)

            sb.append("[L]<b>").append(escapeDantSuText(truncate(item.nama, charsPerLine))).append("</b>\n")
            sb.append("[L]").append(qty).append(" x ").append(priceStr)
                .append("[R]").append(lineTotal).append("\n")
        }

        sb.append("[L]<u>").append("-".repeat(charsPerLine)).append("</u>\n")

        // ===== TOTALS =====
        sb.append(alignedLine("Subtotal", ReceiptFormatter.formatMoney(receipt.transaksi.subtotal)))

        if (receipt.transaksi.diskon > 0) {
            sb.append(alignedLine("Diskon", "-" + ReceiptFormatter.formatMoney(receipt.transaksi.diskon)))
        }
        if (receipt.transaksi.pajak > 0) {
            sb.append(alignedLine("Pajak", ReceiptFormatter.formatMoney(receipt.transaksi.pajak)))
        }

        sb.append("[L]<u>").append("-".repeat(charsPerLine)).append("</u>\n")
        sb.append("[L]<b><font size='big'>TOTAL</font></b>")
            .append("[R]<b><font size='big'>").append(ReceiptFormatter.formatMoney(receipt.transaksi.total))
            .append("</font></b>\n")
        sb.append("[L]\n")

        // ===== PAYMENTS =====
        receipt.payments.forEach { payment ->
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

        sb.append("[L]\n")

        // ===== QR CODE (OPSIONAL) =====
        if (receipt.toko.cetakQr) {
            val qrData = receipt.transaksi.nomor
            sb.append("[C]<qrcode size='6'>")
                .append(escapeDantSuText(qrData))
                .append("</qrcode>\n")
        }

        // ===== FOOTER =====
        sb.append("[C]<b>Terima kasih atas kunjungan Anda!</b>\n")
        if (receipt.footer.isNotBlank() && receipt.footer != "Terima kasih") {
            sb.append("[C]<font size='small'>").append(escapeDantSuText(receipt.footer)).append("</font>\n")
        }

        return sb.toString()
    }

    private fun escapeDantSuText(text: String): String {
        return text
            .replace("[", "&#91;")
            .replace("]", "&#93;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    private fun truncate(text: String, maxWidth: Int): String {
        return if (text.length > maxWidth) text.take(maxWidth - 2) + ".." else text
    }

    private fun alignedLine(label: String, value: String): String = "[L]$label[R]$value\n"

    private fun alignedLineBold(label: String, value: String): String = "[L]<b>$label</b>[R]<b>$value</b>\n"

    private fun isRetryableError(e: Exception): Boolean {
        return e is java.io.IOException
            || e is java.net.SocketTimeoutException
            || e is EscPosConnectionException
    }
}
