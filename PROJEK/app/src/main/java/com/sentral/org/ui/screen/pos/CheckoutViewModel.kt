package com.sentral.org.ui.screen.pos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sentral.org.R
import com.sentral.org.data.model.CheckoutRequest
import com.sentral.org.data.model.CheckoutResult
import com.sentral.org.data.service.CheckoutService
import com.sentral.org.data.usecase.AutoPrintUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import co.touchlab.kermit.Logger

sealed interface CheckoutUiState {
    data object Idle : CheckoutUiState
    data object Processing : CheckoutUiState
    data class Success(val result: CheckoutResult) : CheckoutUiState
    data class Error(val message: String) : CheckoutUiState
}

class CheckoutViewModel(
    application: Application,
    private val checkoutService: CheckoutService,
    private val autoPrintUseCase: AutoPrintUseCase,
) : AndroidViewModel(application) {

    private fun getString(resId: Int): String = getApplication<Application>().getString(resId)

    private companion object {
        val log = Logger.withTag("CheckoutVM")
    }

    private val _state = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Idle)
    val state: StateFlow<CheckoutUiState> = _state.asStateFlow()

    fun checkout(request: CheckoutRequest) {
        log.d { "checkout() called with cartId=${request.cartId}" }

        if (_state.value is CheckoutUiState.Processing) {
            log.w { "Already processing, ignoring" }
            return
        }

        viewModelScope.launch {
            _state.update { CheckoutUiState.Processing }
            log.d { "Calling checkoutService.checkout()" }

            checkoutService.checkout(request).fold(
                onSuccess = { checkoutResult ->
                    log.i { "Checkout success: transactionId=${checkoutResult.transactionId}" }
                    viewModelScope.launch { autoPrintUseCase(checkoutResult.transactionId) }
                    _state.update { CheckoutUiState.Success(checkoutResult) }
                },
                onFailure = { error ->
                    log.e(error) { "Checkout failed: ${error.message}" }
                    _state.update { CheckoutUiState.Error(error.message ?: getString(R.string.err_checkout_failed)) }
                },
            )
        }
    }

    fun reset() {
        _state.update { CheckoutUiState.Idle }
    }
}
