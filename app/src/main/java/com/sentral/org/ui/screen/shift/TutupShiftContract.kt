package com.sentral.org.ui.screen.shift

data class ShiftSummaryUi(
    val kasAwal: Long,
    val totalPenjualanTunai: Long,
    val totalReturTunai: Long,
    val kasDiharapkan: Long,
    val kasAktual: Long?,
    val selisihKas: Long?,
)

enum class TutupShiftStep {
    BLIND_COUNT,
    REKONSILIASI,
}

data class TutupShiftUiState(
    val step: TutupShiftStep = TutupShiftStep.BLIND_COUNT,
    val shiftId: Long? = null,
    val namaKasir: String = "",
    val dimulaiPada: Long = 0L,
    val kasAktualInput: String = "",
    val catatan: String = "",
    val summary: ShiftSummaryUi? = null,
    val sedangMemproses: Boolean = false,
    val sedangCetak: Boolean = false,
    val pesanError: String? = null,
) {
    val kasAktualNominal: Long get() = kasAktualInput.filter(Char::isDigit).toLongOrNull() ?: 0L
}

sealed interface TutupShiftEvent {
    data object ShiftSelesaiDanKeluar : TutupShiftEvent

    data class Pesan(
        val teks: String,
    ) : TutupShiftEvent
}
