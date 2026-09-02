package com.sentral.org.data

import androidx.room3.RoomDatabase
import kotlinx.coroutines.Dispatchers

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
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
