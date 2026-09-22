package com.sentral.org.ui.screen.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentral.org.backup.AppReloadHandler
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

sealed interface BackupRestoreIntent {
    object BukaDialogBackup : BackupRestoreIntent
    object TutupDialogBackup : BackupRestoreIntent
    data class SetPassword(val pwd: String) : BackupRestoreIntent
    data class SetCatatan(val catatan: String) : BackupRestoreIntent
    object KonfirmasiSiapExport : BackupRestoreIntent
    data class EksekusiBackupKeUri(val uri: Uri) : BackupRestoreIntent
    object MulaiProsesRestorePilihFile : BackupRestoreIntent
    data class OnFileBackupDipilih(val uri: Uri) : BackupRestoreIntent
    object TutupDialogPasswordRestore : BackupRestoreIntent
    object VerifikasiDanBukaPreviewRestore : BackupRestoreIntent
    object BatalPreviewRestore : BackupRestoreIntent
    object KomitRestoreDanMuatUlang : BackupRestoreIntent
}

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

    private val actions = BackupRestoreActions(
        backupService = backupService, restoreService = restoreService, scope = viewModelScope,
        sendEvent = { _event.send(it) }, updateState = { _uiState.update { it } },
        saveLastBackupInfo = { ts, name -> prefs.edit { putLong("last_backup_ts", ts); putString("last_backup_name", name) } },
        loadLastBackupInfo = { loadLastBackupInfo() }, restartApp = { AppReloadHandler.restartApp(application) },
    )

    init { loadLastBackupInfo() }

    private fun loadLastBackupInfo() {
        val ts = prefs.getLong("last_backup_ts", 0L).takeIf { it > 0 }
        val name = prefs.getString("last_backup_name", null)
        val size = prefs.getLong("last_backup_size", 0L).takeIf { it > 0 }
        _uiState.update { it.copy(recordInfo = BackupRecordInfo(lastBackupTimestamp = ts, lastBackupFileName = name, lastBackupSizeBytes = size, isHealthy = true)) }
    }

    fun onIntent(intent: BackupRestoreIntent) = when (intent) {
        BackupRestoreIntent.BukaDialogBackup -> _uiState.update { it.copy(dialogBackupTerbuka = true, passwordInput = "", catatanInput = "", pesanError = null) }
        BackupRestoreIntent.TutupDialogBackup -> _uiState.update { it.copy(dialogBackupTerbuka = false) }
        is BackupRestoreIntent.SetPassword -> _uiState.update { it.copy(passwordInput = intent.pwd) }
        is BackupRestoreIntent.SetCatatan -> _uiState.update { it.copy(catatanInput = intent.catatan) }
        BackupRestoreIntent.KonfirmasiSiapExport -> {
            if (_uiState.value.passwordInput.length < 6) { _uiState.update { it.copy(pesanError = "Password minimal 6 karakter") }; return@onIntent }
            _uiState.update { it.copy(dialogBackupTerbuka = false) }
            viewModelScope.launch { _event.send(BackupRestoreEvent.MintaPilihLokasiExport) }
        }
        is BackupRestoreIntent.EksekusiBackupKeUri -> actions.eksekusiBackupKeUri(intent.uri, _uiState.value.passwordInput.trim(), _uiState.value.catatanInput.trim())
        BackupRestoreIntent.MulaiProsesRestorePilihFile -> viewModelScope.launch { _event.send(BackupRestoreEvent.MintaPilihFileImport) }
        is BackupRestoreIntent.OnFileBackupDipilih -> { pendingImportUri = intent.uri; _uiState.update { it.copy(dialogPasswordRestoreTerbuka = true, passwordInput = "", pesanError = null) } }
        BackupRestoreIntent.TutupDialogPasswordRestore -> { _uiState.update { it.copy(dialogPasswordRestoreTerbuka = false) }; pendingImportUri = null }
        BackupRestoreIntent.VerifikasiDanBukaPreviewRestore -> { val uri = pendingImportUri ?: return@onIntent; actions.verifikasiDanBukaPreviewRestore(uri, _uiState.value.passwordInput.trim()) }
        BackupRestoreIntent.BatalPreviewRestore -> { _uiState.value.stagedRestoreFile?.delete(); _uiState.update { it.copy(dialogPreviewRestoreTerbuka = false, stagedRestoreFile = null, previewMetadata = null) } }
        BackupRestoreIntent.KomitRestoreDanMuatUlang -> _uiState.value.stagedRestoreFile?.let { actions.komitRestoreDanMuatUlang(it) }
    }
}