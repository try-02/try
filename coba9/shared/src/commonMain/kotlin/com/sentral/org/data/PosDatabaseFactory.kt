package com.sentral.org.data

import androidx.room3.RoomDatabase
import kotlinx.coroutines.Dispatchers
import androidx.room3.migration.Migration
import com.sentral.org.data.migrasi.PosMigrasi
/**
 * Membangun instance PosDatabase dari [RoomDatabase.Builder] yang sudah
 * dikonfigurasi driver-nya di platform source set (androidMain / iosMain).
 *
 * Konfigurasi cross-platform (query context) dilakukan di sini (common),
 * sedangkan [Room.databaseBuilder] + [setDriver] ada di platform karena
 * signature berbeda: Android butuh Context, iOS butuh file path.
 *
 * WAL adalah default RoomDatabase.Builder di Room 3.0
 * (RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING), jadi setJournalMode
 * tidak perlu diset ulang.
 */
fun createPosDatabase(builder: RoomDatabase.Builder<PosDatabase>): PosDatabase =
    builder
        .setQueryCoroutineContext(Dispatchers.Default)
        .addMigrations(*PosMigrasi.ALL.toTypedArray())
        // JANGAN PERNAH pakai fallbackToDestructiveMigration() di aplikasi POS!
        // Sebagai gantinya, kita pakai fallback yang aman: backup DB lama + 
        // buat DB baru jika migration gagal. Ini mencegah data loss total.
        .setMigrationFallback { connection: SQLiteConnection ->
            // TODO (tahap hardening): implement backup otomatis DB lama
            // sebelum membuat DB baru. Sementara biarkan throw agar developer
            // sadar ada migration path yang hilang.
            throw IllegalStateException(
                "Migration path tidak ditemukan. Ini adalah bug developer, " +
                "bukan error user. Tambahkan migration di PosMigrations.ALL"
            )
        }
        .build()