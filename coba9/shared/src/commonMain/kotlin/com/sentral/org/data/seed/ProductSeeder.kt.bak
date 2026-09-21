package com.sentral.org.data.seed

import androidx.room3.withWriteTransaction   // API Room 3.x pengganti withTransaction
import com.sentral.org.data.PosDatabase
import com.sentral.org.shared.currentTimeMillis

class ProductSeeder(private val db: PosDatabase) {

    suspend fun seedIfEmpty() {
        // Fast-path read tanpa lock transaksi SQLite: hemat waktu booting startup
        if (db.produkDao().count() > 0) return

        val waktu = currentTimeMillis()
        val items = SeedProduct.getDefaultItems()

        db.withWriteTransaction {
            // Double-check di dalam write lock untuk mencegah race-condition
            if (db.produkDao().count() > 0) return@withWriteTransaction

            val ids = db.produkDao().insertAll(SeedProduct.toProdukEntities(items, waktu))
            check(ids.all { it != -1L }) { "Seed produk gagal: ada SKU/barcode duplikat" }

            db.persediaanDao().insertAll(SeedProduct.toPersediaanEntities(ids, items, waktu))
            db.pergerakanPersediaanDao().insertAll(SeedProduct.toPergerakanEntities(ids, items, waktu))
        }
    }
}