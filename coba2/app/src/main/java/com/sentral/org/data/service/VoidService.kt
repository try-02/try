package com.sentral.org.data.service

import com.sentral.org.data.dao.*
import com.sentral.org.data.entity.PergerakanKasEntity
import com.sentral.org.data.model.*

class VoidService(
    private val write: PosWriteService,
    private val transactions: TransaksiDao,
    private val transactionItems: ItemTransaksiDao,
    private val returns: ReturDao,
    private val cashiers: KasirDao,
    private val shifts: ShiftDao,
    private val payments: PembayaranDao,
    private val cashLedger: PergerakanKasDao,
    private val inventory: InventoryMutationService,
) {
    suspend fun void(request: VoidRequest): Result<Unit> = suspendRunCatching {
        write.run {
            val tx = transactions.getById(request.transactionId)
                ?: throw PosDataException.NotFound("Transaksi tidak ditemukan")
            if (tx.status != StatusTransaksi.SELESAI) {
                throw PosDataException.InvalidState("Transaksi sudah VOID atau tidak valid")
            }
            if (returns.existsForTransaction(tx.id)) {
                throw PosDataException.InvalidState("Transaksi yang sudah memiliki retur tidak dapat di-VOID")
            }
            val cashier = cashiers.getById(request.cashierId)
                ?: throw PosDataException.NotFound("Kasir tidak ditemukan")
            if (!cashier.aktif) throw PosDataException.Validation("Kasir tidak aktif")
            val shift = request.shiftId?.let { shifts.getById(it) }
            if (shift != null && (shift.status != StatusShift.TERBUKA || shift.kasirId != cashier.id)) {
                throw PosDataException.InvalidState("Shift tidak aktif atau bukan milik kasir")
            }

            val items = transactionItems.getByTransaction(tx.id)
            check(items.isNotEmpty()) { "Transaksi tidak memiliki item" }
            check(transactions.markVoid(tx.id, request.now, request.reason) == 1) {
                "Transaksi sudah berubah status"
            }

            items.forEach { item ->
                val productId = item.produkId
                    ?: throw PosDataException.Validation("Produk historical tidak tersedia untuk reversal stok")
                inventory.mutateNormal(
                    productId = productId,
                    normalDelta = item.jumlah,
                    type = JenisPergerakanPersediaan.PEMBATALAN_PENJUALAN,
                    transactionId = tx.id,
                    transactionItemId = item.id,
                    shiftId = shift?.id,
                    note = "Void ${tx.nomorTransaksi}",
                    now = request.now,
                )
            }

            val cashRefund = payments.getByTransaction(tx.id)
                .filter { it.metode == MetodePembayaran.CASH }
                .sumOf { it.jumlah }
            if (cashRefund > 0) {
                val sid = shift?.id ?: throw PosDataException.Validation("Void transaksi CASH membutuhkan shift")
                cashLedger.insert(
                    PergerakanKasEntity(
                        shiftId = sid,
                        // CATATAN: enum belum punya nilai khusus untuk refund VOID;
                        // pertimbangkan menambahkan VOID_REFUND agar laporan kas akurat.
                        jenis = JenisPergerakanKas.RETUR,
                        jumlahDelta = -cashRefund,
                        transaksiId = tx.id,
                        pengembalianId = null,
                        keterangan = "Refund VOID ${tx.nomorTransaksi}",
                        dibuatPada = request.now,
                    ),
                )
            }
        }
    }
}