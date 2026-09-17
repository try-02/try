package com.sentral.org.ui.screen.pos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sentral.org.R
import com.sentral.org.data.model.CheckoutRequest
import com.sentral.org.data.model.MetodePembayaran
import com.sentral.org.data.model.MoneyMath
import com.sentral.org.data.model.PaymentRequest
import com.sentral.org.data.model.QUANTITY_SCALE
import com.sentral.org.data.model.quantityOf
import com.sentral.org.data.repository.CartRepository
import com.sentral.org.data.repository.ProdukRepository
import com.sentral.org.data.repository.ProfilTokoRepository
import com.sentral.org.data.dao.PersediaanDao
import com.sentral.org.data.entity.ProfilTokoEntity
import com.sentral.org.data.service.CartService
import com.sentral.org.data.service.CheckoutService
import com.sentral.org.data.session.SesiKasirProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.sentral.org.data.service.PrinterService
import com.sentral.org.data.usecase.AutoPrintUseCase
import com.sentral.org.data.model.PrinterStatus
import com.sentral.org.data.model.PrinterStatus.SIAP
import co.touchlab.kermit.Logger

@OptIn(ExperimentalCoroutinesApi::class)
class KasirViewModel(
    application: Application,
    private val cartService: CartService,
    private val checkoutService: CheckoutService,
    private val produkRepo: ProdukRepository,
    private val cartRepo: CartRepository,
    private val persediaanDao: PersediaanDao,
    private val profilRepo: ProfilTokoRepository,
    private val sesi: SesiKasirProvider,
    private val printerService: PrinterService,
    private val autoPrintUseCase: AutoPrintUseCase,
) : AndroidViewModel(application) {

    private companion object {
        val log = Logger.withTag("KasirVM")
    }

    private val pilihanManual = MutableStateFlow<Long?>(null)
    private val sedangProses = MutableStateFlow(false)
    private val _event = Channel<KasirEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    /** Profil toko utk header (nama toko). Tidak ikut combine utama agar hemat rekomposisi. */
    val profilToko: StateFlow<ProfilTokoEntity?> = profilRepo.observe()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Stok live per produk (scaled -> unit) utk indikator stok rendah di kartu. */
    val stokPerProduk: StateFlow<Map<Long, Long>> = persediaanDao.observeAll()
        .map { daftar -> daftar.associate { it.produkId to it.jumlah / QUANTITY_SCALE } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Status printer untuk ditampilkan di TopAppBar. */
    val printerStatus: StateFlow<PrinterStatus> = 
        printerService.status
            .stateIn(viewModelScope, SharingStarted.Eagerly, SIAP)

    val uiState: StateFlow<KasirUiState> = combine(
        produkRepo.observeAktif(),
        cartRepo.observeOpen(),
        pilihanManual,
        sedangProses,
    ) { produk, carts, manual, proses -> Detil(produk, carts, manual, proses) }
        .flatMapLatest { d ->
            // Auto-pilih: keranjang pilihan user bila masih terbuka, else keranjang teratas.
            val efektif = d.manual?.takeIf { id -> d.carts.any { it.id == id } }
                ?: d.carts.firstOrNull()?.id
            val itemsFlow = if (efektif == null) {
                flowOf(emptyList())
            } else {
                cartRepo.observeItemsLive(efektif)
            }
            itemsFlow.map { rows ->
                val baris = rows.map {
                    BarisKeranjangUi(
                        itemId = it.item.id,
                        produkId = it.item.produkId,
                        nama = it.namaMaster,
                        hargaSatuan = it.hargaMaster,
                        jumlahScaled = it.item.jumlah,
                        totalBaris = MoneyMath.lineTotal(it.hargaMaster, it.item.jumlah),
                    )
                }
                KasirUiState(
                    produk = d.produk,
                    keranjangTerbuka = d.carts,
                    keranjangAktifId = efektif,
                    baris = baris,
                    subtotal = MoneyMath.sumExact(baris.map { it.totalBaris }),
                    sedangProses = d.proses,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KasirUiState())

    // ---------- Intent: keranjang ----------

    fun pilihKeranjang(id: Long) { pilihanManual.update { id } }

    fun keranjangBaru() {
        viewModelScope.launch {
            val s = sesi.sesiAktif() ?: run {
                kirim(getString(R.string.err_shift_not_open), KasirEvent.Pesan.Jenis.GALAT); return@launch
            }
            cartService.buatKeranjang(s.kasirId, System.currentTimeMillis())
                .onSuccess { id ->
                    pilihanManual.update { id }
                    kirim(getString(R.string.info_cart_created), KasirEvent.Pesan.Jenis.INFO)
                }
                .onFailure { kirim(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT) }
        }
    }

    fun tambahProduk(produkId: Long) {
        viewModelScope.launch {
            val cartId = pastikanKeranjangAktif() ?: return@launch
            cartService.addProduct(cartId, produkId, quantityOf(1), System.currentTimeMillis())
                .onFailure { kirim(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT) }
        }
    }

    fun tambahJumlah(produkId: Long) {
        viewModelScope.launch {
            val cartId = uiState.value.keranjangAktifId ?: return@launch
            cartService.ubahJumlah(cartId, produkId, quantityOf(1), System.currentTimeMillis())
                .onFailure { kirim(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT) }
        }
    }

    fun kurangJumlah(produkId: Long) {
        viewModelScope.launch {
            val cartId = uiState.value.keranjangAktifId ?: return@launch
            cartService.ubahJumlah(cartId, produkId, quantityOf(-1), System.currentTimeMillis())
                .onFailure { kirim(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT) }
        }
    }

    fun hapusBaris(produkId: Long) {
        viewModelScope.launch {
            val cartId = uiState.value.keranjangAktifId ?: return@launch
            cartService.hapusProduk(cartId, produkId)
                .onFailure { kirim(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT) }
        }
    }

    fun undoHapusBaris(produkId: Long, jumlahScaled: Long) {
        viewModelScope.launch {
            val cartId = uiState.value.keranjangAktifId ?: run {
                kirim(getString(R.string.err_cart_already_gone), KasirEvent.Pesan.Jenis.GALAT)
                return@launch
            }
            if (jumlahScaled <= 0) return@launch
            cartService.addProduct(cartId, produkId, jumlahScaled, System.currentTimeMillis())
                .onFailure { kirim(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT) }
        }
    }

    /**
     * Trigger cetak struk otomatis setelah checkout sukses.
     */
    private fun triggerAutoPrint(transactionId: Long) {
        viewModelScope.launch { autoPrintUseCase(transactionId) }
    }

    // ---------- Intent: pembayaran ----------

    fun bayarCash(uangDiterima: Long) {
        val total = uiState.value.subtotal
        if (uangDiterima < total) {
            kirim(getString(R.string.err_insufficient_payment), KasirEvent.Pesan.Jenis.GALAT); return
        }
        eksekusiBayar(listOf(PaymentRequest(MetodePembayaran.CASH, total, received = uangDiterima)))
    }

    fun bayarQris(referensi: String? = null) {
        eksekusiBayar(listOf(PaymentRequest(MetodePembayaran.QRIS, uiState.value.subtotal, reference = referensi)))
    }

    // ---------- Internals ----------

    private fun ubah(produkId: Long, delta: Long) {
        viewModelScope.launch {
            val cartId = uiState.value.keranjangAktifId ?: return@launch
            cartService.ubahJumlah(cartId, produkId, delta, System.currentTimeMillis())
                .onFailure { kirim(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT) }
        }
    }

    private fun eksekusiBayar(payments: List<PaymentRequest>) {
        val state = uiState.value
        val cartId = state.keranjangAktifId ?: run {
            kirim(getString(R.string.err_no_active_cart), KasirEvent.Pesan.Jenis.GALAT); return
        }
        if (state.baris.isEmpty()) {
            kirim(getString(R.string.err_cart_empty), KasirEvent.Pesan.Jenis.GALAT); return
        }
        
        log.d { "🛒 eksekusiBayar() called with ${payments.size} payments" }
        
        viewModelScope.launch {
            sedangProses.update { true }
            try {
                val s = sesi.sesiAktif() ?: run {
                    kirim(getString(R.string.err_shift_not_open), KasirEvent.Pesan.Jenis.GALAT); return@launch
                }
                val now = System.currentTimeMillis()
                
                log.d { "⏳ Calling checkoutService.checkout()" }
                
                checkoutService.checkout(
                    CheckoutRequest(
                        cartId = cartId,
                        cashierId = s.kasirId,
                        shiftId = s.shiftId,
                        payments = payments,
                        transactionNumber = NomorTransaksiGenerator.buat(now),
                        now = now,
                    )
                ).fold(
                    onSuccess = { r ->
                        log.i { "✅ Checkout success: txId=${r.transactionId}, number=${r.transactionNumber}" }
                        
                        // ===== AUTO-PRINT: Trigger cetak struk =====
                        triggerAutoPrint(r.transactionId)
                        
                        _event.send(KasirEvent.CheckoutBerhasil(r.transactionNumber, r.change))
                    },
                    onFailure = { error ->
                        log.e(error) { "❌ Checkout failed: ${error.message}" }
                        kirim(error.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT)
                    },
                )
            } finally {
                sedangProses.update { false }
            }
        }
    }

    private suspend fun pastikanKeranjangAktif(): Long? {
        uiState.value.keranjangAktifId?.let { return it }
        val s = sesi.sesiAktif() ?: run {
            kirim(getString(R.string.err_shift_not_open), KasirEvent.Pesan.Jenis.GALAT); return null
        }
        return cartService.buatKeranjang(s.kasirId, System.currentTimeMillis()).fold(
            onSuccess = { id -> pilihanManual.update { id }; id },
            onFailure = { e -> kirim(e.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT); null },
        )
    }

    private fun kirim(teks: String, jenis: KasirEvent.Pesan.Jenis) {
        viewModelScope.launch { _event.send(KasirEvent.Pesan(teks, jenis)) }
    }

    private fun getString(resId: Int): String = getApplication<Application>().getString(resId)

    private data class Detil(
        val produk: List<com.sentral.org.data.entity.ProdukEntity>,
        val carts: List<com.sentral.org.data.entity.KeranjangEntity>,
        val manual: Long?,
        val proses: Boolean,
    )
}

/** Nomor transaksi unik-praktis; unique index DB adalah pengaman pamungkas. */
object NomorTransaksiGenerator {
    fun buat(now: Long): String {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date(now))
        return "TRX-$stamp-${(100..999).random()}"
    }
}
