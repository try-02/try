package com.sentral.org.backup.model

sealed class PosBackupException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class InvalidFormat(
        message: String,
    ) : PosBackupException(message)

    class UnsupportedVersion(
        val version: Int,
    ) : PosBackupException("Versi format backup ($version) tidak didukung aplikasi ini")

    class WrongPasswordOrCorrupted(
        cause: Throwable? = null,
    ) : PosBackupException("Password backup salah atau berkas backup rusak", cause)

    class SQLiteIntegrityFailed(
        val detail: String,
    ) : PosBackupException("Integritas SQLite gagal: $detail")

    class IncompatibleSchema(
        val detail: String,
    ) : PosBackupException("Skema database backup tidak kompatibel: $detail")

    class SystemBusy(
        message: String,
    ) : PosBackupException(message)

    class StorageError(
        message: String,
        cause: Throwable? = null,
    ) : PosBackupException(message, cause)
}
