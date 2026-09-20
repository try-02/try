package com.sentral.org.ui.screen.auth

import com.sentral.org.data.entity.KasirEntity

data class LoginKasirUiState(
    val daftarKasir: List<KasirEntity> = emptyList(),
    val kasirTerpilih: KasirEntity? = null,
    val pinInput: String = "",
    val sedangMemproses: Boolean = false,
    val pesanError: String? = null,
    val sisaDetikTerkunci: Long? = null,
)

sealed interface LoginKasirEvent {
    data class NavigasiKePosUtama(val namaKasir: String) : LoginKasirEvent
    data class NavigasiKeBukaShift(val kasirId: Long, val namaKasir: String) : LoginKasirEvent
    data class Pesan(val teks: String) : LoginKasirEvent
}