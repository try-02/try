package com.sentral.org.data.service

import com.sentral.org.data.dao.*
import com.sentral.org.data.entity.*
import com.sentral.org.data.model.*

class ReturService(
    private val write: PosWriteService,
    private val transactions: TransaksiDao,
    private val transactionItems: ItemTransaksiDao,
    private val returns: ReturDao,
    private val cashiers: KasirDao,
    private val shifts: ShiftDao,
    private val cashLedger: PergerakanKasDao,
    private val inventory: InventoryMutationService,
) {
    private data class PreparedLine(
        val item: ItemTransaksiEntity,
        val line: ReturnLineRequest,
        val refund: Long,
    )

    suspend fun process(request: ReturnRequest): Result<ReturnResult> = suspendRunCatching {
        write.run {
            if (request.lines.isEmpty()) throw PosDataException.Validation("Retur harus memiliki item")
            if (request.lines.map { it.transactionItemId }.distinct().size != request.lines.size) {
                throw PosDataException.Validation("Item transaksi duplikat dalam satu retur")
            }

            val transaction = transactions.getById(request.transactionId)
                ?: throw PosDataException.NotFound("Transaksi tidak ditemukan")
            if (transaction.status != StatusTransaksi.SELESAI) {
                throw PosDataException.InvalidState("Transaksi tidak dapat diretur")
            }
            val cashier = cashiers.getById(request.cashierId)
                ?: throw PosDataException.NotFound("Kasir tidak ditemukan")
            if (!cashier.aktif) throw PosDataException.Validation("Kasir tidak aktif")
            val shift = request.shiftId?.let {
                shifts.getById(it) ?: throw PosDataException.NotFound("Shift tidak ditemukan")
            }
            if (shift != null && (shift.status != StatusShift.TERBUKA || shift.kasirId != cashier.id)) {
                throw PosDataException.InvalidState("Shift retur tidak aktif atau bukan milik kasir")
            }

            val processed = request.lines.map { line ->
                val item = transactionItems.getById(line.transactionItemId)
                    ?: throw PosDataException.NotFound("Item transaksi tidak ditemukan")
                if (item.transaksiId != transaction.id) {
                    throw PosDataException.Validation("Item bukan milik transaksi")
                }
                if (line.quantity <= 0) throw PosDataException.Validation("Quantity retur harus > 0")

                val returnedQty = returns.getReturnedQuantity(item.id)
                val remaining = item.jumlah - returnedQty
                if (line.quantity > remaining) {
                    throw PosDataException.Validation("Quantity retur melebihi sisa quantity yang dapat diretur")
                }

                // Budget anti-over-refund: pembulatan per-sesi bisa saja melebihi
                // neto bila baris diretur bertahap; klem terhadap sisa budget.
                val netLine = item.totalBaris - item.diskonItem
                val budget = netLine - returns.getRefundTotal(item.id)
                if (budget <= 0) {
                    throw PosDataException.InvalidState("Nilai retur untuk item ini sudah habis")
                }
                val refund = MoneyMath.proportional(
                    part = line.quantity,
                    total = item.jumlah,
                    amount = netLine,
                ).coerceAtMost(budget)

                PreparedLine(item, line, refund)
            }

            val totalRefund = MoneyMath.sumExact(processed.map { it.refund })

            val returnId = returns.insert(
                PengembalianEntity(
                    transaksiId = transaction.id,
                    transaksiPenggantiId = request.replacementTransactionId,
                    dikembalikanPada = request.now,
                    kasirId = cashier.id,
                    shiftId = shift?.id,
                    namaKasir = cashier.nama,
                    jumlahPengembalian = totalRefund,
                    metodePengembalian = request.refundMethod,
                    catatan = request.note,
                    adalahTukarGaransi = request.warrantyExchange,
                ),
            )

            val returnItemIds = returns.insertItems(
                processed.map { p ->
                    ItemPengembalianEntity(
                        pengembalianId = returnId,
                        itemTransaksiId = p.item.id,
                        produkId = p.item.produkId,
                        namaProduk = p.item.namaProduk,
                        hargaSatuan = p.item.hargaSatuan,
                        jumlahDikembalikan = p.line.quantity,
                        jumlahRefund = p.refund,
                        tujuanStok = p.line.destination,
                    )
                },
            )

            processed.forEachIndexed { index, p ->
                val productId = p.item.produkId
                    ?: throw PosDataException.Validation("Produk historical tidak tersedia untuk mutasi stok")
                when (p.line.destination) {
                    TujuanStokPengembalian.NORMAL -> inventory.mutateNormal(
                        productId = productId,
                        normalDelta = p.line.quantity,
                        type = JenisPergerakanPersediaan.PENGEMBALIAN_NORMAL,
                        returnId = returnId,
                        returnItemId = returnItemIds[index],
                        shiftId = shift?.id,
                        note = "Retur transaksi ${transaction.nomorTransaksi}",
                        now = request.now,
                    )
                    TujuanStokPengembalian.RUSAK -> inventory.mutateNormal(
                        productId = productId,
                        normalDelta = 0,
                        damagedDelta = p.line.quantity,
                        type = JenisPergerakanPersediaan.PENGEMBALIAN_RUSAK,
                        returnId = returnId,
                        returnItemId = returnItemIds[index],
                        shiftId = shift?.id,
                        note = "Retur rusak transaksi ${transaction.nomorTransaksi}",
                        now = request.now,
                    )
                    TujuanStokPengembalian.TIDAK_DIKEMBALIKAN -> Unit
                }
            }

            if (request.refundMethod == MetodePembayaran.CASH && totalRefund > 0) {
                val sid = shift?.id ?: throw PosDataException.Validation("Refund CASH membutuhkan shift")
                cashLedger.insert(
                    PergerakanKasEntity(
                        shiftId = sid,
                        jenis = JenisPergerakanKas.RETUR,
                        jumlahDelta = -totalRefund,
                        transaksiId = transaction.id,
                        pengembalianId = returnId,
                        keterangan = "Refund retur transaksi ${transaction.nomorTransaksi}",
                        dibuatPada = request.now,
                    ),
                )
            }

            ReturnResult(returnId, totalRefund)
        }
    }
}