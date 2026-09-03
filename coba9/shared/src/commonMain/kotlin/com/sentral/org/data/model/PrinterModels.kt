package com.sentral.org.data.model

/**
 * Status koneksi printer untuk ditampilkan di UI.
 */
enum class PrinterStatus {
    SIAP,           // Printer online, siap cetak
    SIBUK,          // Ada job di antrian
    ERROR,          // Gagal koneksi/cetak
    DINONAKTIFKAN,  // Otomatis dinonaktifkan setelah gagal berulang
}

/**
 * Jenis koneksi printer. Sesuai dengan kolom di PrinterEntity.
 */
enum class PrinterConnectionType {
    BLUETOOTH,
    USB,
    WIFI,
}

/**
 * Hasil eksekusi cetak. Dibawa oleh queue untuk update health tracking.
 */
sealed interface PrintResult {
    data object Success : PrintResult
    data class Failure(val message: String, val isRetryable: Boolean) : PrintResult
}

/**
 * Data struk yang siap dicetak. Dibuat oleh ReceiptFormatter dari CheckoutResult.
 * 
 * DESAIN: Model ini sengaja terpisah dari entity DB agar format struk bisa 
 * berubah tanpa perlu migration database.
 */
data class ReceiptData(
    val toko: StoreInfo,
    val transaksi: TransactionInfo,
    val items: List<ReceiptItem>,
    val payments: List<PaymentInfo>,
    val footer: String,
)

data class StoreInfo(
    val nama: String,
    val alamat: String,
    val footer: String,
)

data class TransactionInfo(
    val nomor: String,
    val kasir: String,
    val waktu: Long,
    val subtotal: Long,
    val diskon: Long,
    val pajak: Long,
    val total: Long,
)

data class ReceiptItem(
    val nama: String,
    val jumlah: Long,       // scaled quantity
    val hargaSatuan: Long,
    val totalBaris: Long,
)

data class PaymentInfo(
    val metode: MetodePembayaran,
    val jumlah: Long,
    val diterima: Long?,
    val kembalian: Long?,
)