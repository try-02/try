package com.sentral.org.ui.screen.inventory

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentral.org.ui.screen.pos.scanner.BarcodeScannerOverlay
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormProdukScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FormProdukViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentOnBack by rememberUpdatedState(onBack)

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is FormProdukEvent.Pesan -> {
                    snackbarHostState.showSnackbar(event.teks)
                }

                FormProdukEvent.SimpanSukses -> {
                    currentOnBack()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = { Text(if (uiState.isEditMode) "Ubah Master Produk" else "Tambah Produk Baru", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = uiState.nama,
                onValueChange = { viewModel.onIntent(FormProdukIntent.SetNama(it)) },
                label = { Text("Nama Produk *") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            // Baris SKU dengan Tombol Auto-Generate
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = uiState.sku,
                    onValueChange = { viewModel.onIntent(FormProdukIntent.SetSku(it)) },
                    label = { Text("SKU Produk (Unik) *") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { viewModel.onIntent(FormProdukIntent.GenerateSkuOtomatis) },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.height(56.dp),
                ) {
                    Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Auto")
                }
            }

            // Baris Barcode dengan Tombol Scan Kamera
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = uiState.barcode,
                    onValueChange = { viewModel.onIntent(FormProdukIntent.SetBarcode(it)) },
                    label = { Text("Barcode (Opsional)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { viewModel.onIntent(FormProdukIntent.BukaScanner) },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.height(56.dp),
                ) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Scan")
                }
            }

            OutlinedTextField(
                value = uiState.kategori,
                onValueChange = { viewModel.onIntent(FormProdukIntent.SetKategori(it)) },
                label = { Text("Kategori") },
                placeholder = { Text("Contoh: Makanan, Minuman, Sembako") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = uiState.hargaJualInput,
                    onValueChange = { viewModel.onIntent(FormProdukIntent.SetHargaJual(it)) },
                    label = { Text("Harga Jual (Rp) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = uiState.hargaModalInput,
                    onValueChange = { viewModel.onIntent(FormProdukIntent.SetHargaModal(it)) },
                    label = { Text("Harga Modal / HPP (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )
            }

            if (!uiState.isEditMode) {
                OutlinedTextField(
                    value = uiState.stokAwalInput,
                    onValueChange = { viewModel.onIntent(FormProdukIntent.SetStokAwal(it)) },
                    label = { Text("Stok Awal (Unit / Kg)") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (uiState.pesanError != null) {
                Text(uiState.pesanError.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.onIntent(FormProdukIntent.SimpanProduk) },
                enabled = !uiState.sedangMenyimpan,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (uiState.sedangMenyimpan) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Simpan Data Produk", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (uiState.scannerTerbuka) {
        BarcodeScannerOverlay(
            onDismiss = { viewModel.onIntent(FormProdukIntent.TutupScanner) },
            onBarcodeScan = { barcode ->
                viewModel.onIntent(FormProdukIntent.OnBarcodeHasilScan(barcode))
                barcode
            },
        )
    }
}
