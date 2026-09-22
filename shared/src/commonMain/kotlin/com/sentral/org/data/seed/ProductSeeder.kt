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
        val items = SeedProduct.getDefaultItems()

        write.run {
            if (db.produkDao().count() > 0) return@run

            val ids = db.produkDao().insertAll(SeedProduct.toProdukEntities(items, waktu))
            check(ids.all { it != -1L }) { "Seed produk gagal: ada SKU/barcode duplikat" }

            db.persediaanDao().insertAll(SeedProduct.toPersediaanEntities(ids, items, waktu))
            db.pergerakanPersediaanDao().insertAll(SeedProduct.toPergerakanEntities(ids, items, waktu))
        }
    }
}