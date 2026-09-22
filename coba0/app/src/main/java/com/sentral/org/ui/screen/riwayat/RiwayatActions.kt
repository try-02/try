package com.sentral.org.ui.screen.riwayat

import androidx.sqlite.SQLiteException
import com.sentral.org.data.entity.TransaksiDenganDetail
import com.sentral.org.data.model.MetodePembayaran
import com.sentral.org.data.model.PrintResult
import com.sentral.org.data.model.QUANTITY_SCALE
import com.sentral.org.data.model.ReturnLineRequest
import com.sentral.org.data.model.ReturnRequest
import com.sentral.org.data.model.TujuanStokPengembalian
import com.sentral.org.data.model.VoidRequest
import com.sentral.org.data.repository.ProfilTokoRepository
import com.sentral.org.data.repository.TransaksiRepository
import com.sentral.org.data.service.PrinterService
import com.sentral.org.data.service.ReceiptFormatter
import com.sentral.org.data.service.ReturService
import com.sentral.org.data.service.VoidService
import com.sentral.org.data.session.ActiveSesiKasirProvider
import com.sentral.org.export.TransaksiExportUseCase
import com.sentral.org.shared.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class RiwayatActions(
    private val transaksiRepo: TransaksiRepository,
    private val profilRepo: ProfilTokoRepository,
    private val printerService: PrinterService,
    private val exportUseCase: TransaksiExportUseCase,
    private val voidService: VoidService,
    private val returService: ReturService,
    private val returDao: ReturDao,
    private val sessionProvider: ActiveSesiKasirProvider,
    private val scope: CoroutineScope,
    private val sendEvent: (RiwayatEvent) -> Unit,
    private val getRawDetail: () -> TransaksiDenganDetail?,
    private val setRawDetail: (TransaksiDenganDetail?) -> Unit,
    private val updateUiState: (RiwayatUiState) -> RiwayatUiState,
    private val refreshFilter: () -> Unit,
) {

    fun cetakUlangStruk() {
        val detail = getRawDetail() ?: return
        if (updateUiState(RiwayatUiState()).sedangReprint) return

        scope.launch {
            updateUiState { it.copy(sedangReprint = true) }
            try {
                val toko = profilRepo.get()
                val receiptData = ReceiptFormatter.format(
                    toko = toko,
                    transaksi = detail.transaksi,
                    items = detail.items,
                    payments = detail.pembayaran,
                    isReprint = true,
                )

                printerService.enqueue(receiptData) { result ->
                    scope.launch {
                        updateUiState { it.copy(sedangReprint = false) }
                        when (result) {
                            is PrintResult.Success -> sendEvent(RiwayatEvent.ReprintSukses(detail.transaksi.nomorTransaksi))
                            is PrintResult.Failure -> sendEvent(RiwayatEvent.Pesan("Gagal cetak salinan: ${result.message}", RiwayatEvent.Pesan.Jenis.GALAT))
                        }
                    }
                }
            } catch (e: SQLiteException) {
                updateUiState { it.copy(sedangReprint = false) }
                sendEvent(RiwayatEvent.Pesan(e.message ?: "Gagal mengambil data untuk cetak ulang", RiwayatEvent.Pesan.Jenis.GALAT))
            }
        }
    }

    fun eksporLaporanExcel(filter: TransaksiFilter) {
        if (updateUiState(RiwayatUiState()).sedangEkspor) return

        scope.launch {
            updateUiState { it.copy(sedangEkspor = true) }
            exportUseCase.exportToExcel(filter).fold(
                onSuccess = { uri ->
                    updateUiState { it.copy(sedangEkspor = false) }
                    sendEvent(RiwayatEvent.FileSiapDibagikan(uri))
                },
                onFailure = { error ->
                    updateUiState { it.copy(sedangEkspor = false) }
                    sendEvent(RiwayatEvent.Pesan(error.message ?: "Gagal ekspor laporan", RiwayatEvent.Pesan.Jenis.GALAT))
                },
            )
        }
    }

    fun konfirmasiVoid(alasan: String) {
        val detail = getRawDetail() ?: return
        if (alasan.trim().isBlank()) {
            scope.launch { sendEvent(RiwayatEvent.Pesan("Alasan pembatalan wajib diisi", RiwayatEvent.Pesan.Jenis.GALAT)) }
            return
        }

        scope.launch {
            updateUiState { it.copy(sedangMemprosesAksi = true) }
            val now = currentTimeMillis()
            val sesi = sessionProvider.sesiAktif()

            val request = VoidRequest(
                transactionId = detail.transaksi.id,
                cashierId = sesi?.kasirId ?: detail.transaksi.kasirId,
                shiftId = sesi?.shiftId,
                reason = alasan.trim(),
                now = now,
            )

            voidService.void(request).fold(
                onSuccess = {
                    try {
                        val toko = profilRepo.get()
                        val receiptData = ReceiptFormatter.formatVoid(
                            toko = toko,
                            transaksi = detail.transaksi,
                            items = detail.items,
                            kasirPelaksana = sesi?.namaKasir ?: detail.transaksi.namaKasir,
                            alasan = alasan.trim(),
                            waktuVoid = now,
                        )
                        printerService.enqueue(receiptData)
                    } catch (_: Exception) {}

                    val updatedDetail = transaksiRepo.getDetailById(detail.transaksi.id)
                    setRawDetail(updatedDetail)
                    updateUiState {
                        it.copy(
                            sedangMemprosesAksi = false,
                            dialogVoidTerbuka = false,
                            detailTerpilih = updatedDetail?.toUiModel(),
                        )
                    }
                    refreshFilter()
                    sendEvent(RiwayatEvent.VoidSukses(detail.transaksi.nomorTransaksi))
                },
                onFailure = { error ->
                    updateUiState { it.copy(sedangMemprosesAksi = false) }
                    sendEvent(RiwayatEvent.Pesan(error.message ?: "Gagal membatalkan transaksi", RiwayatEvent.Pesan.Jenis.GALAT))
                },
            )
        }
    }

    fun bukaDialogRetur() {
        val detail = getRawDetail() ?: return
        scope.launch {
            updateUiState { it.copy(sedangMemprosesAksi = true) }
            val returRows = detail.items.map { item ->
                val alreadyReturned = returDao.getReturnedQuantity(item.id)
                val remaining = (item.jumlah - alreadyReturned).coerceAtLeast(0L)
                ItemReturUi(
                    itemId = item.id,
                    namaProduk = item.namaProduk,
                    sisaQtyScaled = remaining,
                    qtyPilihanScaled = 0L,
                    tujuan = TujuanStokPengembalian.NORMAL,
                )
            }
            updateUiState {
                it.copy(
                    sedangMemprosesAksi = false,
                    dialogReturTerbuka = true,
                    daftarItemRetur = returRows,
                    catatanReturInput = "",
                )
            }
        }
    }

    fun konfirmasiRetur(
        returItems: List<ItemReturUi>,
        metodeRefund: MetodePembayaran,
        catatan: String,
        detailItems: List<com.sentral.org.data.entity.ItemTransaksiEntity>,
    ) {
        if (returItems.isEmpty()) {
            scope.launch { sendEvent(RiwayatEvent.Pesan("Pilih minimal 1 barang untuk diretur", RiwayatEvent.Pesan.Jenis.GALAT)) }
            return
        }

        val detail = getRawDetail() ?: return

        scope.launch {
            updateUiState { it.copy(sedangMemprosesAksi = true) }
            val now = currentTimeMillis()
            val sesi = sessionProvider.sesiAktif()

            val lines = returItems.map {
                ReturnLineRequest(
                    transactionItemId = it.itemId,
                    quantity = it.qtyPilihanScaled,
                    destination = it.tujuan,
                )
            }

            val request = ReturnRequest(
                transactionId = detail.transaksi.id,
                cashierId = sesi?.kasirId ?: detail.transaksi.kasirId,
                shiftId = sesi?.shiftId,
                lines = lines,
                refundMethod = metodeRefund,
                note = catatan.trim(),
                now = now,
            )

            returService.process(request).fold(
                onSuccess = { result ->
                    try {
                        val toko = profilRepo.get()
                        val itemMap = detailItems.associateBy { it.id }
                        val printItems = returItems.mapNotNull { returItem ->
                            itemMap[returItem.itemId]?.let { it to returItem.qtyPilihanScaled }
                        }
                        val receiptData = ReceiptFormatter.formatReturn(
                            toko = toko,
                            transaksi = detail.transaksi,
                            itemsRetur = printItems,
                            totalRefund = result.refundAmount,
                            metodeRefund = metodeRefund,
                            kasirPelaksana = sesi?.namaKasir ?: detail.transaksi.namaKasir,
                            waktuRetur = now,
                        )
                        printerService.enqueue(receiptData)
                    } catch (_: Exception) {}

                    updateUiState {
                        it.copy(sedangMemprosesAksi = false, dialogReturTerbuka = false)
                    }
                    refreshFilter()
                    sendEvent(RiwayatEvent.ReturSukses(detail.transaksi.nomorTransaksi, result.refundAmount))
                },
                onFailure = { error ->
                    updateUiState { it.copy(sedangMemprosesAksi = false) }
                    sendEvent(RiwayatEvent.Pesan(error.message ?: "Gagal memproses retur", RiwayatEvent.Pesan.Jenis.GALAT))
                },
            )
        }
    }
}