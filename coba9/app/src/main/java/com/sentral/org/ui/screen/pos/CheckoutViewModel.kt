package com.sentral.org.ui.screen.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentral.org.data.model.CheckoutRequest
import com.sentral.org.data.model.CheckoutResult
import com.sentral.org.data.repository.TransaksiRepository
import com.sentral.org.data.repository.ProdukRepository
import com.sentral.org.data.repository.ProfilTokoRepository
import com.sentral.org.data.service.CheckoutService
import com.sentral.org.data.service.PrinterService
import com.sentral.org.data.service.ReceiptFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CheckoutUiState {
    data object Idle : CheckoutUiState
    data object Processing : CheckoutUiState
    data class Success(val result: CheckoutResult) : CheckoutUiState
    data class Error(val message: String) : CheckoutUiState
}

class CheckoutViewModel(
    private val checkoutService: CheckoutService,
    private val printerService: PrinterService,
    private val transaksiRepo: TransaksiRepository,
    private val produkRepo: ProdukRepository,
    private val profilRepo: ProfilTokoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Idle)
    val state: StateFlow<CheckoutUiState> = _state.asStateFlow()

    fun checkout(request: CheckoutRequest) {
        if (_state.value is CheckoutUiState.Processing) return
        viewModelScope.launch {
            _state.value = CheckoutUiState.Processing
            val result = checkoutService.checkout(request)
            
            result.fold(
                onSuccess = { checkoutResult ->
                    // ===== AUTO-PRINT: Trigger cetak struk setelah checkout sukses =====
                    triggerAutoPrint(checkoutResult.transactionId)
                    
                    _state.value = CheckoutUiState.Success(checkoutResult)
                },
                onFailure = { 
                    _state.value = CheckoutUiState.Error(it.message ?: "Checkout gagal") 
                },
            )
        }
    }

    /**
     * Trigger cetak struk otomatis setelah checkout sukses.
     * 
     * DESAIN:
     * - Load data transaksi + items + payments + profil toko
     * - Format menjadi ReceiptData
     * - Enqueue ke PrinterService (non-blocking)
     * - Jika gagal load data, log error tapi jangan gagalkan checkout
     * - Cetak dilakukan async di background, tidak memblokir UI
     */
    private fun triggerAutoPrint(transactionId: Long) {
        viewModelScope.launch {
            try {
                // Load semua data yang dibutuhkan untuk cetak struk
                val transaksi = transaksiRepo.getById(transactionId)
                    ?: throw IllegalStateException("Transaksi tidak ditemukan setelah checkout")
                
                val items = transaksiRepo.getItems(transactionId)
                val payments = transaksiRepo.getPayments(transactionId)
                val profilToko = profilRepo.get()

                // Format menjadi ReceiptData
                val receiptData = ReceiptFormatter.format(
                    toko = profilToko,
                    transaksi = transaksi,
                    items = items,
                    payments = payments,
                )

                // Enqueue ke printer service (non-blocking)
                printerService.enqueue(receiptData) { result ->
                    // Callback saat cetak selesai (sukses/gagal)
                    // Bisa dipakai untuk tampilkan snackbar "Cetak gagal" jika needed
                    when (result) {
                        is com.sentral.org.data.model.PrintResult.Success -> {
                            // Log sukses (opsional)
                        }
                        is com.sentral.org.data.model.PrintResult.Failure -> {
                            // TODO: Tampilkan snackbar "Cetak struk gagal, bisa cetak ulang di Riwayat"
                        }
                    }
                }
            } catch (e: Exception) {
                // Gagal load data untuk cetak, tapi checkout sudah sukses
                // Log error tapi jangan gagalkan alur user
                android.util.Log.e("CheckoutViewModel", "Auto-print failed: ${e.message}", e)
            }
        }
    }

    fun reset() {
        _state.value = CheckoutUiState.Idle
    }
}