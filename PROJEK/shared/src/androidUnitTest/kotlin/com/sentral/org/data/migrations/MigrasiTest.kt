package com.sentral.org.data.migrations

import androidx.room3.testing.testing.createTestDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sentral.org.data.PosDatabase
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Template test migration. Jalankan setiap kali menambah MIGRATION_X_Y baru.
 * 
 * POLA TEST:
 * 1. Buat DB di versi awal (startVersion)
 * 2. Isi dengan data dummy yang realistis
 * 3. Tutup DB
 * 4. Buka ulang dengan migration (Room otomatis jalankan MIGRATION_X_Y)
 * 5. Verifikasi: data tidak hilang + kolom baru punya default benar
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Contoh test migration 1 → 2 (aktifkan saat migration pertama dibuat):
     * 
     * @Test
     * fun migration1to2_menambahKolomPajak_defaultNol() = runBlocking {
     *     // 1. Buat DB versi 1 dengan data
     *     val dbV1 = createTestDatabase(
     *         context = context,
     *         name = "migration-test",
     *         factory = PosDatabaseConstructor::class.java,
     *         version = 1,
     *         driver = BundledSQLiteDriver(),
     *     )
     *     dbV1.produkDao().insert(ProdukEntity(...))
     *     dbV1.close()
     * 
     *     // 2. Buka ulang dengan migration
     *     val dbV2 = createTestDatabase(
     *         context = context,
     *         name = "migration-test",
     *         factory = PosDatabaseConstructor::class.java,
     *         version = 2,
     *         driver = BundledSQLiteDriver(),
     *         migrations = listOf(MIGRATION_1_2),
     *     )
     * 
     *     // 3. Verifikasi data bertahan + kolom baru ada
     *     val produk = dbV2.produkDao().getById(1L)
     *     assertEquals(0L, produk?.pajakPersen)
     *     dbV2.close()
     * }
     */

    @Test
    fun placeholder_migrationTestBelumDibutuhkan() {
        // Placeholder agar file test tidak kosong. Hapus saat migration pertama dibuat.
        assertEquals(0, PosMigrations.ALL.size)
    }
}