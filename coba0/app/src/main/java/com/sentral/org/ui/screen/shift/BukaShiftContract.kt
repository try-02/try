package com.sentral.org.ui.screen.shift

data class BukaShiftUiState(
    val kasirId: Long? = null,
    val namaKasir: String = "",
    val modalAwalInput: String = "",
    val catatan: String = "",
    val sedangMemproses: Boolean = false,
    val pesanError: String? = null,
) {
    val modalAwalNominal: Long get() = modalAwalInput.filter(Char::isDigit).toLongOrNull() ?: 0L
}

sealed interface BukaShiftEvent {
    data object ShiftBerhasilDibuka : BukaShiftEvent

    data object KembaliKeLogin : BukaShiftEvent

    data class Pesan(
        val teks: String,
    ) : BukaShiftEvent
}
