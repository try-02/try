package com.sentral.org.data.service

import com.sentral.org.data.dao.PergerakanPersediaanDao
import com.sentral.org.data.dao.PersediaanDao
import com.sentral.org.data.entity.PersediaanEntity
import com.sentral.org.data.entity.PergerakanPersediaanEntity
import com.sentral.org.data.model.JenisPergerakanPersediaan
import com.sentral.org.data.model.PosDataException

class InventoryMutationService(
    private val persediaanDao: PersediaanDao,
    private val ledgerDao: PergerakanPersediaanDao,
) {
    /**
     * Caller wajib berada di dalam transaksi tulis (PosWriteService.run).
     *
     * Auto-upsert: baris persediaan dibuat dengan saldo 0 bila belum ada.
     * Baris ledger delta-0 sengaja TIDAK ditulis agar audit trail hanya
     * berisi mutasi riil (mutasi aktual tetap mencatat saldo 0 -> X).
     *
     * @param allowNegativeStock default true demi menjaga perilaku backorder.
     *       Setel false pada jalur PENJUALAN bila bisnis melarang stok minus.
     */
    suspend fun mutateNormal(
        productId: Long,
        normalDelta: Long = 0,
        damagedDelta: Long = 0,
        type: JenisPergerakanPersediaan,
        allowNegativeStock: Boolean = true,
        transactionId: Long? = null,
        transactionItemId: Long? = null,
        returnId: Long? = null,
        returnItemId: Long? = null,
        shiftId: Long? = null,
        note: String = "",
        now: Long,
    ) {
        require(normalDelta != 0L || damagedDelta != 0L)

        var before = persediaanDao.getByProdukId(productId)
        if (before == null) {
            persediaanDao.insert(
                PersediaanEntity(
                    produkId = productId,
                    jumlah = 0,
                    jumlahRusak = 0,
                    diperbaruiPada = now,
                )
            )
            before = persediaanDao.getByProdukId(productId)
                ?: throw PosDataException.NotFound("Gagal inisialisasi persediaan produk $productId")
        }

        if (!allowNegativeStock && before.jumlah + normalDelta < 0) {
            throw PosDataException.InsufficientStock(
                "Stok produk $productId tidak cukup (sisa ${before.jumlah}, diminta ${-normalDelta})"
            )
        }

        if (normalDelta != 0L) {
            check(persediaanDao.addNormal(productId, normalDelta, now) == 1)
        }
        if (damagedDelta != 0L && persediaanDao.addDamaged(productId, damagedDelta, now) != 1) {
            throw PosDataException.InsufficientDamagedStock("Stok rusak tidak mencukupi untuk produk $productId")
        }

        val after = persediaanDao.getByProdukId(productId)
            ?: throw PosDataException.NotFound("Persediaan produk $productId hilang setelah mutasi")

        check(after.jumlah == before.jumlah + normalDelta)
        check(after.jumlahRusak == before.jumlahRusak + damagedDelta)

        ledgerDao.insert(
            PergerakanPersediaanEntity(
                produkId = productId,
                jenis = type,
                perubahanJumlah = normalDelta,
                perubahanJumlahRusak = damagedDelta,
                saldoJumlahSebelum = before.jumlah,
                saldoJumlahSetelah = after.jumlah,
                saldoRusakSebelum = before.jumlahRusak,
                saldoRusakSetelah = after.jumlahRusak,
                transaksiId = transactionId,
                itemTransaksiId = transactionItemId,
                pengembalianId = returnId,
                itemPengembalianId = returnItemId,
                shiftId = shiftId,
                keterangan = note,
                dibuatPada = now,
            )
        )
    }
}