package com.sentral.org.hardware

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

internal class LogoLoader(
    private val context: Context,
    private val log: Logger,
) {

    companion object {
        private const val TARGET_LOGO_WIDTH = 384
        private const val TARGET_LOGO_HEIGHT = 128
        private const val MAX_SOURCE_DIMENSION = 4096
        private const val MAX_LOGO_FILE_SIZE_BYTES = 10L * 1024 * 1024
        private const val LOGO_LOAD_TIMEOUT_MS = 3000L
    }

    suspend fun loadAndConvertLogo(uriString: String): ByteArray? {
        return withTimeoutOrNull(LOGO_LOAD_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                try {
                    val uri = uriString.toUri()

                    val fileSize = getFileSize(uri)
                    if (fileSize != null && fileSize > MAX_LOGO_FILE_SIZE_BYTES) {
                        log.w { "Logo file too large: $fileSize bytes (max: $MAX_LOGO_FILE_SIZE_BYTES)" }
                        return@withContext null
                    }

                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, bounds)
                    } ?: return@withContext null

                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                        log.w { "Logo dimensions invalid: ${bounds.outWidth}x${bounds.outHeight}" }
                        return@withContext null
                    }

                    if (bounds.outWidth > MAX_SOURCE_DIMENSION || bounds.outHeight > MAX_SOURCE_DIMENSION) {
                        log.w { "Logo too large: ${bounds.outWidth}x${bounds.outHeight} (max: $MAX_SOURCE_DIMENSION)" }
                        return@withContext null
                    }

                    val sampleSize = calculateSampleSize(
                        bounds.outWidth,
                        bounds.outHeight,
                        TARGET_LOGO_WIDTH,
                        TARGET_LOGO_HEIGHT,
                    )
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }

                    val original: Bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, decodeOptions)
                    } ?: return@withContext null

                    val scale = minOf(
                        TARGET_LOGO_WIDTH.toFloat() / original.width,
                        TARGET_LOGO_HEIGHT.toFloat() / original.height,
                    )
                    val targetWidth = (original.width * scale).roundToInt().coerceAtLeast(1)
                    val targetHeight = (original.height * scale).roundToInt().coerceAtLeast(1)

                    val resized = original.scale(targetWidth, targetHeight, filter = true)

                    if (resized !== original) {
                        original.recycle()
                    }

                    val bytes = EscPosPrinterCommands.bitmapToBytes(resized, false)
                    resized.recycle()

                    bytes
                } catch (e: OutOfMemoryError) {
                    log.e(e) { "OOM loading logo: ${e.message}" }
                    null
                } catch (e: SecurityException) {
                    log.e(e) { "No permission to read logo: ${e.message}" }
                    null
                } catch (e: IOException) {
                    log.e(e) { "Failed to load logo: ${e.message}" }
                    null
                } catch (e: IllegalArgumentException) {
                    log.e(e) { "Invalid logo data: ${e.message}" }
                    null
                }
            }
        }
    }

    private fun getContentFileSize(uri: Uri): Long? {
        return context.contentResolver
            .query(uri, null, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIndex >= 0) cursor.getLong(sizeIndex) else null
            }
    }

    private fun getFileSize(uri: Uri): Long? {
        return try {
            when (uri.scheme) {
                "file" -> {
                    val path = uri.path ?: return null
                    val size = java.io.File(path).length()
                    size.takeIf { it > 0 }
                }
                "content" -> getContentFileSize(uri)
                else -> null
            }
        } catch (e: SecurityException) {
            log.e(e) { "Cannot get file size for $uri: ${e.message}" }
            null
        } catch (e: IllegalArgumentException) {
            log.e(e) { "Invalid URI for file size: $uri: ${e.message}" }
            null
        }
    }

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