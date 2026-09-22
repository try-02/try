package com.sentral.org.ui.screen.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentral.org.data.dao.ProdukDao
import com.sentral.org.data.model.QUANTITY_SCALE
import com.sentral.org.data.model.formatQuantity
import com.sentral.org.data.session.ActiveSesiKasirProvider
import com.sentral.org.domain.model.CreateProductRequest
import com.sentral.org.domain.model.UpdateProductRequest
import com.sentral.org.domain.service.ProductManagementService
import com.sentral.org.shared.currentTimeMillis
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FormProdukViewModel(
    savedStateHandle: SavedStateHandle,
    private val productService: ProductManagementService,
    private val produkDao: ProdukDao,
    private val sessionProvider: ActiveSesiKasirProvider,
) : ViewModel() {
    private val targetProdukId: Long? = savedStateHandle.get<Long>("produkId")?.takeIf { it > 0 }

    private val _uiState =
        MutableStateFlow(
            FormProdukUiState(
                produkId = targetProdukId,
                isEditMode = targetProdukId != null,
            ),
        )
    val uiState: StateFlow<FormProdukUiState> = _uiState.asStateFlow()

    private val _event = Channel<FormProdukEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    init {
        muatDataAwal()
    }

    private fun muatDataAwal() {
        viewModelScope.launch {
            val kategoriList = produkDao.observeSemuaKategori().first()
            _uiState.update { it.copy(kategoriTersedia = kategoriList) }

            if (targetProdukId != null) {
                val p = produkDao.getById(targetProdukId)
                if (p != null) {
                    _uiState.update {
                        it.copy(
                            nama = p.nama,
                            sku = p.sku,
                            barcode = p.barcode.orEmpty(),
                            hargaJualInput = p.harga.toString(),
                            hargaModalInput = p.hargaModal.toString(),
                            kategori = p.kategori,
                        )
                    }
                }
            }
        }
    }

    fun setNama(v: String) {
        _uiState.update { it.copy(nama = v, pesanError = null) }
    }

    fun setSku(v: String) {
        _uiState.update { it.copy(sku = v, pesanError = null) }
    }

    fun setBarcode(v: String) {
        _uiState.update { it.copy(barcode = v, pesanError = null) }
    }

    fun setHargaJual(v: String) {
        _uiState.update { it.copy(hargaJualInput = v.filter { c -> c.isDigit() }, pesanError = null) }
    }

    fun setHargaModal(v: String) {
        _uiState.update { it.copy(hargaModalInput = v.filter { c -> c.isDigit() }, pesanError = null) }
    }

    fun setKategori(v: String) {
        _uiState.update { it.copy(kategori = v, pesanError = null) }
    }

    fun setStokAwal(v: String) {
        if (v.isEmpty() || v.matches(Regex("^\\d*([.,]\\d{0,3})?$"))) {
            _uiState.update { it.copy(stokAwalInput = v, pesanError = null) }
        }
    }

    fun bukaScanner() {
        _uiState.update { it.copy(scannerTerbuka = true) }
    }

    fun tutupScanner() {
        _uiState.update { it.copy(scannerTerbuka = false) }
    }

    fun onBarcodeHasilScan(barcode: String) {
        _uiState.update { it.copy(barcode = barcode.trim(), scannerTerbuka = false) }
    }

    fun generateSkuOtomatis() {
        val stamp = SimpleDateFormat("yyMMdd", Locale.ROOT).format(Date())
        val randomSuffix = (1000..9999).random()
        _uiState.update { it.copy(sku = "PRD-$stamp-$randomSuffix", pesanError = null) }
    }

    fun simpanProduk() {
        val s = _uiState.value
        val namaClean = s.nama.trim()
        val skuClean = s.sku.trim()
        val hargaJual = s.hargaJualInput.toLongOrNull() ?: 0L
        val hargaModal = s.hargaModalInput.toLongOrNull() ?: 0L

        if (namaClean.isBlank()) {
            _uiState.update { it.copy(pesanError = "Nama produk tidak boleh kosong") }
            return
        }
        if (skuClean.isBlank()) {
            _uiState.update { it.copy(pesanError = "SKU wajib diisi") }
            return
        }

        val stokAwalNum = s.stokAwalInput.replace(',', '.').toDoubleOrNull() ?: 0.0
        val stokAwalScaled = (stokAwalNum * QUANTITY_SCALE).toLong()

        viewModelScope.launch {
            _uiState.update { it.copy(sedangMenyimpan = true) }
            val now = currentTimeMillis()
            val sesi = sessionProvider.sesiAktif()

            val result =
                if (s.isEditMode && targetProdukId != null) {
                    productService.updateProduct(
                        UpdateProductRequest(
                            id = targetProdukId,
                            nama = namaClean,
                            sku = skuClean,
                            barcode = s.barcode.trim().ifBlank { null },
                            harga = hargaJual,
                            hargaModal = hargaModal,
                            kategori = s.kategori.trim(),
                        ),
                        now,
                    )
                } else {
                    productService
                        .createProduct(
                            CreateProductRequest(
                                nama = namaClean,
                                sku = skuClean,
                                barcode = s.barcode.trim().ifBlank { null },
                                harga = hargaJual,
                                hargaModal = hargaModal,
                                kategori = s.kategori.trim(),
                                stokAwalScaled = stokAwalScaled,
                                operator = sesi?.namaKasir ?: "Admin",
                                shiftId = sesi?.shiftId,
                            ),
                            now,
                        ).map { }
                }

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(sedangMenyimpan = false) }
                    _event.send(FormProdukEvent.SimpanSukses)
                },
                onFailure = { err ->
                    _uiState.update { it.copy(sedangMenyimpan = false, pesanError = err.message ?: "Gagal menyimpan") }
                },
            )
        }
    }
}
