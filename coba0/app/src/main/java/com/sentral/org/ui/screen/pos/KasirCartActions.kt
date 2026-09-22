package com.sentral.org.ui.screen.pos

import com.sentral.org.data.entity.KeranjangEntity
import com.sentral.org.data.model.MetodePembayaran
import com.sentral.org.data.model.PaymentRequest
import com.sentral.org.data.model.QUANTITY_SCALE
import com.sentral.org.data.model.quantityOf
import com.sentral.org.data.repository.CartRepository
import com.sentral.org.data.service.CartService
import com.sentral.org.data.session.SesiKasirProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class KasirCartActions(
    private val cartService: CartService,
    private val cartRepo: CartRepository,
    private val sesi: SesiKasirProvider,
    private val scope: CoroutineScope,
    private val sendEvent: (KasirEvent) -> Unit,
    private val setManualChoice: (Long?) -> Unit,
    private val getActiveCartId: () -> Long?,
) {

    fun keranjangBaru() {
        scope.launch {
            val s = sesi.sesiAktif() ?: run {
                sendEvent(KasirEvent.Pesan("Buka shift kasir terlebih dahulu", KasirEvent.Pesan.Jenis.GALAT))
                return@launch
            }
            cartService.buatKeranjang(s.kasirId, System.currentTimeMillis()).fold(
                onSuccess = { id ->
                    setManualChoice(id)
                    sendEvent(KasirEvent.Pesan("Keranjang baru dibuat", KasirEvent.Pesan.Jenis.INFO))
                },
                onFailure = { sendEvent(KasirEvent.Pesan(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT)) },
            )
        }
    }

    fun tahanKeranjang() {
        scope.launch {
            val cartId = getActiveCartId() ?: return@launch
            cartService.hold(cartId, System.currentTimeMillis()).fold(
                onSuccess = {
                    setManualChoice(null)
                    sendEvent(KasirEvent.Pesan("Keranjang ditahan", KasirEvent.Pesan.Jenis.INFO))
                },
                onFailure = { sendEvent(KasirEvent.Pesan(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT)) },
            )
        }
    }

    fun batalkanKeranjang() {
        scope.launch {
            val cartId = getActiveCartId() ?: return@launch
            cartService.cancel(cartId, System.currentTimeMillis()).fold(
                onSuccess = {
                    setManualChoice(null)
                    sendEvent(KasirEvent.Pesan("Keranjang dibatalkan", KasirEvent.Pesan.Jenis.INFO))
                },
                onFailure = { sendEvent(KasirEvent.Pesan(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT)) },
            )
        }
    }

    fun lanjutkanKeranjang(id: Long) {
        scope.launch {
            cartService.resume(id, System.currentTimeMillis()).fold(
                onSuccess = { setManualChoice(id) },
                onFailure = { sendEvent(KasirEvent.Pesan(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT)) },
            )
        }
    }

    fun tambahProduk(produkId: Long) {
        scope.launch {
            val cartId = getOrCreateCart() ?: return@launch
            cartService.addProduct(cartId, produkId, quantityOf(1), System.currentTimeMillis())
                .onFailure { sendEvent(KasirEvent.Pesan(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT)) }
        }
    }

    suspend fun scanBarcodeTambahProduk(code: String, produkRepo: ProdukRepository): String? {
        val cleanCode = code.trim()
        val produk = produkRepo.getByBarcode(cleanCode) ?: produkRepo.getBySku(cleanCode) ?: return null

        if (!produk.aktif) {
            sendEvent(KasirEvent.Pesan("Produk '${produk.nama}' sedang tidak aktif", KasirEvent.Pesan.Jenis.GALAT))
            return null
        }
        val cartId = getOrCreateCart() ?: return null
        val result = cartService.addProduct(cartId, produk.id, quantityOf(1), System.currentTimeMillis())
        return if (result.isSuccess) produk.nama else null
    }

    fun tambahSatuan(produkId: Long) = ubah(produkId, quantityOf(1))
    fun kurangiSatuan(produkId: Long) = ubah(produkId, -quantityOf(1))

    fun aturJumlah(produkId: Long, kuantitasScaled: Long) {
        scope.launch {
            val cartId = getOrCreateCart() ?: return@launch
            cartService.setJumlah(cartId, produkId, kuantitasScaled, System.currentTimeMillis())
                .onFailure { sendEvent(KasirEvent.Pesan(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT)) }
        }
    }

    fun hapusBaris(produkId: Long) {
        scope.launch {
            val cartId = getActiveCartId() ?: return@launch
            cartService.hapusBaris(cartId, produkId, System.currentTimeMillis())
                .onFailure { sendEvent(KasirEvent.Pesan(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT)) }
        }
    }

    fun batalkanBarisDenganUndo(produkId: Long, barisNama: String, jumlahScaled: Long) {
        scope.launch {
            val cartId = getActiveCartId() ?: return@launch
            cartService.hapusBaris(cartId, produkId, System.currentTimeMillis()).fold(
                onSuccess = {
                    sendEvent(KasirEvent.HapusBaris(nama = barisNama, produkId = produkId, jumlahScaled = jumlahScaled))
                },
                onFailure = { sendEvent(KasirEvent.Pesan(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT)) },
            )
        }
    }

    fun restoreBaris(produkId: Long, jumlahScaled: Long) {
        scope.launch {
            val cartId = getActiveCartId() ?: run {
                sendEvent(KasirEvent.Pesan("Keranjang aktif sudah hilang, undo tidak bisa dilakukan", KasirEvent.Pesan.Jenis.GALAT))
                return@launch
            }
            if (jumlahScaled <= 0) return@launch
            cartService.addProduct(cartId, produkId, jumlahScaled, System.currentTimeMillis())
                .onFailure { sendEvent(KasirEvent.Pesan(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT)) }
        }
    }

    private fun ubah(produkId: Long, delta: Long) {
        scope.launch {
            val cartId = getActiveCartId() ?: return@launch
            cartService.ubahJumlah(cartId, produkId, delta, System.currentTimeMillis())
                .onFailure { sendEvent(KasirEvent.Pesan(it.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT)) }
        }
    }

    private fun getOrCreateCart(): Long? {
        val cartId = getActiveCartId()
        if (cartId != null) return cartId

        val s = sesi.sesiAktif() ?: run {
            sendEvent(KasirEvent.Pesan("Buka shift kasir terlebih dahulu", KasirEvent.Pesan.Jenis.GALAT))
            return null
        }
        return cartService.buatKeranjang(s.kasirId, System.currentTimeMillis()).fold(
            onSuccess = { id ->
                setManualChoice(id)
                id
            },
            onFailure = { e ->
                sendEvent(KasirEvent.Pesan(e.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT))
                null
            },
        )
    }
}