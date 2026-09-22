package com.sentral.org.data

import com.sentral.org.data.seed.ProductSeeder

/**
 * Memaksa Room membuka koneksi SQLite sejak awal lewat satu query riil,
 * lalu mengisi data awal (seed) jika database masih kosong.
 */
class DatabaseWarmup(
    private val database: PosDatabase,
    private val seeder: ProductSeeder,
) {
    suspend fun warm() {
        database.profilTokoDao().get() // buka koneksi
        seeder.seedIfEmpty() // atomik: produk + stok + ledger dalam 1 transaksi
    }
}
