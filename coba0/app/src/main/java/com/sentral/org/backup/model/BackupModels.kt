package com.sentral.org.backup.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupMetadata(
    val appVersion: String,
    val schemaVersion: Int,
    val schemaIdentityHash: String,
    val createdAt: Long,
    val totalTransactions: Long,
    val totalProducts: Long,
    val totalCashiers: Long,
    val totalShifts: Long,
    val note: String = "",
)

data class PosBakHeader(
    val formatVersion: Int,
    val schemaVersion: Int,
    val createdAt: Long,
    val kdfIterations: Int,
    val salt: ByteArray,
    val iv: ByteArray,
    val metadataJson: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PosBakHeader) return false
        return formatVersion == other.formatVersion &&
            schemaVersion == other.schemaVersion &&
            createdAt == other.createdAt &&
            kdfIterations == other.kdfIterations &&
            salt.contentEquals(other.salt) &&
            iv.contentEquals(other.iv) &&
            metadataJson == other.metadataJson
    }

    override fun hashCode(): Int {
        var result = formatVersion
        result = 31 * result + schemaVersion
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + kdfIterations
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + metadataJson.hashCode()
        return result
    }
}

sealed interface BackupState {
    data object Idle : BackupState

    data object CheckingLock : BackupState

    data object CreatingSnapshot : BackupState

    data object Encrypting : BackupState

    data object Verifying : BackupState

    data class Success(
        val fileName: String,
        val sizeBytes: Long,
        val metadata: BackupMetadata,
    ) : BackupState

    data class Error(
        val message: String,
    ) : BackupState
}

sealed interface RestoreState {
    data object Idle : RestoreState

    data object DownloadingToStaging : RestoreState

    data object ValidatingHeader : RestoreState

    data object Decrypting : RestoreState

    data object CheckingIntegrity : RestoreState

    data object CheckingSchema : RestoreState

    data class PreviewReady(
        val metadata: BackupMetadata,
    ) : RestoreState

    data object ReplacingDatabase : RestoreState

    data class Success(
        val metadata: BackupMetadata,
    ) : RestoreState

    data class Error(
        val message: String,
    ) : RestoreState
}

data class BackupRecordInfo(
    val lastBackupTimestamp: Long?,
    val lastBackupFileName: String?,
    val lastBackupSizeBytes: Long?,
    val isHealthy: Boolean,
)
