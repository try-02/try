package com.sentral.org.ui.screen.laporan

import com.sentral.org.data.model.LaporanPenjualan

enum class LaporanRange(val label: String) {
    HARI_INI("Hari Ini"),
    TUJUH_HARI("7 Hari"),
    TIGA_PULUH_HARI("30 Hari"),
}

data class LaporanUiState(
    val range: LaporanRange = LaporanRange.HARI_INI,
    val data: LaporanPenjualan? = null,
    val sedangMemuat: Boolean = true,
)