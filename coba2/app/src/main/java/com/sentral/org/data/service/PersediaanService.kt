package com.sentral.org.data.service

import android.database.sqlite.SQLiteConstraintException
import com.sentral.org.data.dao.PergerakanPersediaanDao
import com.sentral.org.data.dao.PersediaanDao
import com.sentral.org.data.dao.ProdukDao
import com.sentral.org.data.entity.PergerakanPersediaanEntity
import com.sentral.org.data.entity.PersediaanEntity
import com.sentral.org.data.model.JenisPergerakanPersediaan
import com.sentral.org.data.model.PosDataException
import com.sentral.org.data.model.suspendRunCatching

class PersediaanService(
    private val write: PosWriteService,
    private val products: ProdukDao,
    private val stock: PersediaanDao,
    private val ledger: PergerakanPersediaanDao,
) {
    suspend fun createForProduct(
        productId: Long,
        initialQuantity: Long,
        initialDamaged: Long,
        now: Long,
    ): Result<Unit> = suspendRunCatching {
        require(initialQuantity >= 0) { "Stok awal tidak boleh negatif" }
        require(initialDamaged >= 0) { "Stok rusak awal tidak boleh negatif" }
        write.run {
            products.getById(productId)
                ?: throw PosDataException.NotFound("Produk tidak ditemukan")
            try {
                stock.insert(
                    PersediaanEntity(
                        produkId = productId,
                        jumlah = initialQuantity,
                        jumlahRusak = initialDamaged,
                        diperbaruiPada = now,
                    )
                )
            } catch (e: SQLiteConstraintException) {
                throw PosDataException.Duplicate("Produk $productId sudah memiliki baris persediaan")
            }
            if (initialQuantity != 0L || initialDamaged != 0L) {
                ledger.insert(
                    PergerakanPersediaanEntity(
                        produkId = productId,
                        jenis = JenisPergerakanPersediaan.STOK_AWAL,
                        perubahanJumlah = initialQuantity,
                        perubahanJumlahRusak = initialDamaged,
                        saldoJumlahSebelum = 0,
                        saldoJumlahSetelah = initialQuantity,
                        saldoRusakSebelum = 0,
                        saldoRusakSetelah = initialDamaged,
                        transaksiId = null,
                        itemTransaksiId = null,
                        pengembalianId = null,
                        itemPengembalianId = null,
                        shiftId = null,
                        keterangan = "Stok awal",
                        dibuatPada = now,
                    )
                )
            }
        }
    }
}