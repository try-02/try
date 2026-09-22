package com.sentral.org.ui.screen.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentral.org.data.dao.ProdukDenganStokRaw
import com.sentral.org.data.entity.PergerakanPersediaanEntity
import com.sentral.org.data.session.ActiveSesiKasirProvider
import com.sentral.org.domain.model.KartuStokFilter
import com.sentral.org.domain.service.ProductManagementService
import com.sentral.org.shared.currentTimeMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ActiveSelections(
    val query: String,
    val status: StatusStokFilter,
    val kategori: String?,
    val urutan: UrutanProduk,
)

private data class DialogState(
    val penyesuaian: ProdukItemAdminUi?,
    val kartuStokTarget: ProdukItemAdminUi?,
    val kartuStokList: List<KartuStokItemUi>,
)

sealed interface KelolaProdukIntent {
    data class SetQuery(val q: String) : KelolaProdukIntent
    data class SetFilterStatus(val s: StatusStokFilter) : KelolaProdukIntent
    data class SetFilterKategori(val k: String?) : KelolaProdukIntent
    data class SetUrutan(val u: UrutanProduk) : KelolaProdukIntent
    data class BukaDialogPenyesuaian(val item: ProdukItemAdminUi) : KelolaProdukIntent
    object TutupDialogPenyesuaian : KelolaProdukIntent
    data class BukaKartuStok(val item: ProdukItemAdminUi) : KelolaProdukIntent
    object TutupKartuStok : KelolaProdukIntent
    data class ToggleAktifkanProduk(val item: ProdukItemAdminUi) : KelolaProdukIntent
    data class SubmitRestock(val produkId: Long, val jumlahScaled: Long, val alasan: String) : KelolaProdukIntent
    data class SubmitOpname(val produkId: Long, val stokFisikScaled: Long, val alasan: String) : KelolaProdukIntent
    data class SubmitBarangRusak(val produkId: Long, val jumlahScaled: Long, val alasan: String) : KelolaProdukIntent
    data class SubmitPemusnahan(val produkId: Long, val jumlahScaled: Long, val alasan: String) : KelolaProdukIntent
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class KelolaProdukViewModel(
    private val productService: ProductManagementService,
    private val sessionProvider: ActiveSesiKasirProvider,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _filterStatus = MutableStateFlow(StatusStokFilter.SEMUA)
    private val _filterKategori = MutableStateFlow<String?>(null)
    private val _urutan = MutableStateFlow(UrutanProduk.NAMA_AZ)

    private val _dialogPenyesuaianTarget = MutableStateFlow<ProdukItemAdminUi?>(null)
    private val _kartuStokTarget = MutableStateFlow<ProdukItemAdminUi?>(null)

    private val _event = Channel<KelolaProdukEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    private val activeSelectionsFlow = combine(_query, _filterStatus, _filterKategori, _urutan) { q, status, kat, urut ->
        ActiveSelections(query = q, status = status, kategori = kat, urutan = urut) }

    private val debouncedSearchConfigFlow = combine(_query.debounce(300L), _filterStatus, _filterKategori, _urutan) { q, status, kat, urut ->
        ActiveSelections(query = q, status = status, kategori = kat, urutan = urut) }

    private val kartuStokLedgerFlow = _kartuStokTarget.flatMapLatest { target ->
        if (target == null) flowOf(emptyList())
        else productService.observeKartuStok(KartuStokFilter(produkId = target.id)).map { it.map { it.toUi(dateFormat) } }
    }

    private val dialogStateFlow = combine(_dialogPenyesuaianTarget, _kartuStokTarget, kartuStokLedgerFlow) { penyesuaian, kartuTarget, ledgerList ->
        DialogState(penyesuaian = penyesuaian, kartuStokTarget = kartuTarget, kartuStokList = ledgerList) }

    private val produkListFlow = debouncedSearchConfigFlow.flatMapLatest { config ->
        val hanyaAktif = (config.status != StatusStokFilter.NONAKTIF && config.status != StatusStokFilter.SEMUA)
        productService.observeKatalogAdmin(query = config.query.trim(), kategori = config.kategori, hanyaAktif = hanyaAktif).map { rawList ->
            val filtered = rawList.filter { raw ->
                val stok = raw.stokNormal ?: 0L
                when (config.status) {
                    StatusStokFilter.SEMUA -> true
                    StatusStokFilter.MENIPIS -> raw.produk.aktif && stok in 1..5_000L
                    StatusStokFilter.HABIS -> raw.produk.aktif && stok <= 0L
                    StatusStokFilter.NONAKTIF -> !raw.produk.aktif
                }
            }.map { it.toUi() }
            when (config.urutan) {
                UrutanProduk.NAMA_AZ -> filtered.sortedBy { it.nama.lowercase() }
                UrutanProduk.NAMA_ZA -> filtered.sortedByDescending { it.nama.lowercase() }
                UrutanProduk.STOK_TERENDAH -> filtered.sortedBy { it.stokNormalScaled }
                UrutanProduk.STOK_TERTINGGI -> filtered.sortedByDescending { it.stokNormalScaled }
                UrutanProduk.HARGA_TERMURAH -> filtered.sortedBy { it.harga }
                UrutanProduk.HARGA_TERMAHAL -> filtered.sortedByDescending { it.harga }
            }
        }
    }

    val uiState: StateFlow<KelolaProdukUiState> = combine(
        activeSelectionsFlow, productService.observeKategori(), produkListFlow, dialogStateFlow
    ) { selections, categories, produkList, dialogs ->
        KelolaProdukUiState(
            query = selections.query, filterStatus = selections.status, filterKategori = selections.kategori,
            urutan = selections.urutan, daftarKategori = categories, daftarProduk = produkList,
            sedangMemuat = false, dialogPenyesuaianTarget = dialogs.penyesuaian,
            kartuStokTarget = dialogs.kartuStokTarget, kartuStokList = dialogs.kartuStokList,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KelolaProdukUiState())

    private val actions = KelolaProdukActions(
        productService = productService, sessionProvider = sessionProvider, scope = viewModelScope,
        sendEvent = { _event.send(it) },
        dismissDialogs = { _dialogPenyesuaianTarget.value = null; _kartuStokTarget.value = null },
    )

    fun onIntent(intent: KelolaProdukIntent) = when (intent) {
        is KelolaProdukIntent.SetQuery -> _query.value = intent.q
        is KelolaProdukIntent.SetFilterStatus -> _filterStatus.value = intent.s
        is KelolaProdukIntent.SetFilterKategori -> _filterKategori.value = intent.k
        is KelolaProdukIntent.SetUrutan -> _urutan.value = intent.u
        is KelolaProdukIntent.BukaDialogPenyesuaian -> _dialogPenyesuaianTarget.value = intent.item
        KelolaProdukIntent.TutupDialogPenyesuaian -> _dialogPenyesuaianTarget.value = null
        is KelolaProdukIntent.BukaKartuStok -> _kartuStokTarget.value = intent.item
        KelolaProdukIntent.TutupKartuStok -> _kartuStokTarget.value = null
        is KelolaProdukIntent.ToggleAktifkanProduk -> actions.toggleAktifkanProduk(intent.item)
        is KelolaProdukIntent.SubmitRestock -> actions.submitRestock(intent.produkId, intent.jumlahScaled, intent.alasan)
        is KelolaProdukIntent.SubmitOpname -> actions.submitOpname(intent.produkId, intent.stokFisikScaled, intent.alasan)
        is KelolaProdukIntent.SubmitBarangRusak -> actions.submitBarangRusak(intent.produkId, intent.jumlahScaled, intent.alasan)
        is KelolaProdukIntent.SubmitPemusnahan -> actions.submitPemusnahan(intent.produkId, intent.jumlahScaled, intent.alasan)
    }

    private fun ProdukDenganStokRaw.toUi() = ProdukItemAdminUi(
        id = produk.id, nama = produk.nama, sku = produk.sku, barcode = produk.barcode,
        harga = produk.harga, hargaModal = produk.hargaModal, kategori = produk.kategori,
        stokNormalScaled = stokNormal ?: 0L, stokRusakScaled = stokRusak ?: 0L, aktif = produk.aktif,
    )

    private fun PergerakanPersediaanEntity.toUi(format: SimpleDateFormat) = KartuStokItemUi(
        id = id, waktuFormatted = format.format(Date(dibuatPada)), jenis = jenis,
        perubahanJumlah = perubahanJumlah, perubahanJumlahRusak = perubahanJumlahRusak,
        saldoJumlahSebelum = saldoJumlahSebelum, saldoJumlahSetelah = saldoJumlahSetelah, keterangan = keterangan,
    )
}