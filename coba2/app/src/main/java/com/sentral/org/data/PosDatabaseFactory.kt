package com.sentral.org.data

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import androidx.room3.RoomDatabase

object PosDatabaseFactory {
    fun create(context: Context): PosDatabase =
        Room.databaseBuilder<PosDatabase>(context, "pos.db")
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            // Memisahkan jalur baca (UI) dan tulis (Kasir) agar Compose tidak freeze
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
}
