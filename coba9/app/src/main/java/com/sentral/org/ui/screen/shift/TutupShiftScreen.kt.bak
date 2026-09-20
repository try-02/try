package com.sentral.org.ui.screen.shift

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentral.org.data.model.ShiftSummary
import com.sentral.org.ui.screen.pos.formatRupiah
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutupShiftScreen(
    onBack: () -> Unit,
    onShiftDitutup: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TutupShiftViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                TutupShiftEvent.ShiftSelesaiDanKeluar -> onShiftDitutup()
                is TutupShiftEvent.Pesan -> snackbarHostState.showSnackbar(event.teks)
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
                        if (uiState.step == TutupShiftStep.BLIND_COUNT) "Penghitungan Kas Laci" else "Rekonsiliasi Shift",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (uiState.step == TutupShiftStep.BLIND_COUNT) {
                        OutlinedButton(
                            onClick = { viewModel.cetakLaporanX() },
                            enabled = !uiState.sedangCetak,
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            if (uiState.sedangCetak) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            } else {
                                Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Laporan X", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (uiState.step) {
                TutupShiftStep.BLIND_COUNT -> {
                    PanelBlindCount(
                        namaKasir = uiState.namaKasir,
                        dimulaiPada = uiState.dimulaiPada,
                        kasAktualNominal = uiState.kasAktualNominal,
                        pesanError = uiState.pesanError,
                        sedangMemproses = uiState.sedangMemproses,
                        onTekan = { viewModel.tekanAngka(it) },
                        onHapus = { viewModel.hapusDigit() },
                        onVerifikasi = { viewModel.hitungRekonsiliasi() },
                    )
                }
                TutupShiftStep.REKONSILIASI -> {
                    uiState.summary?.let { summary ->
                        PanelRekonsiliasi(
                            summary = summary,
                            catatan = uiState.catatan,
                            onCatatanChange = { viewModel.setCatatan(it) },
                            sedangMemproses = uiState.sedangMemproses,
                            onHitungUlang = { viewModel.hitungUlangFisik() },
                            onKonfirmasiTutup = { viewModel.konfirmasiTutupShift() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.PanelBlindCount(
    namaKasir: String,
    dimulaiPada: Long,
    kasAktualNominal: Long,
    pesanError: String?,
    sedangMemproses: Boolean,
    onTekan: (String) -> Unit,
    onHapus: () -> Unit,
    onVerifikasi: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val jamMulai = remember(dimulaiPada) { if (dimulaiPada > 0) dateFormat.format(Date(dimulaiPada)) else "-" }

    Spacer(Modifier.height(8.dp))

    // Banner Informasi Blind Count
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp),
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "Shift Kasir: $namaKasir (Mulai $jamMulai)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    "Hitung seluruh uang fisik di laci kasir, lalu masukkan nominal totalnya.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    // Display Kas Fisik
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 20.dp),
        ) {
            Text(
                "TOTAL UANG FISIK DI LACI",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                formatRupiah(kasAktualNominal),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    AnimatedVisibility(visible = pesanError != null) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            Text(
                text = pesanError.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
    }

    // Numpad
    NumpadTutupShift(
        onTekan = onTekan,
        onHapus = onHapus,
    )

    Spacer(Modifier.weight(1f))

    Button(
        onClick = onVerifikasi,
        enabled = !sedangMemproses,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        if (sedangMemproses) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text("Verifikasi & Hitung Selisih", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun ColumnScope.PanelRekonsiliasi(
    summary: ShiftSummary,
    catatan: String,
    onCatatanChange: (String) -> Unit,
    sedangMemproses: Boolean,
    onHitungUlang: () -> Unit,
    onKonfirmasiTutup: () -> Unit,
) {
    val selisih = summary.selisihKas ?: 0L

    Spacer(Modifier.height(8.dp))

    // Kartu Hasil Selisih
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = when {
            selisih == 0L -> MaterialTheme.colorScheme.primaryContainer
            selisih < 0L -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.tertiaryContainer
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp),
        ) {
            Icon(
                imageVector = when {
                    selisih == 0L -> Icons.Filled.CheckCircle
                    selisih < 0L -> Icons.Filled.Warning
                    else -> Icons.Filled.Info
                },
                contentDescription = null,
                tint = when {
                    selisih == 0L -> MaterialTheme.colorScheme.onPrimaryContainer
                    selisih < 0L -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onTertiaryContainer
                },
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    selisih == 0L -> "KAS COCOK (PAS)"
                    selisih < 0L -> "KAS KURANG (SHORT)"
                    else -> "KAS LEBIH (OVER)"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
            )
            Text(
                formatRupiah(selisih),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    // Rincian Kalkulasi
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            BarisRincian("Modal Kas Awal", formatRupiah(summary.kasAwal))
            BarisRincian("Penjualan Tunai", "+${formatRupiah(summary.totalPenjualanTunai)}")
            if (summary.totalReturTunai > 0) {
                BarisRincian("Refund Retur Tunai", "-${formatRupiah(summary.totalReturTunai)}")
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 6.dp))
            BarisRincian("Kas Diharapkan Sistem", formatRupiah(summary.kasDiharapkan), tebal = true)
            BarisRincian("Uang Fisik Dihitung", formatRupiah(summary.kasAktual ?: 0L), tebal = true)
        }
    }

    Spacer(Modifier.height(12.dp))

    // Catatan Penutupan Shift
    OutlinedTextField(
        value = catatan,
        onValueChange = onCatatanChange,
        label = { Text("Catatan Penutupan (Alasan selisih jika ada)") },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        singleLine = true,
    )

    Spacer(Modifier.weight(1f))

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedButton(
            onClick = onHitungUlang,
            enabled = !sedangMemproses,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text("Hitung Ulang")
        }

        Button(
            onClick = onKonfirmasiTutup,
            enabled = !sedangMemproses,
            modifier = Modifier
                .weight(1.5f)
                .height(52.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            if (sedangMemproses) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Tutup Shift & Z Report", fontWeight = FontWeight.Bold)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun BarisRincian(label: String, nilai: String, tebal: Boolean = false) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            label,
            style = if (tebal) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (tebal) FontWeight.Bold else FontWeight.Normal,
        )
        Text(
            nilai,
            style = if (tebal) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (tebal) FontWeight.Bold else FontWeight.SemiBold,
        )
    }
}

@Composable
private fun NumpadTutupShift(
    onTekan: (String) -> Unit,
    onHapus: () -> Unit,
) {
    val tombolBaris = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("000", "0", "⌫"),
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        tombolBaris.forEach { baris ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                baris.forEach { t ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (t == "⌫") MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        border = if (t != "⌫") BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clickable {
                                if (t == "⌫") onHapus() else onTekan(t)
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (t == "⌫") {
                                Icon(
                                    Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Hapus",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            } else {
                                Text(
                                    t,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}