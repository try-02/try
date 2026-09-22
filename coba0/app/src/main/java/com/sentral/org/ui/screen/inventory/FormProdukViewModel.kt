package com.sentral.org.ui.screen.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentral.org.data.dao.ProdukDao
import com.sentral.org.data.model.QUANTITY_SCALE
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

sealed interface FormProdukIntent {
    data class SetNama(val v: String) : FormProdukIntent
    data class SetSku(val v: String) : FormProdukIntent
    data class SetBarcode(val v: String) : FormProdukIntent
    data class SetHargaJual(val v: String) : FormProdukIntent
    data class SetHargaModal(val v: String) : FormProdukIntent
    data class SetKategori(val v: String) : FormProdukIntent
    data class SetStokAwal(val v: String) : FormProdukIntent
    object BukaScanner : FormProdukIntent
    object TutupScanner : FormProdukIntent
    data class OnBarcodeHasilScan(val barcode: String) : FormProdukIntent
    object GenerateSkuOtomatis : FormProdukIntent
    object SimpanProduk : FormProdukIntent
}

class FormProdukViewModel(
    savedStateHandle: SavedStateHandle,
    private val productService: ProductManagementService,
    private val produkDao: ProdukDao,
    private val sessionProvider: ActiveSesiKasirProvider,
) : ViewModel() {

    private val targetProdukId: Long? = savedStateHandle.get<Long>("produkId")?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(
        FormProdukUiState(
            produkId = targetProdukId,
            isEditMode = targetProdukId != null,
        ),
    )
    val uiState: StateFlow<FormProdukUiState> = _uiState.asStateFlow()

    private val _event = Channel<FormProdukEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    init { loadInitialData() }

    private fun loadInitialData() {
        viewModelScope.launch {
            val kategoriList = produkDao.observeSemuaKategori().first()
            _uiState.update { it.copy(kategoriTersedia = kategoriList) }

            targetProdukId?.let { id ->
                val p = produkDao.getById(id)
                p?.let {
                    _uiState.update {
                        it.copy(
                            nama = it.nama,
                            sku = it.sku,
                            barcode = it.barcode.orEmpty(),
                            hargaJualInput = it.harga.toString(),
                            hargaModalInput = it.hargaModal.toString(),
                            kategori = it.kategori,
                        )
                    }
                }
            }
        }
    }

    fun onIntent(intent: FormProdukIntent) = when (intent) {
        is FormProdukIntent.SetNama -> _uiState.update { it.copy(nama = intent.v, pesanError = null) }
        is FormProdukIntent.SetSku -> _uiState.update { it.copy(sku = intent.v, pesanError = null) }
        is FormProdukIntent.SetBarcode -> _uiState.update { it.copy(barcode = intent.v, pesanError = null) }
        is FormProdukIntent.SetHargaJual -> _uiState.update { it.copy(hargaJualInput = intent.v.filter { it.isDigit() }, pesanError = null) }
        is FormProdukIntent.SetHargaModal -> _uiState.update { it.copy(hargaModalInput = intent.v.filter { it.isDigit() }, pesanError = null) }
        is FormProdukIntent.SetKategori -> _uiState.update { it.copy(kategori = intent.v, pesanError = null) }
        is FormProdukIntent.SetStokAwal -> {
            if (intent.v.isEmpty() || intent.v.matches(Regex("^\\d*([.,]\\d{0,3})?$"))) {
                _uiState.update { it.copy(stokAwalInput = intent.v, pesanError = null) }
            }
        }
        FormProdukIntent.BukaScanner -> _uiState.update { it.copy(scannerTerbuka = true) }
        FormProdukIntent.TutupScanner -> _uiState.update { it.copy(scannerTerbuka = false) }
        is FormProdukIntent.OnBarcodeHasilScan -> _uiState.update { it.copy(barcode = intent.barcode.trim(), scannerTerbuka = false) }
        FormProdukIntent.GenerateSkuOtomatis -> {
            val stamp = SimpleDateFormat("yyMMdd", Locale.ROOT).format(Date())
            val randomSuffix = (1000..9999).random()
            _uiState.update { it.copy(sku = "PRD-$stamp-$randomSuffix", pesanError = null) }
        }
        FormProdukIntent.SimpanProduk -> simpanProduk()
    }

    private fun simpanProduk() {
        val s = _uiState.value
        val namaClean = s.nama.trim()
        val skuClean = s.sku.trim()
        val hargaJual = s.hargaJualInput.toLongOrNull() ?: 0L
        val hargaModal = s.hargaModalInput.toLongOrNull() ?: 0L

        if (namaClean.isBlank()) { _uiState.update { it.copy(pesanError = "Nama produk tidak boleh kosong") }; return }
        if (skuClean.isBlank()) { _uiState.update { it.copy(pesanError = "SKU wajib diisi") }; return }

        val stokAwalScaled = (s.stokAwalInput.replace(',', '.').toDoubleOrNull() ?: 0.0 * QUANTITY_SCALE).toLong()

        viewModelScope.launch {
            _uiState.update { it.copy(sedangMenyimpan = true) }
            val now = currentTimeMillis()
            val sesi = sessionProvider.sesiAktif()

            val result = if (s.isEditMode && targetProdukId != null) {
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
                productService.createProduct(
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
                onSuccess = { _uiState.update { it.copy(sedangMenyimpan = false) }; _event.send(FormProdukEvent.SimpanSukses) },
                onFailure = { err -> _uiState.update { it.copy(sedangMenyimpan = false, pesanError = err.message ?: "Gagal menyimpan") } },
            )
        }
    }
}