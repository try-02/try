package com.sentral.org.ui.screen.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentral.org.backup.model.BackupMetadata
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupRestoreViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val createDocLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream"),
        ) { uri: Uri? ->
            uri?.let { viewModel.onIntent(BackupRestoreIntent.EksekusiBackupKeUri(it)) }
        }

    val openDocLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri: Uri? ->
            uri?.let { viewModel.onIntent(BackupRestoreIntent.OnFileBackupDipilih(it)) }
        }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is BackupRestoreEvent.Pesan -> {
                    snackbarHostState.showSnackbar(event.teks)
                }

                BackupRestoreEvent.MintaPilihLokasiExport -> {
                    val timeStamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date())
                    createDocLauncher.launch("POS-Backup-$timeStamp.posbak")
                }

                BackupRestoreEvent.MintaPilihFileImport -> {
                    openDocLauncher.launch(arrayOf("*/*"))
                }

                BackupRestoreEvent.RestartAplikasi -> {}
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = {
                    Text(
                        "Cadangan & Pemulihan Data",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            // Kartu Ringkasan Status Backup
            KartuStatusBackup(uiState.recordInfo.lastBackupTimestamp, uiState.recordInfo.lastBackupFileName)

            Spacer(Modifier.height(20.dp))

            // Progress Banner
            AnimatedVisibility(visible = uiState.sedangMemproses) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(
                            uiState.statusPesan,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // Tombol Aksi Backup
            Button(
                onClick = { viewModel.onIntent(BackupRestoreIntent.BukaDialogBackup) },
                enabled = !uiState.sedangMemproses,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cadangkan Sekarang (.posbak)", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))

            // Tombol Aksi Restore
            OutlinedButton(
                onClick = { viewModel.onIntent(BackupRestoreIntent.MulaiProsesRestorePilihFile) },
                enabled = !uiState.sedangMemproses,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Pulihkan dari File Cadangan", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))

            // Informasi Keamanan
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Filled.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Berkas cadangan diamankan dengan algoritma AES-256-GCM (PBKDF2 100.000 iterasi). File dapat dipindahkan dan dipulihkan ke tablet/ponsel baru.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    // Dialog Input Password Backup
    if (uiState.dialogBackupTerbuka) {
        DialogPasswordBackup(
            password = uiState.passwordInput,
            catatan = uiState.catatanInput,
            pesanError = uiState.pesanError,
            onPasswordChange = { viewModel.onIntent(BackupRestoreIntent.SetPassword(it)) },
            onCatatanChange = { viewModel.onIntent(BackupRestoreIntent.SetCatatan(it)) },
            onKonfirmasi = { viewModel.onIntent(BackupRestoreIntent.KonfirmasiSiapExport) },
            onDismiss = { viewModel.onIntent(BackupRestoreIntent.TutupDialogBackup) },
        )
    }

    // Dialog Password Dekripsi Restore
    if (uiState.dialogPasswordRestoreTerbuka) {
        DialogPasswordRestore(
            password = uiState.passwordInput,
            pesanError = uiState.pesanError,
            onPasswordChange = { viewModel.onIntent(BackupRestoreIntent.SetPassword(it)) },
            onKonfirmasi = { viewModel.onIntent(BackupRestoreIntent.VerifikasiDanBukaPreviewRestore) },
            onDismiss = { viewModel.onIntent(BackupRestoreIntent.TutupDialogPasswordRestore) },
        )
    }

    // Dialog Preview Data Restore
    if (uiState.dialogPreviewRestoreTerbuka) {
        uiState.previewMetadata?.let { meta ->
            DialogPreviewRestore(
                metadata = meta,
                onKonfirmasi = { viewModel.onIntent(BackupRestoreIntent.KomitRestoreDanMuatUlang) },
                onBatal = { viewModel.onIntent(BackupRestoreIntent.BatalPreviewRestore) },
            )
        }
    }
}

@Composable
private fun KartuStatusBackup(
    lastTs: Long?,
    fileName: String?,
) {
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()) }
    val waktuStr = remember(lastTs) { lastTs?.let { dateFormat.format(Date(it)) } ?: "Belum pernah" }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(20.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Backup,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    "Cadangan Terakhir",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    waktuStr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!fileName.isNullOrBlank()) {
                    Text(
                        fileName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogPasswordBackup(
    password: String,
    catatan: String,
    pesanError: String?,
    onPasswordChange: (String) -> Unit,
    onCatatanChange: (String) -> Unit,
    onKonfirmasi: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kunci Cadangan Data", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Masukkan password untuk mengenkripsi berkas cadangan. Simpan password ini untuk memulihkan data pada perangkat lain.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password Cadangan (Min. 6 Karakter)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = catatan,
                    onValueChange = onCatatanChange,
                    label = { Text("Catatan Cadangan (Opsional)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (pesanError != null) {
                    Text(pesanError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = onKonfirmasi, shape = MaterialTheme.shapes.medium) {
                Text("Lanjutkan ke Penyimpanan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
    )
}

@Composable
private fun DialogPasswordRestore(
    password: String,
    pesanError: String?,
    onPasswordChange: (String) -> Unit,
    onKonfirmasi: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Masukkan Password Cadangan", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Masukkan password yang digunakan saat membuat berkas .posbak ini untuk didekripsi dan diverifikasi.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password Cadangan") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (pesanError != null) {
                    Text(pesanError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = onKonfirmasi, shape = MaterialTheme.shapes.medium) {
                Text("Dekripsi & Verifikasi", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
    )
}

@Composable
private fun DialogPreviewRestore(
    metadata: BackupMetadata,
    onKonfirmasi: () -> Unit,
    onBatal: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val tglStr = remember(metadata.createdAt) { dateFormat.format(Date(metadata.createdAt)) }

    AlertDialog(
        onDismissRequest = onBatal,
        title = { Text("Verifikasi Berhasil: Terapkan Restore?", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Berkas cadangan valid dan lolos uji integritas. Database saat ini akan digantikan secara penuh:",
                    style = MaterialTheme.typography.bodySmall,
                )
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Dibuat: $tglStr", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                        Text("Transaksi: ${metadata.totalTransactions}", style = MaterialTheme.typography.bodySmall)
                        Text("Produk: ${metadata.totalProducts}", style = MaterialTheme.typography.bodySmall)
                        Text("Kasir: ${metadata.totalCashiers}", style = MaterialTheme.typography.bodySmall)
                        Text("Shift: ${metadata.totalShifts}", style = MaterialTheme.typography.bodySmall)
                        if (metadata.note.isNotBlank()) {
                            Text(
                                "Catatan: ${metadata.note}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                Text(
                    "Peringatan: Aplikasi akan memuat ulang (restart) secara otomatis setelah penimpaan file database.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onKonfirmasi,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Text("Ganti Database & Restart", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onBatal) { Text("Batal") }
        },
    )
}
