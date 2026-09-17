package com.sentral.org.data.usecase

import co.touchlab.kermit.Logger
import com.sentral.org.data.model.PrintResult
import com.sentral.org.data.repository.ProfilTokoRepository
import com.sentral.org.data.repository.TransaksiRepository
import com.sentral.org.data.service.PrinterService
import com.sentral.org.data.service.ReceiptFormatter

/**
 * Use case untuk auto-print struk setelah checkout sukses.
 * 
 * DESIGN:
 * - Centralizes auto-print logic (sebelumnya duplikat di KasirVM & CheckoutVM)
 * - Non-blocking: tidak menggagalkan flow utama jika print gagal
 * - Graceful error handling: log error tapi jangan throw
 * 
 * USAGE:
 * ```kotlin
 * viewModelScope.launch {
 *     autoPrintUseCase(transactionId)
 * }
 * ```
 */
class AutoPrintUseCase(
    private val transaksiRepo: TransaksiRepository,
    private val profilRepo: ProfilTokoRepository,
    private val printerService: PrinterService,
) {
    private val log = Logger.withTag("AutoPrint")
    
    suspend operator fun invoke(transactionId: Long) {
        try {
            log.d { "Auto-print triggered for transaction $transactionId" }
            
            // Load semua data yang dibutuhkan untuk cetak struk
            val transaksi = transaksiRepo.getById(transactionId)
                ?: run {
                    log.e { "Transaksi tidak ditemukan: $transactionId" }
                    return
                }
            
            val items = transaksiRepo.getItems(transactionId)
            val payments = transaksiRepo.getPayments(transactionId)
            val profilToko = profilRepo.get()
            
            log.d { "Loaded transaction data: ${items.size} items, ${payments.size} payments" }
            
            // Format menjadi ReceiptData
            val receiptData = ReceiptFormatter.format(
                toko = profilToko,
                transaksi = transaksi,
                items = items,
                payments = payments,
            )
            
            log.d { "Formatted receipt data, enqueueing to printer service" }
            
            // Enqueue ke printer service (non-blocking)
            printerService.enqueue(receiptData) { result ->
                when (result) {
                    is PrintResult.Success -> {
                        log.i { "Print success for transaction $transactionId" }
                    }
                    is PrintResult.Failure -> {
                        log.e { "Print failed for transaction $transactionId: ${result.message}" }
                    }
                }
            }
        } catch (e: Exception) {
            log.e(e) { "Auto-print exception for transaction $transactionId: ${e.message}" }
        }
    }
}
