package com.sentral.org.ui.screen.pos

import com.sentral.org.data.model.CheckoutRequest
import com.sentral.org.data.model.MetodePembayaran
import com.sentral.org.data.model.PaymentRequest
import com.sentral.org.data.service.CheckoutService
import com.sentral.org.data.service.PrinterService
import com.sentral.org.data.repository.ProfilTokoRepository
import com.sentral.org.data.repository.TransaksiRepository
import com.sentral.org.data.session.SesiKasirProvider
import com.sentral.org.data.service.ReceiptFormatter
import com.sentral.org.shared.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

internal class KasirPaymentActions(
    private val checkoutService: CheckoutService,
    private val printerService: PrinterService,
    private val profilRepo: ProfilTokoRepository,
    private val transaksiRepo: TransaksiRepository,
    private val sesi: SesiKasirProvider,
    private val scope: CoroutineScope,
    private val sendEvent: (KasirEvent) -> Unit,
    private val setProcessing: (Boolean) -> Unit,
    private val getActiveCartId: () -> Long?,
    private val getCartItems: () -> List<BarisKeranjangUi>,
    private val getSubtotal: () -> Long,
    private val clearManualChoice: () -> Unit,
    private val nomorTransaksiGenerator: () -> String,
) {

    fun bayarCash(uangDiterima: Long) {
        val total = getSubtotal()
        if (uangDiterima < total) {
            sendEvent(KasirEvent.Pesan("Uang diterima kurang dari total", KasirEvent.Pesan.Jenis.GALAT))
            return
        }
        eksekusiBayar(listOf(PaymentRequest(MetodePembayaran.CASH, total, received = uangDiterima)))
    }

    fun bayarQris(referensi: String? = null) {
        eksekusiBayar(listOf(PaymentRequest(MetodePembayaran.QRIS, getSubtotal(), reference = referensi)))
    }

    private fun eksekusiBayar(payments: List<PaymentRequest>) {
        val cartId = getActiveCartId() ?: run {
            sendEvent(KasirEvent.Pesan("Tidak ada keranjang aktif", KasirEvent.Pesan.Jenis.GALAT))
            return
        }
        if (getCartItems().isEmpty()) {
            sendEvent(KasirEvent.Pesan("Keranjang kosong", KasirEvent.Pesan.Jenis.GALAT))
            return
        }

        scope.launch {
            setProcessing(true)
            try {
                val s = sesi.sesiAktif() ?: run {
                    sendEvent(KasirEvent.Pesan("Buka shift kasir terlebih dahulu", KasirEvent.Pesan.Jenis.GALAT))
                    return@launch
                }
                val now = System.currentTimeMillis()

                checkoutService.checkout(
                    CheckoutRequest(
                        cartId = cartId,
                        cashierId = s.kasirId,
                        shiftId = s.shiftId,
                        payments = payments,
                        transactionNumber = nomorTransaksiGenerator(),
                        now = now,
                    ),
                ).fold(
                    onSuccess = { r ->
                        clearManualChoice()
                        triggerAutoPrint(r.transactionId)
                        sendEvent(KasirEvent.CheckoutBerhasil(r.transactionNumber, r.change))
                    },
                    onFailure = { error ->
                        sendEvent(KasirEvent.Pesan(error.pesanPengguna(), KasirEvent.Pesan.Jenis.GALAT))
                    },
                )
            } finally {
                setProcessing(false)
            }
        }
    }

    private fun triggerAutoPrint(transactionId: Long) {
        scope.launch {
            try {
                val profilToko = profilRepo.get()
                if (profilToko?.cetakOtomatis == false) return@launch

                val transaksi = transaksiRepo.getById(transactionId) ?: return@launch
                val items = transaksiRepo.getItems(transactionId)
                val payments = transaksiRepo.getPayments(transactionId)

                val receiptData = ReceiptFormatter.format(
                    toko = profilToko,
                    transaksi = transaksi,
                    items = items,
                    payments = payments,
                )

                printerService.enqueue(receiptData) { result ->
                    scope.launch {
                        when (result) {
                            is com.sentral.org.data.model.PrintResult.Success -> {}
                            is com.sentral.org.data.model.PrintResult.Failure -> {
                                sendEvent(KasirEvent.Pesan("Struk gagal dicetak: ${result.message}", KasirEvent.Pesan.Jenis.GALAT))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Log error but don't fail checkout
            }
        }
    }
}