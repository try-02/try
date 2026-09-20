package com.sentral.org.ui.screen.shift

import com.sentral.org.data.model.ShiftSummary

enum class TutupShiftStep {
    BLIND_COUNT,      // Tahap 1: Input uang fisik tanpa melihat sistem
    REKONSILIASI,     // Tahap 2: Tampilkan perbandingan sistem vs fisik + selisih
}

data class TutupShiftUiState(
    val step: TutupShiftStep = TutupShiftStep.BLIND_COUNT,
    val shiftId: Long? = null,
    val namaKasir: String = "",
    val dimulaiPada: Long = 0L,
    val kasAktualInput: String = "",
    val catatan: String = "",
    val summary: ShiftSummary? = null,
    val sedangMemproses: Boolean = false,
    val sedangCetak: Boolean = false,
    val pesanError: String? = null,
) {
    val kasAktualNominal: Long get() = kasAktualInput.filter(Char::isDigit).toLongOrNull() ?: 0L
}

sealed interface TutupShiftEvent {
    data object ShiftSelesaiDanKeluar : TutupShiftEvent
    data class Pesan(val teks: String) : TutupShiftEvent
}