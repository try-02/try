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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    private val queryDebounced = _query.debounce(300L)

    private val rawProdukFlow = combine(
        queryDebounced,
        _filterKategori,
        _filterStatus,
    ) { q, kat, status -> Triple(q, kat, status) }
        .flatMapLatest { (q, kat, status) ->
            val hanyaAktif = (status != StatusStokFilter.NONAKTIF && status != StatusStokFilter.SEMUA)
            productService.observeKatalogAdmin(
                query = q.trim(),
                kategori = kat,
                hanyaAktif = hanyaAktif,
            )
        }

    val uiState: StateFlow<KelolaProdukUiState> = combine(
        _query,
        _filterStatus,
        _filterKategori,
        _urutan,
        productService.observeKategori(),
        rawProdukFlow,
        _dialogPenyesuaianTarget,
        _kartuStokTarget,
    ) { q, status, kat, urut, categories, rawList, dialogTarget, kartuTarget ->
        val filtered = rawList.filter { raw ->
            val stok = raw.stokNormal ?: 0L
            when (status) {
                StatusStokFilter.SEMUA -> true
                StatusStokFilter.MENIPIS -> raw.produk.aktif && stok in 1..5_000L
                StatusStokFilter.HABIS -> raw.produk.aktif && stok <= 0L
                StatusStokFilter.NONAKTIF -> !raw.produk.aktif
            }
        }.map { it.toUi() }

        val sorted = when (urut) {
            UrutanProduk.NAMA_AZ -> filtered.sortedBy { it.nama.lowercase() }
            UrutanProduk.NAMA_ZA -> filtered.sortedByDescending { it.nama.lowercase() }
            UrutanProduk.STOK_TERENDAH -> filtered.sortedBy { it.stokNormalScaled }
            UrutanProduk.STOK_TERTINGGI -> filtered.sortedByDescending { it.stokNormalScaled }
            UrutanProduk.HARGA_TERMURAH -> filtered.sortedBy { it.harga }
            UrutanProduk.HARGA_TERMAHAL -> filtered.sortedByDescending { it.harga }
        }

        KelolaProdukUiState(
            query = q,
            filterStatus = status,
            filterKategori = kat,
            urutan = urut,
            daftarKategori = categories,
            daftarProduk = sorted,
            sedangMemuat = false,
            dialogPenyesuaianTarget = dialogTarget,
            kartuStokTarget = kartuTarget,
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