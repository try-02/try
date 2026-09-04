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
        android.util.Log.e("CheckoutVM", "🚀 checkout() called with cartId=${request.cartId}")
        
        if (_state.value is CheckoutUiState.Processing) {
            android.util.Log.e("CheckoutVM", "⚠️ Already processing, ignoring") // ini log w loh
            return
        }
        
        viewModelScope.launch {
            _state.value = CheckoutUiState.Processing
            android.util.Log.e("CheckoutVM", "⏳ Calling checkoutService.checkout()")
            
            val result = checkoutService.checkout(request)
            
            android.util.Log.e("CheckoutVM", "📥 checkoutService returned: ${if (result.isSuccess) "SUCCESS" else "FAILURE"}")
            
            result.fold(
                onSuccess = { checkoutResult ->
                    android.util.Log.e("CheckoutVM", "✅ Checkout success: transactionId=${checkoutResult.transactionId}")
                    
                    // ===== AUTO-PRINT: Trigger cetak struk setelah checkout sukses =====
                    triggerAutoPrint(checkoutResult.transactionId)
                    
                    _state.value = CheckoutUiState.Success(checkoutResult)
                },
                onFailure = { error ->
                    android.util.Log.e("CheckoutVM", "❌ Checkout failed: ${error.message}", error)
                    _state.value = CheckoutUiState.Error(error.message ?: "Checkout gagal") 
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
                android.util.Log.e("CheckoutVM", "Auto-print triggered for transaction $transactionId")
                
                // Load semua data yang dibutuhkan untuk cetak struk
                val transaksi = transaksiRepo.getById(transactionId)
                    ?: throw IllegalStateException("Transaksi tidak ditemukan setelah checkout")
                
                val items = transaksiRepo.getItems(transactionId)
                val payments = transaksiRepo.getPayments(transactionId)
                val profilToko = profilRepo.get()

                android.util.Log.e("CheckoutVM", "Loaded transaction data: ${items.size} items, ${payments.size} payments")

                // Format menjadi ReceiptData
                val receiptData = ReceiptFormatter.format(
                    toko = profilToko,
                    transaksi = transaksi,
                    items = items,
                    payments = payments,
                )

                android.util.Log.e("CheckoutVM", "Formatted receipt data, enqueueing to printer service")

                // Enqueue ke printer service (non-blocking)
                printerService.enqueue(receiptData) { result ->
                    when (result) {
                        is com.sentral.org.data.model.PrintResult.Success -> {
                            android.util.Log.e("CheckoutVM", "✅ Print success for transaction $transactionId")
                        }
                        is com.sentral.org.data.model.PrintResult.Failure -> {
                            android.util.Log.e("CheckoutVM", "❌ Print failed for transaction $transactionId: ${result.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CheckoutVM", "Auto-print failed for transaction $transactionId: ${e.message}", e)
            }
        }
    }

    fun reset() {
        _state.value = CheckoutUiState.Idle
    }
}