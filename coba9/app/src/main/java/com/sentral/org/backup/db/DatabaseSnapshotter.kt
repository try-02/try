package com.sentral.org.backup.db

import androidx.room3.execSQL
import androidx.room3.useWriterConnection
import com.sentral.org.data.PosDatabase
import java.io.File

class DatabaseSnapshotter(
    private val database: PosDatabase,
) {
    suspend fun snapshotTo(destinationFile: File) {
        if (destinationFile.exists()) {
            destinationFile.delete()
        }
        val pathEscaped = destinationFile.absolutePath.replace("'", "''")
        database.useWriterConnection { transactor ->
            transactor.execSQL("VACUUM INTO '$pathEscaped'")
        }
    }
}