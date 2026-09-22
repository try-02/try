package com.sentral.org.ui.screen.laporan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentral.org.data.repository.LaporanRepository
import com.sentral.org.shared.currentTimeMillis
import com.sentral.org.shared.getStartOfDayMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class LaporanViewModel(
    private val laporanRepo: LaporanRepository,
) : ViewModel() {
    private val _range = MutableStateFlow(LaporanRange.HARI_INI)
    val range: StateFlow<LaporanRange> = _range.asStateFlow()

    val uiState: StateFlow<LaporanUiState> =
        _range
            .flatMapLatest { selectedRange ->
                val now = currentTimeMillis()
                val (start, end) = hitungRentangWaktu(selectedRange, now)
                laporanRepo.observeLaporan(start, end).map { data ->
                    LaporanUiState(
                        range = selectedRange,
                        data = data.toUi(),
                        sedangMemuat = false,
                    )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = LaporanUiState(),
            )

    fun pilihRange(range: LaporanRange) {
        _range.value = range
    }

    private fun hitungRentangWaktu(
        range: LaporanRange,
        now: Long,
    ): Pair<Long, Long> {
        val startOfToday = getStartOfDayMillis(now)
        val startOfTomorrow = startOfToday + 86_400_000L

        return when (range) {
            LaporanRange.HARI_INI -> Pair(startOfToday, startOfTomorrow)
            LaporanRange.TUJUH_HARI -> Pair(startOfTomorrow - (7L * 86_400_000L), startOfTomorrow)
            LaporanRange.TIGA_PULUH_HARI -> Pair(startOfTomorrow - (30L * 86_400_000L), startOfTomorrow)
        }
    }
}
