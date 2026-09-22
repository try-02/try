package com.sentral.org.ui.screen.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentral.org.backup.AppReloadHandler
import com.sentral.org.backup.model.BackupMetadata
import com.sentral.org.backup.model.BackupRecordInfo
import com.sentral.org.backup.model.BackupState
import com.sentral.org.backup.model.RestoreState
import com.sentral.org.backup.service.BackupService
import com.sentral.org.backup.service.RestoreService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class BackupRestoreViewModel(
    private val backupService: BackupService,
    private val restoreService: RestoreService,
    private val application: Application,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BackupRestoreUiState())
    val uiState: StateFlow<BackupRestoreUiState> = _uiState.asStateFlow()

    private val _event = Channel<BackupRestoreEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    private val prefs = application.getSharedPreferences("pos_backup_pref", Context.MODE_PRIVATE)
    private var pendingImportUri: Uri? = null

    init {
        muatInfoBackupTerakhir()
    }

    private fun muatInfoBackupTerakhir() {
        val ts = prefs.getLong("last_backup_ts", 0L).takeIf { it > 0 }
        val name = prefs.getString("last_backup_name", null)
        val size = prefs.getLong("last_backup_size", 0L).takeIf { it > 0 }
        _uiState.update {
            it.copy(
                recordInfo =
                    BackupRecordInfo(
                        lastBackupTimestamp = ts,
                        lastBackupFileName = name,
                        lastBackupSizeBytes = size,
                        isHealthy = true,
                    ),
            )
        }
    }

    fun bukaDialogBackup() {
        _uiState.update { it.copy(dialogBackupTerbuka = true, passwordInput = "", catatanInput = "", pesanError = null) }
    }

    fun tutupDialogBackup() {
        _uiState.update { it.copy(dialogBackupTerbuka = false) }
    }

    fun setPassword(pwd: String) {
        _uiState.update { it.copy(passwordInput = pwd) }
    }

    fun setCatatan(catatan: String) {
        _uiState.update { it.copy(catatanInput = catatan) }
    }

    fun konfirmasiSiapExport() {
        if (_uiState.value.passwordInput.length < 6) {
            _uiState.update { it.copy(pesanError = "Password minimal 6 karakter") }
            return
        }
        _uiState.update { it.copy(dialogBackupTerbuka = false) }
        viewModelScope.launch { _event.send(BackupRestoreEvent.MintaPilihLokasiExport) }
    }

    fun eksekusiBackupKeUri(uri: Uri) {
        val pwd = _uiState.value.passwordInput.trim()
        val note = _uiState.value.catatanInput.trim()

        viewModelScope.launch {
            _uiState.update { it.copy(sedangMemproses = true, statusPesan = "Memulai backup...") }

            backupService
                .performBackup(uri, pwd, note) { state ->
                    val pesan =
                        when (state) {
                            BackupState.CheckingLock -> "Memeriksa kunci sistem..."
                            BackupState.CreatingSnapshot -> "Membuat snapshot VACUUM INTO..."
                            BackupState.Verifying -> "Memverifikasi integritas SQLite..."
                            BackupState.Encrypting -> "Mengenkripsi dengan AES-256-GCM..."
                            is BackupState.Success -> "Backup berhasil disimpan"
                            is BackupState.Error -> "Error: ${state.message}"
                            BackupState.Idle -> ""
                        }
                    _uiState.update { it.copy(statusPesan = pesan) }
                }.fold(
                    onSuccess = { meta ->
                        val now = System.currentTimeMillis()
                        prefs.edit {
                            putLong("last_backup_ts", now)
                            putString("last_backup_name", "POS-Backup-$now.posbak")
                        }
                        muatInfoBackupTerakhir()
                        _uiState.update { it.copy(sedangMemproses = false) }
                        _event.send(BackupRestoreEvent.Pesan("Backup berhasil dibuat dan diverifikasi"))
                    },
                    onFailure = { err ->
                        _uiState.update { it.copy(sedangMemproses = false) }
                        _event.send(BackupRestoreEvent.Pesan(err.message ?: "Gagal membuat backup", isError = true))
                    },
                )
        }
    }

    fun mulaiProsesRestorePilihFile() {
        viewModelScope.launch { _event.send(BackupRestoreEvent.MintaPilihFileImport) }
    }

    fun onFileBackupDipilih(uri: Uri) {
        pendingImportUri = uri
        _uiState.update { it.copy(dialogPasswordRestoreTerbuka = true, passwordInput = "", pesanError = null) }
    }

    fun tutupDialogPasswordRestore() {
        _uiState.update { it.copy(dialogPasswordRestoreTerbuka = false) }
        pendingImportUri = null
    }

    fun verifikasiDanBukaPreviewRestore() {
        val uri = pendingImportUri ?: return
        val pwd = _uiState.value.passwordInput.trim()
        if (pwd.isBlank()) {
            _uiState.update { it.copy(pesanError = "Masukkan password backup") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    sedangMemproses = true,
                    dialogPasswordRestoreTerbuka = false,
                    statusPesan = "Mendekripsi dan memvalidasi...",
                )
            }

            restoreService
                .inspectAndStage(uri, pwd) { state ->
                    val pesan =
                        when (state) {
                            RestoreState.DownloadingToStaging -> "Mengunduh file ke cache..."
                            RestoreState.ValidatingHeader -> "Validasi header POSBAK..."
                            RestoreState.Decrypting -> "Dekripsi AES-GCM..."
                            RestoreState.CheckingIntegrity -> "PRAGMA integrity_check..."
                            RestoreState.CheckingSchema -> "Validasi 15 tabel & hash..."
                            is RestoreState.PreviewReady -> "Verifikasi selesai"
                            else -> ""
                        }
                    _uiState.update { it.copy(statusPesan = pesan) }
                }.fold(
                    onSuccess = { (fileStaged, metadata) ->
                        _uiState.update {
                            it.copy(
                                sedangMemproses = false,
                                dialogPreviewRestoreTerbuka = true,
                                previewMetadata = metadata,
                                stagedRestoreFile = fileStaged,
                            )
                        }
                    },
                    onFailure = { err ->
                        _uiState.update { it.copy(sedangMemproses = false) }
                        _event.send(BackupRestoreEvent.Pesan(err.message ?: "Validasi restore gagal", isError = true))
                    },
                )
        }
    }

    fun batalPreviewRestore() {
        _uiState.value.stagedRestoreFile?.delete()
        _uiState.update { it.copy(dialogPreviewRestoreTerbuka = false, stagedRestoreFile = null, previewMetadata = null) }
    }

    fun komitRestoreDanMuatUlang() {
        val staged = _uiState.value.stagedRestoreFile ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(sedangMemproses = true, statusPesan = "Mengganti database...") }

            restoreService
                .commitRestore(staged) { state ->
                    if (state == RestoreState.ReplacingDatabase) {
                        _uiState.update { it.copy(statusPesan = "Menimpa pos.db & membersihkan WAL...") }
                    }
                }.fold(
                    onSuccess = {
                        _uiState.update { it.copy(sedangMemproses = false) }
                        _event.send(BackupRestoreEvent.RestartAplikasi)
                        AppReloadHandler.restartApp(application)
                    },
                    onFailure = { err ->
                        _uiState.update { it.copy(sedangMemproses = false) }
                        _event.send(BackupRestoreEvent.Pesan("Gagal menerapkan restore: ${err.message}", isError = true))
                    },
                )
        }
    }
}
