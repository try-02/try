package com.sentral.org.ui.screen.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.SQLiteException
import co.touchlab.kermit.Logger
import com.sentral.org.data.dao.PersediaanDao
import com.sentral.org.data.entity.ProfilTokoEntity
import com.sentral.org.data.model.MetodePembayaran
import com.sentral.org.data.model.MoneyMath
import com.sentral.org.data.model.PrinterStatus
import com.sentral.org.data.model.PrinterStatus.SIAP
import com.sentral.org.data.model.QUANTITY_SCALE
import com.sentral.org.data.model.quantityOf
import com.sentral.org.data.repository.CartRepository
import com.sentral.org.data.repository.ProdukRepository
import com.sentral.org.data.repository.ProfilTokoRepository
import com.sentral.org.data.repository.TransaksiRepository
import com.sentral.org.data.service.CartService
import com.sentral.org.data.service.CheckoutService
import com.sentral.org.data.service.PrinterService
import com.sentral.org.data.session.SesiKasirProvider
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

sealed interface KasirIntent {
    data class PilihKeranjang(val id: Long) : KasirIntent
    object KeranjangBaru : KasirIntent
    object TahanKeranjang : KasirIntent
    object BatalkanKeranjang : KasirIntent
    data class LanjutkanKeranjang(val id: Long) : KasirIntent
    data class TambahProduk(val produkId: Long) : KasirIntent
    data class ScanBarcodeTambahProduk(val code: String) : KasirIntent
    data class TambahSatuan(val produkId: Long) : KasirIntent
    data class KurangiSatuan(val produkId: Long) : KasirIntent
    data class AturJumlah(val produkId: Long, val kuantitasScaled: Long) : KasirIntent
    data class HapusBaris(val produkId: Long) : KasirIntent
    data class BatalkanBarisDenganUndo(val produkId: Long) : KasirIntent
    data class RestoreBaris(val produkId: Long, val jumlahScaled: Long) : KasirIntent
    data class BayarCash(val uangDiterima: Long) : KasirIntent
    data class BayarQris(val referensi: String?) : KasirIntent
}

