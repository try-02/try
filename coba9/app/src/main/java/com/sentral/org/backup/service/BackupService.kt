package com.sentral.org.backup.service

import android.content.Context
import android.net.Uri
import com.sentral.org.backup.crypto.BackupCryptoEngine
import com.sentral.org.backup.db.DatabaseSnapshotter
import com.sentral.org.backup.db.DatabaseValidator
import com.sentral.org.backup.model.BackupMetadata
import com.sentral.org.backup.model.BackupState
import com.sentral.org.backup.model.PosBackupException
import com.sentral.org.data.concurrency.PosExecutionLock
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupService(
    private val context: Context,
    private val snapshotter: DatabaseSnapshotter,
    private val validator: DatabaseValidator,
    private val crypto: BackupCryptoEngine,
    private val lock: PosExecutionLock,
) {
    suspend fun performBackup(
        destinationUri: Uri,
        password: String,
        note: String = "",
        onProgress: (BackupState) -> Unit,
    ): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        runCatching {
            if (password.length < 6) {
                throw IllegalArgumentException("Password backup minimal 6 karakter")
            }

            onProgress(BackupState.CheckingLock)
            lock.withBackupLock(failIfBusy = true) {
                val timestamp = System.currentTimeMillis()
                val stagingFile = File(context.cacheDir, "staging_snapshot_$timestamp.db")

                try {
                    onProgress(BackupState.CreatingSnapshot)
                    snapshotter.snapshotTo(stagingFile)

                    onProgress(BackupState.Verifying)
                    validator.verifyIntegrity(stagingFile)
                    val stats = validator.verifySchemaAndGetStats(stagingFile)

                    val metadata = BackupMetadata(
                        appVersion = "1.0.0",
                        schemaVersion = 1,
                        schemaIdentityHash = stats.identityHash,
                        createdAt = timestamp,
                        totalTransactions = stats.totalTransactions,
                        totalProducts = stats.totalProducts,
                        totalCashiers = stats.totalCashiers,
                        totalShifts = stats.totalShifts,
                        note = note,
                    )

                    onProgress(BackupState.Encrypting)
                    context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                        crypto.encrypt(
                            sourceDbFile = stagingFile,
                            outputStream = output,
                            password = password.toCharArray(),
                            metadata = metadata,
                        )
                    } ?: throw PosBackupException.StorageError("Tidak dapat menulis ke URI tujuan")

                    val fileSize = try {
                        context.contentResolver.openFileDescriptor(destinationUri, "r")?.use { it.statSize } ?: 0L
                    } catch (_: Exception) { 0L }

                    onProgress(BackupState.Success(
                        fileName = "POS-Backup-$timestamp.posbak",
                        sizeBytes = fileSize,
                        metadata = metadata,
                    ))

                    metadata
                } finally {
                    if (stagingFile.exists()) stagingFile.delete()
                }
            }
        }
    }
}