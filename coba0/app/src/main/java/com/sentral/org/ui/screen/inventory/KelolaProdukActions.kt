package com.sentral.org.ui.screen.inventory

import com.sentral.org.data.session.ActiveSesiKasirProvider
import com.sentral.org.domain.model.DisposeDamageRequest
import com.sentral.org.domain.model.KartuStokFilter
import com.sentral.org.domain.model.RecordDamageRequest
import com.sentral.org.domain.model.RestockRequest
import com.sentral.org.domain.model.StockOpnameRequest
import com.sentral.org.domain.service.ProductManagementService
import com.sentral.org.shared.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class KelolaProdukActions(
    private val productService: ProductManagementService,
    private val sessionProvider: ActiveSesiKasirProvider,
    private val scope: CoroutineScope,
    private val sendEvent: (KelolaProdukEvent) -> Unit,
    private val dismissDialogs: () -> Unit,
) {

    fun toggleAktifkanProduk(item: ProdukItemAdminUi) {
        scope.launch {
            val now = currentTimeMillis()
            val result = if (item.aktif) {
                productService.deactivateProduct(item.id, now)
            } else {
                productService.activateProduct(item.id, now)
            }
            result.fold(
                onSuccess = {
                    val statusStr = if (item.aktif) "dinonaktifkan" else "diaktifkan"
                    sendEvent(KelolaProdukEvent.Pesan("Produk '${item.nama}' berhasil $statusStr"))
                },
                onFailure = { err ->
                    sendEvent(KelolaProdukEvent.Pesan(err.message ?: "Gagal mengubah status", isError = true))
                },
            )
        }
    }

    fun submitRestock(produkId: Long, jumlahScaled: Long, alasan: String) {
        scope.launch {
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
                    dismissDialogs()
                    sendEvent(KelolaProdukEvent.Pesan("Stok berhasil ditambahkan"))
                },
                onFailure = { err ->
                    sendEvent(KelolaProdukEvent.Pesan(err.message ?: "Gagal restock", isError = true))
                },
            )
        }
    }

    fun submitOpname(produkId: Long, stokFisikScaled: Long, alasan: String) {
        scope.launch {
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
                    dismissDialogs()
                    sendEvent(KelolaProdukEvent.Pesan("Stock opname berhasil disesuaikan"))
                },
                onFailure = { err ->
                    sendEvent(KelolaProdukEvent.Pesan(err.message ?: "Gagal opname", isError = true))
                },
            )
        }
    }

    fun submitBarangRusak(produkId: Long, jumlahScaled: Long, alasan: String) {
        scope.launch {
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
                    dismissDialogs()
                    sendEvent(KelolaProdukEvent.Pesan("Barang rusak berhasil dicatat"))
                },
                onFailure = { err ->
                    sendEvent(KelolaProdukEvent.Pesan(err.message ?: "Gagal mencatat rusak", isError = true))
                },
            )
        }
    }

    fun submitPemusnahan(produkId: Long, jumlahScaled: Long, alasan: String) {
        scope.launch {
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
                    dismissDialogs()
                    sendEvent(KelolaProdukEvent.Pesan("Pemusnahan barang rusak berhasil dicatat"))
                },
                onFailure = { err ->
                    sendEvent(KelolaProdukEvent.Pesan(err.message ?: "Gagal pemusnahan", isError = true))
                },
            )
        }
    }
}