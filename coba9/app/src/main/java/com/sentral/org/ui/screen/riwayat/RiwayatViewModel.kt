package com.sentral.org.ui.screen.riwayat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.sentral.org.data.entity.TransaksiDenganDetail
import com.sentral.org.data.entity.TransaksiEntity
import com.sentral.org.data.model.PrintResult
import com.sentral.org.data.model.StatusTransaksi
import com.sentral.org.data.model.TransaksiFilter
import com.sentral.org.data.repository.ProfilTokoRepository
import com.sentral.org.data.repository.TransaksiRepository
import com.sentral.org.data.service.PrinterService
import com.sentral.org.data.service.ReceiptFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.sentral.org.export.TransaksiExportUseCase

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class RiwayatViewModel(
    private val transaksiRepo: TransaksiRepository,
    private val profilRepo: ProfilTokoRepository,
    private val printerService: PrinterService,
    private val exportUseCase: TransaksiExportUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RiwayatUiState())
    val uiState: StateFlow<RiwayatUiState> = _uiState.asStateFlow()

    private val _event = Channel<RiwayatEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    private val _filterFlow = MutableStateFlow(TransaksiFilter())

    /**
     * Stream PagingData yang secara otomatis bereaksi terhadap perubahan filter.
     * Menggunakan debounce 300ms untuk menahan spamming kueri saat kasir mengetik nomor transaksi.
     */
    val pagedTransaksi: Flow<PagingData<TransaksiEntity>> = _filterFlow
        .debounce(300L)
        .distinctUntilChanged()
        .flatMapLatest { filter ->
            transaksiRepo.getRiwayatPaged(filter)
        }
        .cachedIn(viewModelScope)

    // ---------- Intent: Filter & Pencarian ----------

    fun setQueryPencarian(query: String) {
        val updated = _uiState.value.filter.copy(query = query)
        _uiState.update { it.copy(filter = updated) }
        _filterFlow.value = updated
    }

    fun setFilterStatus(status: StatusTransaksi?) {
        val updated = _uiState.value.filter.copy(status = status)
        _uiState.update { it.copy(filter = updated) }
        _filterFlow.value = updated
    }

    fun setFilterRentangTanggal(startDate: Long?, endDate: Long?) {
        val updated = _uiState.value.filter.copy(startDate = startDate, endDate = endDate)
        _uiState.update { it.copy(filter = updated) }
        _filterFlow.value = updated
    }

    fun resetFilter() {
        val reset = TransaksiFilter()
        _uiState.update { it.copy(filter = reset) }
        _filterFlow.value = reset
    }

    // ---------- Intent: Detail Transaksi ----------

    fun bukaDetailTransaksi(transaksiId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(sedangMemuatDetail = true) }
            try {
                val detail = transaksiRepo.getDetailById(transaksiId)
                if (detail != null) {
                    _uiState.update { it.copy(detailTerpilih = detail, sedangMemuatDetail = false) }
                } else {
                    _uiState.update { it.copy(sedangMemuatDetail = false) }
                    _event.send(RiwayatEvent.Pesan("Data transaksi tidak ditemukan", RiwayatEvent.Pesan.Jenis.GALAT))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(sedangMemuatDetail = false) }
                _event.send(RiwayatEvent.Pesan(e.message ?: "Gagal memuat detail transaksi", RiwayatEvent.Pesan.Jenis.GALAT))
            }
        }
    }

    fun tutupDetailTransaksi() {
        _uiState.update { it.copy(detailTerpilih = null) }
    }

    // ---------- Intent: Cetak Ulang Struk (Reprint) ----------

    fun cetakUlangStruk(detail: TransaksiDenganDetail) {
        if (_uiState.value.sedangReprint) return

        viewModelScope.launch {
            _uiState.update { it.copy(sedangReprint = true) }
            try {
                val toko = profilRepo.get()
                // Format struk dengan parameter isReprint = true (menambahkan watermark / footer salinan)
                val receiptData = ReceiptFormatter.format(
                    toko = toko,
                    transaksi = detail.transaksi,
                    items = detail.items,
                    payments = detail.pembayaran,
                    isReprint = true,
                )

                printerService.enqueue(receiptData) { result ->
                    viewModelScope.launch {
                        _uiState.update { it.copy(sedangReprint = false) }
                        when (result) {
                            is PrintResult.Success -> {
                                _event.send(RiwayatEvent.ReprintSukses(detail.transaksi.nomorTransaksi))
                            }
                            is PrintResult.Failure -> {
                                _event.send(
                                    RiwayatEvent.Pesan(
                                        "Gagal mencetak salinan struk: ${result.message}",
                                        RiwayatEvent.Pesan.Jenis.GALAT,
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(sedangReprint = false) }
                _event.send(RiwayatEvent.Pesan(e.message ?: "Terjadi kesalahan cetak ulang", RiwayatEvent.Pesan.Jenis.GALAT))
            }
        }
    }

    // ---------- Intent: Ekspor Laporan Excel ----------

    fun eksporLaporanExcel() {
        if (_uiState.value.sedangEkspor) return

        viewModelScope.launch {
            _uiState.update { it.copy(sedangEkspor = true) }
            exportUseCase.exportToExcel(_uiState.value.filter).fold(
                onSuccess = { uri ->
                    _uiState.update { it.copy(sedangEkspor = false) }
                    _event.send(RiwayatEvent.FileSiapDibagikan(uri))
                },
                onFailure = { error ->
                    _uiState.update { it.copy(sedangEkspor = false) }
                    _event.send(
                        RiwayatEvent.Pesan(
                            error.message ?: "Gagal mengekspor laporan transaksi",
                            RiwayatEvent.Pesan.Jenis.GALAT,
                        )
                    )
                },
            )
        }
    }
}