@OptIn(ExperimentalCoroutinesApi::class)
class KasirViewModel(
    private val cartService: CartService,
    private val checkoutService: CheckoutService,
    private val produkRepo: ProdukRepository,
    private val cartRepo: CartRepository,
    private val persediaanDao: PersediaanDao,
    private val profilRepo: ProfilTokoRepository,
    private val sesi: SesiKasirProvider,
    private val printerService: PrinterService,
    private val transaksiRepo: TransaksiRepository,
) : ViewModel() {

    private companion object { private val log = Logger.withTag("KasirVM") }

    private val keranjangMutex = Mutex()
    private val pilihanManual = MutableStateFlow<Long?>(null)
    private val sedangProses = MutableStateFlow(false)
    private val _event = Channel<KasirEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    val profilToko: StateFlow<ProfilTokoEntity?> = profilRepo.observe().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val stokPerProduk: StateFlow<Map<Long, Long>> = persediaanDao.observeAll().map { it.associate { it.produkId to it.jumlah / QUANTITY_SCALE } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    val printerStatus: StateFlow<PrinterStatus> = printerService.status.stateIn(viewModelScope, SharingStarted.Eagerly, SIAP)

    private data class DetilKeranjang(
        val produk: List<com.sentral.org.data.entity.ProdukEntity>,
        val carts: List<com.sentral.org.data.entity.KeranjangEntity>,
        val manual: Long?,
    )

    private val dataKeranjangFlow = combine(produkRepo.observeAktif(), cartRepo.observeOpen(), pilihanManual) { produk, carts, manual -> DetilKeranjang(produk, carts, manual) }
        .flatMapLatest { d ->
            val efektif = d.manual?.takeIf { id -> d.carts.any { it.id == id } } ?: d.carts.firstOrNull()?.id
            val itemsFlow = if (efektif == null) flowOf(emptyList()) else cartRepo.observeItemsLive(efektif)
            itemsFlow.map { rows ->
                val baris = rows.map { BarisKeranjangUi(itemId = it.item.id, produkId = it.item.produkId, nama = it.namaMaster, hargaSatuan = it.hargaMaster, jumlahScaled = it.item.jumlah, totalBaris = MoneyMath.lineTotal(it.hargaMaster, it.item.jumlah)) }
                Triple(d, efektif, baris)
            }
        }

    val uiState: StateFlow<KasirUiState> = combine(dataKeranjangFlow, sedangProses) { (d, efektif, baris), proses ->
        KasirUiState(produk = d.produk, keranjangTerbuka = d.carts, keranjangAktifId = efektif, baris = baris, subtotal = MoneyMath.sumExact(baris.map { it.totalBaris }), sedangProses = proses)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KasirUiState())

    private val cartActions = KasirCartActions(
        cartService = cartService, cartRepo = cartRepo, sesi = sesi, scope = viewModelScope,
        sendEvent = { _event.send(it) }, setManualChoice = { pilihanManual.value = it }, getActiveCartId = { uiState.value.keranjangAktifId },
    )

    private val paymentActions = KasirPaymentActions(
        checkoutService = checkoutService, printerService = printerService, profilRepo = profilRepo,
        transaksiRepo = transaksiRepo, sesi = sesi, scope = viewModelScope, sendEvent = { _event.send(it) },
        setProcessing = { sedangProses.value = it }, getActiveCartId = { uiState.value.keranjangAktifId },
        getCartItems = { uiState.value.baris }, getSubtotal = { uiState.value.subtotal },
        clearManualChoice = { pilihanManual.value = null }, nomorTransaksiGenerator = { NomorTransaksiGenerator.buat(System.currentTimeMillis()) },
    )

    fun onIntent(intent: KasirIntent) = when (intent) {
        is KasirIntent.PilihKeranjang -> pilihanManual.value = intent.id
        KasirIntent.KeranjangBaru -> cartActions.keranjangBaru()
        KasirIntent.TahanKeranjang -> cartActions.tahanKeranjang()
        KasirIntent.BatalkanKeranjang -> cartActions.batalkanKeranjang()
        is KasirIntent.LanjutkanKeranjang -> cartActions.lanjutkanKeranjang(intent.id)
        is KasirIntent.TambahProduk -> cartActions.tambahProduk(intent.produkId)
        is KasirIntent.ScanBarcodeTambahProduk -> viewModelScope.launch { _event.send(KasirEvent.ScanResult(cartActions.scanBarcodeTambahProduk(intent.code, produkRepo))) }
        is KasirIntent.TambahSatuan -> cartActions.tambahSatuan(intent.produkId)
        is KasirIntent.KurangiSatuan -> cartActions.kurangiSatuan(intent.produkId)
        is KasirIntent.AturJumlah -> cartActions.aturJumlah(intent.produkId, intent.kuantitasScaled)
        is KasirIntent.HapusBaris -> cartActions.hapusBaris(intent.produkId)
        is KasirIntent.BatalkanBarisDenganUndo -> {
            val baris = uiState.value.baris.firstOrNull { it.produkId == intent.produkId } ?: return
            cartActions.batalkanBarisDenganUndo(intent.produkId, baris.nama, baris.jumlahScaled)
        }
        is KasirIntent.RestoreBaris -> cartActions.restoreBaris(intent.produkId, intent.jumlahScaled)
        is KasirIntent.BayarCash -> paymentActions.bayarCash(intent.uangDiterima)
        is KasirIntent.BayarQris -> paymentActions.bayarQris(intent.referensi)
    }

    private suspend fun pastikanKeranjangAktif(): Long? = keranjangMutex.withLock {
        val manualId = pilihanManual.value
        if (manualId != null) { val cart = cartRepo.getById(manualId); if (cart != null && cart.status == com.sentral.org.data.model.StatusKeranjang.AKTIF) return manualId; pilihanManual.value = null }
        val stateCartId = uiState.value.keranjangAktifId
        if (stateCartId != null) { val cart = cartRepo.getById(stateCartId); if (cart != null && cart.status == com.sentral.org.data.model.StatusKeranjang.AKTIF) return stateCartId }
        val s = sesi.sesiAktif() ?: run { _event.send(KasirEvent.Pesan("Buka shift kasir terlebih dahulu", KasirEvent.Pesan.Jenis.GALAT)); return null }
        return cartService.buatKeranjang(s.kasirId, System.currentTimeMillis()).fold(onSuccess = { id -> pilihanManual.value = id; id }, onFailure = { e -> _event.send(KasirEvent.Pesan(e.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT)); null })
    }
}

object NomorTransaksiGenerator {
    private val counter = AtomicInteger(0)
    fun buat(now: Long): String {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date(now))
        val seq = (counter.incrementAndGet() % 1000).let { if (it < 0) it + 1000 else it }
        val randomSuffix = (10..99).random()
        return "TRX-$stamp-${seq.toString().padStart(3, '0')}-$randomSuffix"
    }
}