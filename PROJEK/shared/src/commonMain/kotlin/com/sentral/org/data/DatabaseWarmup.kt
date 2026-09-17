package com.sentral.org.data

import com.sentral.org.data.seed.ProductSeeder
import com.sentral.org.data.session.DevSessionBootstrap

/**
 * Memaksa Room membuka koneksi SQLite sejak awal lewat satu query riil,
 * lalu mengisi data awal (seed) jika database masih kosong.
 */
class DatabaseWarmup(
    private val database: PosDatabase,
    private val seeder: ProductSeeder,
    private val devSession: DevSessionBootstrap,   // ← SEMENTARA (dev)
) {
    suspend fun warm() {
        database.profilTokoDao().get()   // buka koneksi (perilaku lama dipertahankan)
        seeder.seedIfEmpty()             // atomik: produk + stok + ledger dalam 1 transaksi
        devSession.pastikanAdaSesi()    // ← HAPUS saat login/shift jadi
    }
}