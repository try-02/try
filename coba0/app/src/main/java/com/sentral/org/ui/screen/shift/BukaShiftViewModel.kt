package com.sentral.org.ui.screen.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentral.org.data.dao.KasirDao
import com.sentral.org.data.service.ShiftService
import com.sentral.org.data.session.ActiveSesiKasirProvider
import com.sentral.org.shared.currentTimeMillis
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BukaShiftViewModel(
    private val shiftService: ShiftService,
    private val kasirDao: KasirDao,
    private val sessionProvider: ActiveSesiKasirProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BukaShiftUiState())
    val uiState: StateFlow<BukaShiftUiState> = _uiState.asStateFlow()

    private val _event = Channel<BukaShiftEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    init {
        muatDataKasirLogin()
    }

    private fun muatDataKasirLogin() {
        viewModelScope.launch {
            val kasirId = sessionProvider.kasirLoginId.value
            if (kasirId != null) {
                val kasir = kasirDao.getById(kasirId)
                if (kasir != null) {
                    _uiState.update { it.copy(kasirId = kasir.id, namaKasir = kasir.nama) }
                } else {
                    _event.send(BukaShiftEvent.KembaliKeLogin)
                }
            } else {
                _event.send(BukaShiftEvent.KembaliKeLogin)
            }
        }
    }

    fun tekanAngka(digit: String) {
        val current = _uiState.value.modalAwalInput
        if (current.length < 11 && !_uiState.value.sedangMemproses) {
            val updated = (current.filter(Char::isDigit) + digit).take(11)
            _uiState.update { it.copy(modalAwalInput = updated, pesanError = null) }
        }
    }

    fun hapusDigit() {
        val current = _uiState.value.modalAwalInput
        if (current.isNotEmpty()) {
            _uiState.update { it.copy(modalAwalInput = current.dropLast(1), pesanError = null) }
        }
    }

    fun setNominalCepat(nominal: Long) {
        _uiState.update { it.copy(modalAwalInput = nominal.toString(), pesanError = null) }
    }

    fun setCatatan(catatan: String) {
        _uiState.update { it.copy(catatan = catatan) }
    }

    fun logoutGantiKasir() {
        sessionProvider.logout()
        viewModelScope.launch {
            _event.send(BukaShiftEvent.KembaliKeLogin)
        }
    }

    fun submitBukaShift() {
        val state = _uiState.value
        val kasirId = state.kasirId ?: return
        val modalAwal = state.modalAwalNominal

        viewModelScope.launch {
            _uiState.update { it.copy(sedangMemproses = true) }
            val now = currentTimeMillis()

            shiftService
                .open(
                    cashierId = kasirId,
                    openingCash = modalAwal,
                    now = now,
                    note = state.catatan.trim(),
                ).fold(
                    onSuccess = {
                        _uiState.update { it.copy(sedangMemproses = false) }
                        _event.send(BukaShiftEvent.ShiftBerhasilDibuka)
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                sedangMemproses = false,
                                pesanError = error.message ?: "Gagal membuka shift",
                            )
                        }
                    },
                )
        }
    }
}
