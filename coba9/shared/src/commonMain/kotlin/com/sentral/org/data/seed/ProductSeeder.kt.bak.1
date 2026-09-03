package com.sentral.org.data.seed

import androidx.room3.withWriteTransaction   // API Room 3.x pengganti withTransaction
import com.sentral.org.data.PosDatabase

class ProductSeeder(private val db: PosDatabase) {

    suspend fun seedIfEmpty() {
        val waktu = System.currentTimeMillis()
        val items = SeedProduct.getDefaultItems()

        db.withWriteTransaction {
            if (db.produkDao().count() > 0) return@withWriteTransaction

            // insertAll mengembalikan id (-1 jika conflict IGNORE terjadi)
            val ids = db.produkDao().insertAll(SeedProduct.toProdukEntities(items, waktu))
            check(ids.all { it != -1L }) { "Seed produk gagal: ada SKU/barcode duplikat" }

            db.persediaanDao().insertAll(SeedProduct.toPersediaanEntities(ids, items, waktu))
            db.pergerakanPersediaanDao().insertAll(SeedProduct.toPergerakanEntities(ids, items, waktu))
        }
    }
}