package com.sentral.org.data

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/**
 * Android-specific: bangun PosDatabase dari [Context].
 * Dipanggil dari app (Android entry point) lewat Koin `androidContext()`.
 */
fun createAndroidPosDatabase(context: Context): PosDatabase =
    createPosDatabase(
        Room.databaseBuilder<PosDatabase>(context, "pos.db")
            .setDriver(BundledSQLiteDriver())
    )
