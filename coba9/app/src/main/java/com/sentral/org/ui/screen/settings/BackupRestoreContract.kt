package com.sentral.org.ui.screen.settings

import com.sentral.org.backup.model.BackupMetadata
import com.sentral.org.backup.model.BackupRecordInfo
import java.io.File

data class BackupRestoreUiState(
    val recordInfo: BackupRecordInfo = BackupRecordInfo(null, null, null, true),
    val sedangMemproses: Boolean = false,
    val statusPesan: String = "",
    val dialogBackupTerbuka: Boolean = false,
    val dialogPasswordRestoreTerbuka: Boolean = false,
    val dialogPreviewRestoreTerbuka: Boolean = false,
    val previewMetadata: BackupMetadata? = null,
    val stagedRestoreFile: File? = null,
    val passwordInput: String = "",
    val catatanInput: String = "",
    val pesanError: String? = null,
)

sealed interface BackupRestoreEvent {
    data class Pesan(val teks: String, val isError: Boolean = false) : BackupRestoreEvent
    data object MintaPilihLokasiExport : BackupRestoreEvent
    data object MintaPilihFileImport : BackupRestoreEvent
    data object RestartAplikasi : BackupRestoreEvent
}