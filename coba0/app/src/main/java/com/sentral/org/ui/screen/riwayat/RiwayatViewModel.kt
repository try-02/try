package com.sentral.org.ui.screen.riwayat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import androidx.sqlite.SQLiteException
import com.sentral.org.data.dao.ReturDao
import com.sentral.org.data.entity.TransaksiDenganDetail
import com.sentral.org.data.entity.TransaksiEntity
import com.sentral.org.data.model.MetodePembayaran
import com.sentral.org.data.model.QUANTITY_SCALE
import com.sentral.org.data.model.StatusTransaksi
import com.sentral.org.data.model.TransaksiFilter
import com.sentral.org.data.model.TujuanStokPengembalian
import com.sentral.org.data.repository.ProfilTokoRepository
import com.sentral.org.data.repository.TransaksiRepository
import com.sentral.org.data.service.PrinterService
import com.sentral.org.data.service.ReturService
import com.sentral.org.data.service.VoidService
import com.sentral.org.data.session.ActiveSesiKasirProvider
import com.sentral.org.export.TransaksiExportUseCase
import com.sentral.org.shared.currentTimeMillis
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface RiwayatIntent {
    data class SetQueryPencarian(val query: String) : RiwayatIntent
    data class SetFilterStatus(val status: StatusTransaksi?) : RiwayatIntent
    object ResetFilter : RiwayatIntent
    data class BukaDetailTransaksi(val transaksiId: Long) : RiwayatIntent
    object TutupDetailTransaksi : RiwayatIntent
    object CetakUlangStruk : RiwayatIntent
    object EksporLaporanExcel : RiwayatIntent
    object BukaDialogVoid : RiwayatIntent
    object TutupDialogVoid : RiwayatIntent
    data class SetAlasanVoid(val alasan: String) : RiwayatIntent
    object KonfirmasiVoid : RiwayatIntent
    object BukaDialogRetur : RiwayatIntent
    object TutupDialogRetur : RiwayatIntent
    data class UbahQtyRetur(val itemId: Long, val deltaUnit: Long) : RiwayatIntent
    data class UbahTujuanRetur(val itemId: Long, val tujuan: TujuanStokPengembalian) : RiwayatIntent
    data class SetMetodeRefund(val metode: MetodePembayaran) : RiwayatIntent
    data class SetCatatanRetur(val catatan: String) : RiwayatIntent
    object KonfirmasiRetur : RiwayatIntent
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class RiwayatViewModel(
    private val transaksiRepo: TransaksiRepository,
    private val profilRepo: ProfilTokoRepository,
    private val printerService: PrinterService,
    private val exportUseCase: TransaksiExportUseCase,
    private val voidService: VoidService,
    private val returService: ReturService,
    private val returDao: ReturDao,
    private val sessionProvider: ActiveSesiKasirProvider,
) : ViewModel() {

    private var rawDetailTerpilih: TransaksiDenganDetail? = null

    private val _uiState = MutableStateFlow(RiwayatUiState())
    val uiState: StateFlow<RiwayatUiState> = _uiState.asStateFlow()

    private val _event = Channel<RiwayatEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    private val _filterFlow = MutableStateFlow(TransaksiFilter())

    val pagedTransaksi: Flow<PagingData<TransaksiItemUi>> = _filterFlow
        .debounce(300L)
        .distinctUntilChanged()
        .flatMapLatest { filter ->
            transaksiRepo.getRiwayatPaged(filter).map { pagingData: PagingData<TransaksiEntity> ->
                pagingData.map { it.toUiModel() }
            }
        }.cachedIn(viewModelScope)

    private val actions = RiwayatActions(
        transaksiRepo = transaksiRepo, profilRepo = profilRepo, printerService = printerService,
        exportUseCase = exportUseCase, voidService = voidService, returService = returService,
        returDao = returDao, sessionProvider = sessionProvider, scope = viewModelScope,
        sendEvent = { _event.send(it) }, getRawDetail = { rawDetailTerpilih },
        setRawDetail = { rawDetailTerpilih = it },
        updateUiState = { _uiState.update { it } },
        refreshFilter = { _filterFlow.value = _uiState.value.filter.copy(refreshEpoch = currentTimeMillis()) },
    )

    fun onIntent(intent: RiwayatIntent) = when (intent) {
        is RiwayatIntent.SetQueryPencarian -> { val updated = _uiState.value.filter.copy(query = intent.query); _uiState.update { it.copy(filter = updated) }; _filterFlow.value = updated }
        is RiwayatIntent.SetFilterStatus -> { val updated = _uiState.value.filter.copy(status = intent.status); _uiState.update { it.copy(filter = updated) }; _filterFlow.value = updated }
        RiwayatIntent.ResetFilter -> { val reset = TransaksiFilter(); _uiState.update { it.copy(filter = reset) }; _filterFlow.value = reset }
        is RiwayatIntent.BukaDetailTransaksi -> bukaDetailTransaksi(intent.transaksiId)
        RiwayatIntent.TutupDetailTransaksi -> { rawDetailTerpilih = null; _uiState.update { it.copy(detailTerpilih = null, dialogVoidTerbuka = false, dialogReturTerbuka = false) } }
        RiwayatIntent.CetakUlangStruk -> actions.cetakUlangStruk()
        RiwayatIntent.EksporLaporanExcel -> actions.eksporLaporanExcel(_uiState.value.filter)
        RiwayatIntent.BukaDialogVoid -> _uiState.update { it.copy(dialogVoidTerbuka = true, alasanVoidInput = "") }
        RiwayatIntent.TutupDialogVoid -> _uiState.update { it.copy(dialogVoidTerbuka = false, alasanVoidInput = "") }
        is RiwayatIntent.SetAlasanVoid -> _uiState.update { it.copy(alasanVoidInput = intent.alasan) }
        RiwayatIntent.KonfirmasiVoid -> actions.konfirmasiVoid(_uiState.value.alasanVoidInput.trim())
        RiwayatIntent.BukaDialogRetur -> actions.bukaDialogRetur()
        RiwayatIntent.TutupDialogRetur -> _uiState.update { it.copy(dialogReturTerbuka = false) }
        is RiwayatIntent.UbahQtyRetur -> _uiState.update { state ->
            val updated = state.daftarItemRetur.map { row ->
                if (row.itemId == intent.itemId) {
                    val currentUnit = row.qtyPilihanScaled / QUANTITY_SCALE
                    val maxUnit = row.sisaQtyScaled / QUANTITY_SCALE
                    val newUnit = (currentUnit + intent.deltaUnit).coerceIn(0L, maxUnit)
                    row.copy(qtyPilihanScaled = newUnit * QUANTITY_SCALE)
                } else row
            }
            state.copy(daftarItemRetur = updated)
        }
        is RiwayatIntent.UbahTujuanRetur -> _uiState.update { state ->
            val updated = state.daftarItemRetur.map { row -> if (row.itemId == intent.itemId) row.copy(tujuan = intent.tujuan) else row }
            state.copy(daftarItemRetur = updated)
        }
        is RiwayatIntent.SetMetodeRefund -> _uiState.update { it.copy(metodeRefundRetur = intent.metode) }
        is RiwayatIntent.SetCatatanRetur -> _uiState.update { it.copy(catatanReturInput = intent.catatan) }
        RiwayatIntent.KonfirmasiRetur -> actions.konfirmasiRetur(
            returItems = _uiState.value.daftarItemRetur.filter { it.qtyPilihanScaled > 0 },
            metodeRefund = _uiState.value.metodeRefundRetur,
            catatan = _uiState.value.catatanReturInput.trim(),
            detailItems = rawDetailTerpilih?.items ?: emptyList(),
        )
    }

    private fun bukaDetailTransaksi(transaksiId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(sedangMemuatDetail = true) }
            try {
                val detail = transaksiRepo.getDetailById(transaksiId)
                if (detail != null) { rawDetailTerpilih = detail; _uiState.update { it.copy(detailTerpilih = detail.toUiModel(), sedangMemuatDetail = false) } }
                else { _uiState.update { it.copy(sedangMemuatDetail = false) }; _event.send(RiwayatEvent.Pesan("Data transaksi tidak ditemukan", RiwayatEvent.Pesan.Jenis.GALAT)) }
            } catch (e: SQLiteException) { _uiState.update { it.copy(sedangMemuatDetail = false) }; _event.send(RiwayatEvent.Pesan(e.message ?: "Gagal memuat detail", RiwayatEvent.Pesan.Jenis.GALAT)) }
        }
    }
}