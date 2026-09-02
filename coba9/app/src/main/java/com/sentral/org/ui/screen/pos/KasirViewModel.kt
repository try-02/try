package com.sentral.org.ui.screen.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

@OptIn(ExperimentalCoroutinesApi::class)
class KasirViewModel(
    private val cartService: CartService,
    private val checkoutService: CheckoutService,
    private val produkRepo: ProdukRepository,
    private val cartRepo: CartRepository,
    private val persediaanDao: PersediaanDao,
    private val profilRepo: ProfilTokoRepository,
    private val sesi: SesiKasirProvider,
) : ViewModel() {

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

    fun pilihKeranjang(id: Long) { pilihanManual.value = id }

    fun keranjangBaru() {
        viewModelScope.launch {
            val s = sesi.sesiAktif() ?: run {
                kirim("Buka shift kasir terlebih dahulu", KasirEvent.Pesan.Jenis.GALAT); return@launch
            }
            cartService.buatKeranjang(s.kasirId, System.currentTimeMillis()).fold(
                onSuccess = { id ->
                    pilihanManual.value = id
                    kirim("Keranjang baru dibuat", KasirEvent.Pesan.Jenis.INFO)
                },
                onFailure = { kirim(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT) },
            )
        }
    }

    fun tahanKeranjang() {
        viewModelScope.launch {
            val cartId = uiState.value.keranjangAktifId ?: return@launch
            cartService.hold(cartId, System.currentTimeMillis()).fold(
                onSuccess = { pilihanManual.value = null; kirim("Keranjang ditahan", KasirEvent.Pesan.Jenis.INFO) },
                onFailure = { kirim(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT) },
            )
        }
    }

    fun batalkanKeranjang() {
        viewModelScope.launch {
            val cartId = uiState.value.keranjangAktifId ?: return@launch
            cartService.cancel(cartId, System.currentTimeMillis()).fold(
                onSuccess = { pilihanManual.value = null; kirim("Keranjang dibatalkan", KasirEvent.Pesan.Jenis.INFO) },
                onFailure = { kirim(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT) },
            )
        }
    }

    fun lanjutkanKeranjang(id: Long) {
        viewModelScope.launch {
            cartService.resume(id, System.currentTimeMillis()).fold(
                onSuccess = { pilihanManual.value = id },
                onFailure = { kirim(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT) },
            )
        }
    }

    // ---------- Intent: item ----------

    fun tambahProduk(produkId: Long) {
        viewModelScope.launch {
            val cartId = pastikanKeranjangAktif() ?: return@launch
            cartService.addProduct(cartId, produkId, quantityOf(1), System.currentTimeMillis())
                .onFailure { kirim(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT) }
        }
    }

    fun tambahSatuan(produkId: Long) = ubah(produkId, quantityOf(1))
    fun kurangiSatuan(produkId: Long) = ubah(produkId, -quantityOf(1))

    fun hapusBaris(produkId: Long) {
        viewModelScope.launch {
            val cartId = uiState.value.keranjangAktifId ?: return@launch
            cartService.hapusBaris(cartId, produkId, System.currentTimeMillis())
                .onFailure { kirim(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT) }
        }
    }

    /**
     * Hapus baris dengan konfirmasi lembut: pesan sukses + tombol UNDO.
     * Dipakai SwipeToDismiss di keranjang — swipe tanpa dialog, masih bisa dibatalkan.
     * Event HapusBaris kini membawa produkId + jumlahScaled agar UNDO kembalikan qty tepat.
     */
    fun batalkanBarisDenganUndo(produkId: Long) {
        val baris = uiState.value.baris.firstOrNull { it.produkId == produkId } ?: return
        viewModelScope.launch {
            val cartId = uiState.value.keranjangAktifId ?: return@launch
            cartService.hapusBaris(cartId, produkId, System.currentTimeMillis()).fold(
                onSuccess = {
                    _event.send(
                        KasirEvent.HapusBaris(
                            nama = baris.nama,
                            produkId = produkId,
                            jumlahScaled = baris.jumlahScaled,
                        )
                    )
                },
                onFailure = { kirim(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT) },
            )
        }
    }

    /**
     * Dipanggil saat user tap UNDO di snackbar hapus baris.
     * Menambahkan ulang produk dengan jumlah asli (scaled) ke keranjang aktif.
     */
    fun restoreBaris(produkId: Long, jumlahScaled: Long) {
        viewModelScope.launch {
            val cartId = uiState.value.keranjangAktifId ?: run {
                kirim("Keranjang aktif sudah hilang, undo tidak bisa dilakukan", KasirEvent.Pesan.Jenis.GALAT)
                return@launch
            }
            if (jumlahScaled <= 0) return@launch
            cartService.addProduct(cartId, produkId, jumlahScaled, System.currentTimeMillis())
                .onFailure { kirim(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT) }
        }
    }

    // ---------- Intent: pembayaran ----------

    fun bayarCash(uangDiterima: Long) {
        val total = uiState.value.subtotal
        if (uangDiterima < total) {
            kirim("Uang diterima kurang dari total", KasirEvent.Pesan.Jenis.GALAT); return
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
            kirim("Tidak ada keranjang aktif", KasirEvent.Pesan.Jenis.GALAT); return
        }
        if (state.baris.isEmpty()) {
            kirim("Keranjang kosong", KasirEvent.Pesan.Jenis.GALAT); return
        }
        viewModelScope.launch {
            sedangProses.value = true
            try {
                val s = sesi.sesiAktif() ?: run {
                    kirim("Buka shift kasir terlebih dahulu", KasirEvent.Pesan.Jenis.GALAT); return@launch
                }
                val now = System.currentTimeMillis()
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
                        _event.send(KasirEvent.CheckoutBerhasil(r.transactionNumber, r.change))
                    },
                    onFailure = { kirim(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT) },
                )
            } finally {
                sedangProses.value = false
            }
        }
    }

    private suspend fun pastikanKeranjangAktif(): Long? {
        uiState.value.keranjangAktifId?.let { return it }
        val s = sesi.sesiAktif() ?: run {
            kirim("Buka shift kasir terlebih dahulu", KasirEvent.Pesan.Jenis.GALAT); return null
        }
        return cartService.buatKeranjang(s.kasirId, System.currentTimeMillis()).fold(
            onSuccess = { id -> pilihanManual.value = id; id },
            onFailure = { e -> kirim(e.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT); null },
        )
    }

    private fun kirim(teks: String, jenis: KasirEvent.Pesan.Jenis) {
        viewModelScope.launch { _event.send(KasirEvent.Pesan(teks, jenis)) }
    }

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