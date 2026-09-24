package com.sentral.org.hardware

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import androidx.core.net.toUri
import co.touchlab.kermit.Logger
import com.dantsu.escposprinter.EscPosPrinterCommands
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import kotlin.math.roundToInt

/**
 * Processor khusus untuk memuat, memvalidasi, dan mengonversi logo toko
 * menjadi byte array ESC/POS (monochrome) untuk dicetak oleh printer termal.
 *
 * Dirancang sebagai stateless object untuk efisiensi dan kemudahan pengujian.
 *
 * SAFETY GUARDS (Anti-OOM & Timeout):
 * 1. Timeout keseluruhan proses (default 3 detik).
 * 2. Validasi ukuran file sebelum decode pixel (>10MB = skip).
 * 3. Validasi dimensi bounds sebelum decode (>4096px = skip).
 * 4. Penggunaan `inSampleSize` untuk hemat memori saat decoding.
 * 5. Bitmap selalu di-recycle setelah dikonversi menjadi byte array.
 * 6. `InputStream` otomatis ditutup via blok `use {}`.
 * 7. Graceful degradation: jika logo gagal diproses, proses cetak struk tetap berjalan.
 */
object EscPosLogoProcessor {
    private const val TAG = "EscPosLogoProcessor"
    private val log = Logger.withTag(TAG)

    // Target width di pixels: 48mm × 203 DPI / 25.4 ≈ 384px
    private const val TARGET_LOGO_WIDTH = 384
    private const val TARGET_LOGO_HEIGHT = 128 // Max height DantSu support

    // Source gambar max: 4096×4096 (16MP). Di atas ini, skip.
    private const val MAX_SOURCE_DIMENSION = 4096

    // File size max: 10MB. Di atas ini, skip.
    private const val MAX_LOGO_FILE_SIZE_BYTES = 10L * 1024 * 1024

    // Timeout load logo: 3 detik
    private const val LOGO_LOAD_TIMEOUT_MS = 3000L

    /**
     * Memuat logo dari URI, melakukan resize proporsional, dan mengonversinya
     * menjadi byte array ESC/POS.
     *
     * @param context Context Android untuk mengakses ContentResolver.
     * @param uriString URI string dari logo toko.
     * @return Byte array ESC/POS jika berhasil, atau `null` jika gagal/timeout/dilewati.
     */
    suspend fun loadAndConvertLogo(
        context: Context,
        uriString: String,
    ): ByteArray? = withTimeoutOrNull(LOGO_LOAD_TIMEOUT_MS) {
        withContext(Dispatchers.IO) {
            try {
                val uri = uriString.toUri()
                if (!isLogoSafeToDecode(context, uri)) return@withContext null
                processBitmapToEscPosBytes(context, uri)
            } catch (e: Exception) {
                logException(e)
                null
            } catch (e: OutOfMemoryError) {
                log.e(e) { "OOM loading logo: ${e.message}" }
                null
            }
        }
    }

    /**
     * Melakukan validasi keamanan memori (ukuran file & dimensi) sebelum decode pixel.
     */
    private fun isLogoSafeToDecode(
        context: Context,
        uri: Uri,
    ): Boolean {
        val fileSize = getFileSize(context, uri)
        if (fileSize != null && fileSize > MAX_LOGO_FILE_SIZE_BYTES) {
            log.w { "Logo file too large: $fileSize bytes (max: $MAX_LOGO_FILE_SIZE_BYTES)" }
            return false
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: return false

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            log.w { "Logo dimensions invalid: ${bounds.outWidth}x${bounds.outHeight}" }
            return false
        }

        if (bounds.outWidth > MAX_SOURCE_DIMENSION || bounds.outHeight > MAX_SOURCE_DIMENSION) {
            log.w { "Logo too large: ${bounds.outWidth}x${bounds.outHeight} (max: $MAX_SOURCE_DIMENSION)" }
            return false
        }
        return true
    }

    /**
     * Decode bitmap dengan sample size yang aman, lalu delegasikan ke fungsi resize.
     */
    private fun processBitmapToEscPosBytes(
        context: Context,
        uri: Uri,
    ): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: return null

        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, TARGET_LOGO_WIDTH, TARGET_LOGO_HEIGHT)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val original: Bitmap =
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

        return resizeAndConvertToBytes(original)
    }

    /**
     * Resize bitmap ke dimensi target printer, convert ke bytes ESC/POS, dan recycle memori.
     */
    private fun resizeAndConvertToBytes(original: Bitmap): ByteArray {
        val scaleFactor = minOf(
            TARGET_LOGO_WIDTH.toFloat() / original.width,
            TARGET_LOGO_HEIGHT.toFloat() / original.height,
        )
        val targetWidth = (original.width * scaleFactor).roundToInt().coerceAtLeast(1)
        val targetHeight = (original.height * scaleFactor).roundToInt().coerceAtLeast(1)

        val resized = original.scale(targetWidth, targetHeight, filter = true)

        if (resized !== original) {
            original.recycle()
        }

        val bytes = EscPosPrinterCommands.bitmapToBytes(resized, false)
        resized.recycle()

        return bytes
    }

    /**
     * Sentralisasi logging exception untuk mengurangi kompleksitas siklomatik pada blok catch.
     */
    private fun logException(e: Exception) {
        when (e) {
            is SecurityException -> log.e(e) { "No permission to read logo: ${e.message}" }
            is IOException -> log.e(e) { "Failed to load logo: ${e.message}" }
            is IllegalArgumentException -> log.e(e) { "Invalid logo data: ${e.message}" }
            else -> log.e(e) { "Unexpected error loading logo: ${e.message}" }
        }
    }

    /**
     * Menghitung ukuran file dari URI.
     * Mendukung skema `file://` dan `content://`.
     */
    private fun getFileSize(
        context: Context,
        uri: Uri,
    ): Long? = try {
        when (uri.scheme) {
            "file" -> {
                val path = uri.path ?: return null
                val size = java.io.File(path).length()
                size.takeIf { it > 0 }
            }
            "content" -> getContentFileSize(context, uri)
            else -> null
        }
    } catch (e: SecurityException) {
        log.e(e) { "Cannot get file size for $uri: ${e.message}" }
        null
    } catch (e: IllegalArgumentException) {
        log.e(e) { "Invalid URI for file size: $uri: ${e.message}" }
        null
    }

    private fun getContentFileSize(
        context: Context,
        uri: Uri,
    ): Long? = context.contentResolver
        .query(uri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (sizeIndex >= 0) cursor.getLong(sizeIndex) else null
        }

    /**
     * Menghitung `inSampleSize` (pangkat 2) untuk BitmapFactory.
     */
    private fun calculateSampleSize(
        width: Int,
        height: Int,
        targetWidth: Int,
        targetHeight: Int,
    ): Int {
        var sampleSize = 1
        if (width > targetWidth || height > targetHeight) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            while ((halfWidth / sampleSize) >= targetWidth && (halfHeight / sampleSize) >= targetHeight) {
                sampleSize *= 2
            }
        }
        return sampleSize.coerceAtLeast(1)
    }
}
