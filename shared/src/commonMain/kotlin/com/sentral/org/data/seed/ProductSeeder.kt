package com.sentral.org.data.seed

import com.sentral.org.data.PosDatabase
import com.sentral.org.data.service.PosWriteService
import com.sentral.org.shared.currentTimeMillis

class ProductSeeder(
    private val db: PosDatabase,
    private val write: PosWriteService,
) {
    suspend fun seedIfEmpty() {
        if (db.produkDao().count() > 0) return

        val waktu = currentTimeMillis()
        val items = SeedProduct.getDummyItems()

        write.run {
            if (db.produkDao().count() > 0) return@run

            // Insert produk per-chunk agar ukuran batch tetap terkendali,
            // semua chunk tetap berjalan dalam SATU transaksi (atomik).
            val chunks = items.chunked(INSERT_CHUNK)
            val idsByChunk =
                chunks.map { chunk ->
                    val ids = db.produkDao().insertAll(SeedProduct.toProdukEntities(chunk, waktu))
                    check(ids.all { it != -1L }) { "Seed produk gagal: ada SKU/barcode duplikat" }
                    ids
                }

            chunks.forEachIndexed { i, chunk ->
                val ids = idsByChunk[i]
                db.persediaanDao().insertAll(SeedProduct.toPersediaanEntities(ids, chunk, waktu))
                db.pergerakanPersediaanDao().insertAll(SeedProduct.toPergerakanEntities(ids, chunk, waktu))
            }
        }
    }

    private companion object {
        const val INSERT_CHUNK = 1_000
    }
}
