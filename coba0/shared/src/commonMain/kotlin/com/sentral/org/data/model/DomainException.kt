package com.sentral.org.data.model

sealed class PosDataException(message: String) : IllegalStateException(message) {
    class NotFound(message: String) : PosDataException(message)
    class InvalidState(message: String) : PosDataException(message)
    class Validation(message: String) : PosDataException(message)
    class InsufficientDamagedStock(message: String) : PosDataException(message)
    class InsufficientStock(message: String) : PosDataException(message)
    class Duplicate(message: String) : PosDataException(message)
    class ProductInactive(val productId: Long, val productName: String) :
        PosDataException("Produk '$productName' tidak aktif. Hapus dari keranjang untuk melanjutkan.")
}

/**
 * runCatching standar menelan CancellationException dan merusak structured concurrency.
 * WAJIB dipakai oleh semua service suspend.
 */
inline fun <T> suspendRunCatching(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (c: kotlinx.coroutines.CancellationException) {
        throw c
    } catch (e: Exception) {
        Result.failure(e)
    }
}