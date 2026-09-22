package com.sentral.org.ui.screen.riwayat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.sentral.org.data.dao.ReturDao
import com.sentral.org.data.entity.TransaksiDenganDetail
import com.sentral.org.data.entity.TransaksiEntity
import com.sentral.org.data.model.MetodePembayaran
import com.sentral.org.data.model.PrintResult
import com.sentral.org.data.model.QUANTITY_SCALE
import com.sentral.org.data.model.ReturnLineRequest
import com.sentral.org.data.model.ReturnRequest
import com.sentral.org.data.model.StatusTransaksi
import com.sentral.org.data.model.TransaksiFilter
import com.sentral.org.data.model.TujuanStokPengembalian
import com.sentral.org.data.model.VoidRequest
import com.sentral.org.data.repository.ProfilTokoRepository
import com.sentral.org.data.repository.TransaksiRepository
import com.sentral.org.data.service.PrinterService
import com.sentral.org.data.service.ReceiptFormatter
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
import androidx.sqlite.SQLiteException

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
                    rawDetailTerpilih = detail
                    _uiState.update { it.copy(detailTerpilih = detail.toUiModel(), sedangMemuatDetail = false) }
                } else {
                    _uiState.update { it.copy(sedangMemuatDetail = false) }
                    _event.send(RiwayatEvent.Pesan("Data transaksi tidak ditemukan", RiwayatEvent.Pesan.Jenis.GALAT))
                }
            } catch (e: androidx.sqlite.SQLiteException) {
                _uiState.update { it.copy(sedangMemuatDetail = false) }
                _event.send(RiwayatEvent.Pesan(e.message ?: "Gagal memuat detail", RiwayatEvent.Pesan.Jenis.GALAT))
            }
        }
    }

    fun tutupDetailTransaksi() {
        rawDetailTerpilih = null
        _uiState.update { it.copy(detailTerpilih = null, dialogVoidTerbuka = false, dialogReturTerbuka = false) }
    }

    // ---------- Intent: Cetak Ulang Struk ----------

    fun cetakUlangStruk() {
        val detail = rawDetailTerpilih ?: return
        if (_uiState.value.sedangReprint) return

        viewModelScope.launch {
            _uiState.update { it.copy(sedangReprint = true) }
            try {
                val toko = profilRepo.get()
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
                                _event.send(RiwayatEvent.Pesan("Gagal cetak salinan: ${result.message}", RiwayatEvent.Pesan.Jenis.GALAT))
                            }
                        }
                    }
                }
            } catch (e: androidx.sqlite.SQLiteException) {
                _uiState.update { it.copy(sedangReprint = false) }
                _event.send(RiwayatEvent.Pesan(e.message ?: "Gagal mengambil data untuk cetak ulang", RiwayatEvent.Pesan.Jenis.GALAT))
            }
        }
    }

    // ---------- Intent: Ekspor Excel ----------

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
                    _event.send(RiwayatEvent.Pesan(error.message ?: "Gagal ekspor laporan", RiwayatEvent.Pesan.Jenis.GALAT))
                },
            )
        }
    }

    // ---------- Intent: Pembatalan Transaksi (Void) ----------

    fun bukaDialogVoid() {
        _uiState.update { it.copy(dialogVoidTerbuka = true, alasanVoidInput = "") }
    }

    fun tutupDialogVoid() {
        _uiState.update { it.copy(dialogVoidTerbuka = false, alasanVoidInput = "") }
    }

    fun setAlasanVoid(alasan: String) {
        _uiState.update { it.copy(alasanVoidInput = alasan) }
    }

    fun konfirmasiVoid() {
        val detail = rawDetailTerpilih ?: return
        val alasan = _uiState.value.alasanVoidInput.trim()
        if (alasan.isBlank()) {
            viewModelScope.launch { _event.send(RiwayatEvent.Pesan("Alasan pembatalan wajib diisi", RiwayatEvent.Pesan.Jenis.GALAT)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(sedangMemprosesAksi = true) }
            val now = currentTimeMillis()
            val sesi = sessionProvider.sesiAktif()

            val request = VoidRequest(
                transactionId = detail.transaksi.id,
                cashierId = sesi?.kasirId ?: detail.transaksi.kasirId,
                shiftId = sesi?.shiftId,
                reason = alasan,
                now = now,
            )

            voidService.void(request).fold(
                onSuccess = {
                    // Cetak struk bukti Void
                    try {
                        val toko = profilRepo.get()
                        val receiptData = ReceiptFormatter.formatVoid(
                            toko = toko,
                            transaksi = detail.transaksi,
                            items = detail.items,
                            kasirPelaksana = sesi?.namaKasir ?: detail.transaksi.namaKasir,
                            alasan = alasan,
                            waktuVoid = now,
                        )
                        printerService.enqueue(receiptData)
                    } catch (_: Exception) {}

                    // Refresh detail transaksi
                    val updatedDetail = transaksiRepo.getDetailById(detail.transaksi.id)
                    rawDetailTerpilih = updatedDetail
                    _uiState.update {
                        it.copy(
                            sedangMemprosesAksi = false,
                            dialogVoidTerbuka = false,
                            detailTerpilih = updatedDetail?.toUiModel(),
                        )
                    }
                    val refreshFilter = _uiState.value.filter.copy(refreshEpoch = currentTimeMillis())
                    _uiState.update { it.copy(filter = refreshFilter) }
                    _filterFlow.value = refreshFilter
                    _event.send(RiwayatEvent.VoidSukses(detail.transaksi.nomorTransaksi))
                },
                onFailure = { error ->
                    _uiState.update { it.copy(sedangMemprosesAksi = false) }
                    _event.send(RiwayatEvent.Pesan(error.message ?: "Gagal membatalkan transaksi", RiwayatEvent.Pesan.Jenis.GALAT))
                },
            )
        }
    }

    // ---------- Intent: Pengembalian Barang (Retur) ----------

    fun bukaDialogRetur() {
        val detail = rawDetailTerpilih ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(sedangMemprosesAksi = true) }
            val returRows = detail.items.map { item ->
                val alreadyReturned = returDao.getReturnedQuantity(item.id)
                val remaining = (item.jumlah - alreadyReturned).coerceAtLeast(0L)
                ItemReturUi(
                    itemId = item.id,
                    namaProduk = item.namaProduk,
                    sisaQtyScaled = remaining,
                    qtyPilihanScaled = 0L,
                    tujuan = TujuanStokPengembalian.NORMAL,
                )
            }
            _uiState.update {
                it.copy(
                    sedangMemprosesAksi = false,
                    dialogReturTerbuka = true,
                    daftarItemRetur = returRows,
                    catatanReturInput = "",
                )
            }
        }
    }

    fun tutupDialogRetur() {
        _uiState.update { it.copy(dialogReturTerbuka = false) }
    }

    fun ubahQtyRetur(itemId: Long, deltaUnit: Long) {
        _uiState.update { state ->
            val updated = state.daftarItemRetur.map { row ->
                if (row.itemId == itemId) {
                    val currentUnit = row.qtyPilihanScaled / QUANTITY_SCALE
                    val maxUnit = row.sisaQtyScaled / QUANTITY_SCALE
                    val newUnit = (currentUnit + deltaUnit).coerceIn(0L, maxUnit)
                    row.copy(qtyPilihanScaled = newUnit * QUANTITY_SCALE)
                } else row
            }
            state.copy(daftarItemRetur = updated)
        }
    }

    fun ubahTujuanRetur(itemId: Long, tujuan: TujuanStokPengembalian) {
        _uiState.update { state ->
            val updated = state.daftarItemRetur.map { row ->
                if (row.itemId == itemId) row.copy(tujuan = tujuan) else row
            }
            state.copy(daftarItemRetur = updated)
        }
    }

    fun setMetodeRefund(metode: MetodePembayaran) {
        _uiState.update { it.copy(metodeRefundRetur = metode) }
    }

    fun setCatatanRetur(catatan: String) {
        _uiState.update { it.copy(catatanReturInput = catatan) }
    }

    fun konfirmasiRetur() {
        val detail = rawDetailTerpilih ?: return
        val returItems = _uiState.value.daftarItemRetur.filter { it.qtyPilihanScaled > 0 }

        if (returItems.isEmpty()) {
            viewModelScope.launch { _event.send(RiwayatEvent.Pesan("Pilih minimal 1 barang untuk diretur", RiwayatEvent.Pesan.Jenis.GALAT)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(sedangMemprosesAksi = true) }
            val now = currentTimeMillis()
            val sesi = sessionProvider.sesiAktif()

            val lines = returItems.map {
                ReturnLineRequest(
                    transactionItemId = it.itemId,
                    quantity = it.qtyPilihanScaled,
                    destination = it.tujuan,
                )
            }

            val request = ReturnRequest(
                transactionId = detail.transaksi.id,
                cashierId = sesi?.kasirId ?: detail.transaksi.kasirId,
                shiftId = sesi?.shiftId,
                lines = lines,
                refundMethod = _uiState.value.metodeRefundRetur,
                note = _uiState.value.catatanReturInput.trim(),
                now = now,
            )

            returService.process(request).fold(
                onSuccess = { result ->
                    // Cetak struk bukti Retur
                    try {
                        val toko = profilRepo.get()
                        val itemMap = detail.items.associateBy { it.id }
                        val printItems = returItems.mapNotNull { returItem ->
                            itemMap[returItem.itemId]?.let { it to returItem.qtyPilihanScaled }
                        }
                        val receiptData = ReceiptFormatter.formatReturn(
                            toko = toko,
                            transaksi = detail.transaksi,
                            itemsRetur = printItems,
                            totalRefund = result.refundAmount,
                            metodeRefund = _uiState.value.metodeRefundRetur,
                            kasirPelaksana = sesi?.namaKasir ?: detail.transaksi.namaKasir,
                            waktuRetur = now,
                        )
                        printerService.enqueue(receiptData)
                    } catch (_: Exception) {}

                    _uiState.update {
                        it.copy(
                            sedangMemprosesAksi = false,
                            dialogReturTerbuka = false,
                        )
                    }
                    _filterFlow.value = _uiState.value.filter
                    _event.send(RiwayatEvent.ReturSukses(detail.transaksi.nomorTransaksi, result.refundAmount))
                },
                onFailure = { error ->
                    _uiState.update { it.copy(sedangMemprosesAksi = false) }
                    _event.send(RiwayatEvent.Pesan(error.message ?: "Gagal memproses retur", RiwayatEvent.Pesan.Jenis.GALAT))
                },
            )
        }
    }
}