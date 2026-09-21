package com.sentral.org.ui.screen.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentral.org.data.dao.ProdukDenganStokRaw
import com.sentral.org.data.session.ActiveSesiKasirProvider
import com.sentral.org.domain.model.DisposeDamageRequest
import com.sentral.org.domain.model.RecordDamageRequest
import com.sentral.org.domain.model.RestockRequest
import com.sentral.org.domain.model.StockOpnameRequest
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class FilterConfig(
    val query: String,
    val status: StatusStokFilter,
    val kategori: String?,
    val urutan: UrutanProduk,
)

private data class DialogTargets(
    val penyesuaian: ProdukItemAdminUi?,
    val kartuStok: ProdukItemAdminUi?,
)

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

    // Sub-Grup 1: Konfigurasi Filter & Urutan (4 Flow <= 5)
    private val filterConfigFlow = combine(
        _query.debounce(300L),
        _filterStatus,
        _filterKategori,
        _urutan,
    ) { q, status, kat, urut ->
        FilterConfig(query = q, status = status, kategori = kat, urutan = urut)
    }

    // Sub-Grup 2: State Dialog (2 Flow <= 5)
    private val dialogTargetsFlow = combine(
        _dialogPenyesuaianTarget,
        _kartuStokTarget,
    ) { penyesuaian, kartuStok ->
        DialogTargets(penyesuaian = penyesuaian, kartuStok = kartuStok)
    }

    // Sub-Grup 3: Stream Produk Reaktif Terfilter & Terurut
    private val produkListFlow = filterConfigFlow.flatMapLatest { config ->
        val hanyaAktif = (config.status != StatusStokFilter.NONAKTIF && config.status != StatusStokFilter.SEMUA)
        productService.observeKatalogAdmin(
            query = config.query.trim(),
            kategori = config.kategori,
            hanyaAktif = hanyaAktif,
        ).map { rawList ->
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

    // Kombinasi Akhir Type-Safe (4 Flow <= 5)
    val uiState: StateFlow<KelolaProdukUiState> = combine(
        filterConfigFlow,
        productService.observeKategori(),
        produkListFlow,
        dialogTargetsFlow,
    ) { config, categories, produkList, dialogs ->
        KelolaProdukUiState(
            query = config.query,
            filterStatus = config.status,
            filterKategori = config.kategori,
            urutan = config.urutan,
            daftarKategori = categories,
            daftarProduk = produkList,
            sedangMemuat = false,
            dialogPenyesuaianTarget = dialogs.penyesuaian,
            kartuStokTarget = dialogs.kartuStok,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KelolaProdukUiState())

    fun setQuery(q: String) { _query.value = q }
    fun setFilterStatus(s: StatusStokFilter) { _filterStatus.value = s }
    fun setFilterKategori(k: String?) { _filterKategori.value = k }
    fun setUrutan(u: UrutanProduk) { _urutan.value = u }

    fun bukaDialogPenyesuaian(item: ProdukItemAdminUi) { _dialogPenyesuaianTarget.value = item }
    fun tutupDialogPenyesuaian() { _dialogPenyesuaianTarget.value = null }

    fun bukaKartuStok(item: ProdukItemAdminUi) { _kartuStokTarget.value = item }
    fun tutupKartuStok() { _kartuStokTarget.value = null }

    fun toggleAktifkanProduk(item: ProdukItemAdminUi) {
        viewModelScope.launch {
            val now = currentTimeMillis()
            val result = if (item.aktif) {
                productService.deactivateProduct(item.id, now)
            } else {
                productService.activateProduct(item.id, now)
            }
            result.fold(
                onSuccess = {
                    val statusStr = if (item.aktif) "dinonaktifkan" else "diaktifkan"
                    _event.send(KelolaProdukEvent.Pesan("Produk '${item.nama}' berhasil $statusStr"))
                },
                onFailure = { err ->
                    _event.send(KelolaProdukEvent.Pesan(err.message ?: "Gagal mengubah status", isError = true))
                }
            )
        }
    }

    fun submitRestock(produkId: Long, jumlahScaled: Long, alasan: String) {
        viewModelScope.launch {
            val s = sessionProvider.sesiAktif()
            productService.restock(
                RestockRequest(
                    produkId = produkId,
                    jumlahScaled = jumlahScaled,
                    alasan = alasan,
                    operator = s?.namaKasir ?: "Admin",
                    shiftId = s?.shiftId,
                ),
                currentTimeMillis(),
            ).fold(
                onSuccess = {
                    tutupDialogPenyesuaian()
                    _event.send(KelolaProdukEvent.Pesan("Stok berhasil ditambahkan"))
                },
                onFailure = { err ->
                    _event.send(KelolaProdukEvent.Pesan(err.message ?: "Gagal restock", isError = true))
                }
            )
        }
    }

    fun submitOpname(produkId: Long, stokFisikScaled: Long, alasan: String) {
        viewModelScope.launch {
            val s = sessionProvider.sesiAktif()
            productService.adjustStockOpname(
                StockOpnameRequest(
                    produkId = produkId,
                    stokFisikScaled = stokFisikScaled,
                    alasan = alasan,
                    operator = s?.namaKasir ?: "Admin",
                    shiftId = s?.shiftId,
                ),
                currentTimeMillis(),
            ).fold(
                onSuccess = {
                    tutupDialogPenyesuaian()
                    _event.send(KelolaProdukEvent.Pesan("Stock opname berhasil disesuaikan"))
                },
                onFailure = { err ->
                    _event.send(KelolaProdukEvent.Pesan(err.message ?: "Gagal opname", isError = true))
                }
            )
        }
    }

    fun submitBarangRusak(produkId: Long, jumlahScaled: Long, alasan: String) {
        viewModelScope.launch {
            val s = sessionProvider.sesiAktif()
            productService.recordDamage(
                RecordDamageRequest(
                    produkId = produkId,
                    jumlahScaled = jumlahScaled,
                    alasan = alasan,
                    operator = s?.namaKasir ?: "Admin",
                    shiftId = s?.shiftId,
                ),
                currentTimeMillis(),
            ).fold(
                onSuccess = {
                    tutupDialogPenyesuaian()
                    _event.send(KelolaProdukEvent.Pesan("Barang rusak berhasil dicatat"))
                },
                onFailure = { err ->
                    _event.send(KelolaProdukEvent.Pesan(err.message ?: "Gagal mencatat rusak", isError = true))
                }
            )
        }
    }

    fun submitPemusnahan(produkId: Long, jumlahScaled: Long, alasan: String) {
        viewModelScope.launch {
            val s = sessionProvider.sesiAktif()
            productService.disposeDamage(
                DisposeDamageRequest(
                    produkId = produkId,
                    jumlahScaled = jumlahScaled,
                    alasan = alasan,
                    operator = s?.namaKasir ?: "Admin",
                    shiftId = s?.shiftId,
                ),
                currentTimeMillis(),
            ).fold(
                onSuccess = {
                    tutupDialogPenyesuaian()
                    _event.send(KelolaProdukEvent.Pesan("Pemusnahan barang rusak berhasil dicatat"))
                },
                onFailure = { err ->
                    _event.send(KelolaProdukEvent.Pesan(err.message ?: "Gagal pemusnahan", isError = true))
                }
            )
        }
    }

    private fun ProdukDenganStokRaw.toUi() = ProdukItemAdminUi(
        id = produk.id,
        nama = produk.nama,
        sku = produk.sku,
        barcode = produk.barcode,
        harga = produk.harga,
        hargaModal = produk.hargaModal,
        kategori = produk.kategori,
        stokNormalScaled = stokNormal ?: 0L,
        stokRusakScaled = stokRusak ?: 0L,
        aktif = produk.aktif,
    )
}