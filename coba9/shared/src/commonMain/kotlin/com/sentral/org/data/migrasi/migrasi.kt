package com.sentral.org.data.migrasi

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * KUMPULAN MIGRASI DATABASE POS
 * 
 * ATURAN EMAS:
 * 1. JANGAN PERNAH hapus tabel/kolom yang berisi data transaksi (transaksi, 
 *    item_transaksi, pembayaran, pengembalian, pergerakan_kas, pergerakan_persediaan).
 *    Data keuangan = immutable (hanya boleh tambah, tidak boleh ubah/hapus).
 * 2. Kolom baru WAJIB punya DEFAULT VALUE agar tidak NULL saat migrasi.
 * 3. Setiap migration HARUS punya test di :shared:testAndroidHostTest.
 * 
 * VERSI SAAT INI: 1 (baseline)
 * Target selanjutnya: v2, v3, dst.
 */
object PosMigrasi {

    /**
     * Semua migration diurutkan dari versi terlama ke terbaru.
     * Room akan otomatis menjalankan chain yang dibutuhkan, misal:
     * user upgrade dari v1 ke v5 → Room jalankan MIGRATION_1_2, 2_3, 3_4, 4_5.
     */
    val ALL: List<Migration> = listOf(
        // MIGRATION_1_2,  // akan ditambahkan saat ada perubahan skema pertama
    )
}

/**
 * TEMPLATE MIGRASI (contoh saat nanti tambah kolom pajak di produk):
 * 
 * val MIGRATION_1_2 = object : Migration(1, 2) {
 *     override fun migrate(connection: SQLiteConnection) {
 *         connection.execSQL("""
 *             ALTER TABLE produk 
 *             ADD COLUMN pajak_persen INTEGER NOT NULL DEFAULT 0
 *         """.trimIndent())
 *     }
 * }
 * 
 * CATATAN PENTING:
 * - SQLite TIDAK mendukung DROP COLUMN (sebelum versi 3.35).
 * - Untuk hapus/ubah kolom, harus buat tabel baru + copy data + rename.
 * - Selalu test migration di :shared:testAndroidHostTest sebelum commit.
 */