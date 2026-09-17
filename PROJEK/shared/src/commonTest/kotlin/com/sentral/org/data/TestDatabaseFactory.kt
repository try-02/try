package com.sentral.org.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/**
 * Helper untuk membuat In-Memory Database khusus Testing.
 * Menggunakan BundledSQLiteDriver() sehingga berjalan lancar di JVM/Host tanpa butuh Android emulator.
 */
fun createInMemoryPosDatabase(): PosDatabase {
    val builder = Room.inMemoryDatabaseBuilder<PosDatabase>()
        .setDriver(BundledSQLiteDriver())
    return createPosDatabase(builder)
}