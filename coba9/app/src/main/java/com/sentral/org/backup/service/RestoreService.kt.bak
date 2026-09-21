package com.sentral.org.backup.service

import android.content.Context
import android.net.Uri
import com.sentral.org.backup.crypto.BackupCryptoEngine
import com.sentral.org.backup.db.DatabaseFileSwap
import com.sentral.org.backup.db.DatabaseValidator
import com.sentral.org.backup.model.BackupMetadata
import com.sentral.org.backup.model.PosBackupException
import com.sentral.org.backup.model.RestoreState
import com.sentral.org.data.concurrency.PosExecutionLock
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RestoreService(
    private val context: Context,
    private val validator: DatabaseValidator,
    private val crypto: BackupCryptoEngine,
    private val fileSwap: DatabaseFileSwap,
    private val lock: PosExecutionLock,
) {
    suspend fun inspectAndStage(
        sourceUri: Uri,
        password: String,
        onProgress: (RestoreState) -> Unit,
    ): Result<Pair<File, BackupMetadata>> = withContext(Dispatchers.IO) {
        runCatching {
            val timestamp = System.currentTimeMillis()
            val tempEncrypted = File(context.cacheDir, "restore_enc_$timestamp.tmp")
            val tempDecrypted = File(context.cacheDir, "restore_staging_$timestamp.db")

            try {
                onProgress(RestoreState.DownloadingToStaging)
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    tempEncrypted.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: throw PosBackupException.StorageError("Tidak dapat membuka berkas backup dari URI")

                onProgress(RestoreState.ValidatingHeader)
                onProgress(RestoreState.Decrypting)
                val metadata = crypto.decrypt(
                    sourceEncryptedFile = tempEncrypted,
                    destinationDbFile = tempDecrypted,
                    password = password.toCharArray(),
                )

                onProgress(RestoreState.CheckingIntegrity)
                validator.verifyIntegrity(tempDecrypted)

                onProgress(RestoreState.CheckingSchema)
                validator.verifySchemaAndGetStats(tempDecrypted)

                onProgress(RestoreState.PreviewReady(metadata))
                Pair(tempDecrypted, metadata)
            } catch (e: Exception) {
                if (tempDecrypted.exists()) tempDecrypted.delete()
                throw e
            } finally {
                if (tempEncrypted.exists()) tempEncrypted.delete()
            }
        }
    }

    suspend fun commitRestore(
        stagedDbFile: File,
        onProgress: (RestoreState) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            lock.withRestoreLock {
                onProgress(RestoreState.ReplacingDatabase)
                fileSwap.swap(stagedDbFile)
            }
        }
    }
}