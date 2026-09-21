package com.sentral.org.backup.db

import android.content.Context
import com.sentral.org.data.PosDatabase
import java.io.File
import java.io.IOException

class DatabaseFileSwap(
    private val context: Context,
    private val database: PosDatabase,
) {
    fun swap(decryptedStagingFile: File) {
        val dbFile = context.getDatabasePath("pos.db")
        val walFile = File(dbFile.parentFile, "pos.db-wal")
        val shmFile = File(dbFile.parentFile, "pos.db-shm")
        val emergencyBackup = File(dbFile.parentFile, "pos.db.bak")

        // 1. Tutup Room Database
        database.close()

        // 2. Buat backup darurat sebelum penimpaan
        if (dbFile.exists()) {
            if (emergencyBackup.exists()) emergencyBackup.delete()
            dbFile.copyTo(emergencyBackup, overwrite = true)
        }

        try {
            // 3. Bersihkan log WAL dan shared memory lama
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            // 4. Timpa file utama dengan staging yang sudah divalidasi
            if (dbFile.exists()) dbFile.delete()
            decryptedStagingFile.copyTo(dbFile, overwrite = true)

            // 5. Bersihkan staging dan backup darurat
            decryptedStagingFile.delete()
            if (emergencyBackup.exists()) emergencyBackup.delete()
        } catch (e: Exception) {
            // Rollback darurat
            if (emergencyBackup.exists()) {
                emergencyBackup.copyTo(dbFile, overwrite = true)
                emergencyBackup.delete()
            }
            throw IOException("Gagal menimpa berkas database: ${e.message}", e)
        }
    }
}