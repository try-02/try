package com.sentral.org.ui.screen.settings

import android.content.Context
import android.net.Uri
import com.sentral.org.backup.model.BackupMetadata
import com.sentral.org.backup.model.BackupRecordInfo
import com.sentral.org.backup.model.BackupState
import com.sentral.org.backup.model.RestoreState
import com.sentral.org.backup.service.BackupService
import com.sentral.org.backup.service.RestoreService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class BackupRestoreActions(
    private val backupService: BackupService,
    private val restoreService: RestoreService,
    private val scope: CoroutineScope,
    private val sendEvent: (BackupRestoreEvent) -> Unit,
    private val updateState: (BackupRestoreUiState) -> BackupRestoreUiState,
    private val saveLastBackupInfo: (Long, String) -> Unit,
    private val loadLastBackupInfo: () -> Unit,
    private val restartApp: () -> Unit,
) {

    fun eksekusiBackupKeUri(uri: Uri, password: String, note: String) {
        scope.launch {
            updateState { it.copy(sedangMemproses = true, statusPesan = "Memulai backup...") }

            backupService.performBackup(uri, password, note) { state ->
                val pesan = when (state) {
                    BackupState.CheckingLock -> "Memeriksa kunci sistem..."
                    BackupState.CreatingSnapshot -> "Membuat snapshot VACUUM INTO..."
                    BackupState.Verifying -> "Memverifikasi integritas SQLite..."
                    BackupState.Encrypting -> "Mengenkripsi dengan AES-256-GCM..."
                    is BackupState.Success -> "Backup berhasil disimpan"
                    is BackupState.Error -> "Error: ${state.message}"
                    BackupState.Idle -> ""
                }
                updateState { it.copy(statusPesan = pesan) }
            }.fold(
                onSuccess = { meta ->
                    val now = System.currentTimeMillis()
                    saveLastBackupInfo(now, "POS-Backup-$now.posbak")
                    loadLastBackupInfo()
                    updateState { it.copy(sedangMemproses = false) }
                    sendEvent(BackupRestoreEvent.Pesan("Backup berhasil dibuat dan diverifikasi"))
                },
                onFailure = { err ->
                    updateState { it.copy(sedangMemproses = false) }
                    sendEvent(BackupRestoreEvent.Pesan(err.message ?: "Gagal membuat backup", isError = true))
                },
            )
        }
    }

    fun verifikasiDanBukaPreviewRestore(uri: Uri, password: String) {
        scope.launch {
            updateState {
                it.copy(
                    sedangMemproses = true,
                    dialogPasswordRestoreTerbuka = false,
                    statusPesan = "Mendekripsi dan memvalidasi...",
                )
            }

            restoreService.inspectAndStage(uri, password) { state ->
                val pesan = when (state) {
                    RestoreState.DownloadingToStaging -> "Mengunduh file ke cache..."
                    RestoreState.ValidatingHeader -> "Validasi header POSBAK..."
                    RestoreState.Decrypting -> "Dekripsi AES-GCM..."
                    RestoreState.CheckingIntegrity -> "PRAGMA integrity_check..."
                    RestoreState.CheckingSchema -> "Validasi 15 tabel & hash..."
                    is RestoreState.PreviewReady -> "Verifikasi selesai"
                    else -> ""
                }
                updateState { it.copy(statusPesan = pesan) }
            }.fold(
                onSuccess = { (fileStaged, metadata) ->
                    updateState {
                        it.copy(
                            sedangMemproses = false,
                            dialogPreviewRestoreTerbuka = true,
                            previewMetadata = metadata,
                            stagedRestoreFile = fileStaged,
                        )
                    }
                },
                onFailure = { err ->
                    updateState { it.copy(sedangMemproses = false) }
                    sendEvent(BackupRestoreEvent.Pesan(err.message ?: "Validasi restore gagal", isError = true))
                },
            )
        }
    }

    fun komitRestoreDanMuatUlang(stagedFile: java.io.File) {
        scope.launch {
            updateState { it.copy(sedangMemproses = true, statusPesan = "Mengganti database...") }

            restoreService.commitRestore(stagedFile) { state ->
                if (state == RestoreState.ReplacingDatabase) {
                    updateState { it.copy(statusPesan = "Menimpa pos.db & membersihkan WAL...") }
                }
            }.fold(
                onSuccess = {
                    updateState { it.copy(sedangMemproses = false) }
                    sendEvent(BackupRestoreEvent.RestartAplikasi)
                    restartApp()
                },
                onFailure = { err ->
                    updateState { it.copy(sedangMemproses = false) }
                    sendEvent(BackupRestoreEvent.Pesan("Gagal menerapkan restore: ${err.message}", isError = true))
                },
            )
        }
    }
}