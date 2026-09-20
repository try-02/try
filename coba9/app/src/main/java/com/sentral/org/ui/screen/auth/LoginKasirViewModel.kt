package com.sentral.org.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentral.org.data.dao.KasirDao
import com.sentral.org.data.entity.KasirEntity
import com.sentral.org.data.service.AuthResult
import com.sentral.org.data.service.AuthService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginKasirViewModel(
    private val kasirDao: KasirDao,
    private val authService: AuthService,
) : ViewModel() {

    private val _stateInternal = MutableStateFlow(LoginKasirUiState())
    private val _event = Channel<LoginKasirEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    val uiState: StateFlow<LoginKasirUiState> = combine(
        kasirDao.observeAktif(),
        _stateInternal,
    ) { kasirs, internal ->
        internal.copy(
            daftarKasir = kasirs,
            kasirTerpilih = internal.kasirTerpilih ?: kasirs.firstOrNull(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoginKasirUiState())

    fun pilihKasir(kasir: KasirEntity) {
        _stateInternal.update {
            it.copy(
                kasirTerpilih = kasir,
                pinInput = "",
                pesanError = null,
                sisaDetikTerkunci = null,
            )
        }
    }

    fun tekanAngka(digit: String) {
        val current = _stateInternal.value.pinInput
        if (current.length < 6 && !_stateInternal.value.sedangMemproses) {
            val updated = current + digit
            _stateInternal.update { it.copy(pinInput = updated, pesanError = null) }
            // Auto-submit bila panjang PIN sudah mencapai 6 digit
            if (updated.length == 6) {
                submitPin(updated)
            }
        }
    }

    fun hapusDigit() {
        val current = _stateInternal.value.pinInput
        if (current.isNotEmpty()) {
            _stateInternal.update { it.copy(pinInput = current.dropLast(1), pesanError = null) }
        }
    }

    fun bersihkanPin() {
        _stateInternal.update { it.copy(pinInput = "", pesanError = null) }
    }

    fun submitPin(pinOverride: String? = null) {
        val kasir = uiState.value.kasirTerpilih ?: return
        val pin = pinOverride ?: uiState.value.pinInput
        if (pin.length < 4) {
            _stateInternal.update { it.copy(pesanError = "PIN minimal 4 digit") }
            return
        }

        viewModelScope.launch {
            _stateInternal.update { it.copy(sedangMemproses = true) }
            when (val result = authService.login(kasir.id, pin)) {
                is AuthResult.Success -> {
                    _stateInternal.update { it.copy(sedangMemproses = false, pinInput = "", pesanError = null) }
                    if (result.hasOpenShift) {
                        _event.send(LoginKasirEvent.NavigasiKePosUtama(result.namaKasir))
                    } else {
                        _event.send(LoginKasirEvent.NavigasiKeBukaShift(result.kasirId, result.namaKasir))
                    }
                }
                is AuthResult.Failed -> {
                    _stateInternal.update {
                        it.copy(
                            sedangMemproses = false,
                            pinInput = "",
                            pesanError = "PIN salah. Sisa percobaan: ${result.sisaPercobaan}",
                        )
                    }
                }
                is AuthResult.Locked -> {
                    _stateInternal.update {
                        it.copy(
                            sedangMemproses = false,
                            pinInput = "",
                            sisaDetikTerkunci = result.sisaDetik,
                            pesanError = "Terlalu banyak percobaan. Terkunci ${result.sisaDetik} detik",
                        )
                    }
                }
                is AuthResult.KasirTidakAktif -> {
                    _stateInternal.update {
                        it.copy(sedangMemproses = false, pinInput = "", pesanError = "Kasir tidak aktif")
                    }
                }
            }
        }
    }
}