package com.sentral.org.ui.screen.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentral.org.data.model.formatQuantity
import com.sentral.org.ui.screen.pos.formatRupiah
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KelolaProdukScreen(
    onBack: () -> Unit,
    onTambahProduk: () -> Unit,
    onEditProduk: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: KelolaProdukViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is KelolaProdukEvent.Pesan -> snackbarHostState.showSnackbar(event.teks)
                is KelolaProdukEvent.NavigasiKeForm -> onEditProduk(event.produkId ?: 0L)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = { Text("Katalog & Stok Produk", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onTambahProduk,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Tambah Produk", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { viewModel.setQuery(it) },
                placeholder = { Text("Cari nama, SKU, barcode...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Hapus")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )

            // Status Filter Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                StatusStokFilter.entries.forEach { s ->
                    FilterChip(
                        selected = uiState.filterStatus == s,
                        onClick = { viewModel.setFilterStatus(s) },
                        label = { Text(s.label) },
                        shape = MaterialTheme.shapes.small,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 4.dp))

            if (uiState.sedangMemuat && uiState.daftarProduk.isEmpty()) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.daftarProduk.isEmpty()) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Text("Tidak ada produk ditemukan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(uiState.daftarProduk, key = { it.id }) { item ->
                        KartuProdukAdmin(
                            item = item,
                            onEdit = { onEditProduk(item.id) },
                            onPenyesuaian = { viewModel.bukaDialogPenyesuaian(item) },
                            onKartuStok = { viewModel.bukaKartuStok(item) },
                            onToggleAktif = { viewModel.toggleAktifkanProduk(item) },
                        )
                    }
                }
            }
        }
    }

    // Modal Penyesuaian Stok
    uiState.dialogPenyesuaianTarget?.let { target ->
        DialogPenyesuaianStok(
            produk = target,
            onDismiss = { viewModel.tutupDialogPenyesuaian() },
            onRestock = { qty, alasan -> viewModel.submitRestock(target.id, qty, alasan) },
            onOpname = { fisik, alasan -> viewModel.submitOpname(target.id, fisik, alasan) },
            onBarangRusak = { qty, alasan -> viewModel.submitBarangRusak(target.id, qty, alasan) },
            onPemusnahan = { qty, alasan -> viewModel.submitPemusnahan(target.id, qty, alasan) },
        )
    }

    // Sheet Kartu Stok Audit
    uiState.kartuStokTarget?.let { target ->
        KartuStokSheet(
            produk = target,
            onDismiss = { viewModel.tutupKartuStok() },
        )
    }
}

@Composable
private fun KartuProdukAdmin(
    item: ProdukItemAdminUi,
    onEdit: () -> Unit,
    onPenyesuaian: () -> Unit,
    onKartuStok: () -> Unit,
    onToggleAktif: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(item.nama, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("SKU: ${item.sku}${if (!item.barcode.isNullOrBlank()) " | ${item.barcode}" else ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        !item.aktif -> MaterialTheme.colorScheme.surfaceVariant
                        item.stokNormalScaled <= 0 -> MaterialTheme.colorScheme.errorContainer
                        item.stokNormalScaled <= 5_000L -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    },
                ) {
                    Text(
                        text = when {
                            !item.aktif -> "Non-Aktif"
                            item.stokNormalScaled <= 0 -> "Habis"
                            item.stokNormalScaled <= 5_000L -> "Menipis"
                            else -> "Aman"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            !item.aktif -> MaterialTheme.colorScheme.onSurfaceVariant
                            item.stokNormalScaled <= 0 -> MaterialTheme.colorScheme.onErrorContainer
                            item.stokNormalScaled <= 5_000L -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Harga Jual: ${formatRupiah(item.harga)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text("HPP/Modal: ${formatRupiah(item.hargaModal)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Stok: ${formatQuantity(item.stokNormalScaled)} unit", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    if (item.stokRusakScaled > 0L) {
                        Text("Rusak: ${formatQuantity(item.stokRusakScaled)} unit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(6.dp))

            // Actions Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Ubah Produk", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onPenyesuaian, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Tune, contentDescription = "Sesuaikan Stok", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onKartuStok, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.History, contentDescription = "Kartu Stok", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (item.aktif) "Aktif" else "Mati", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.width(6.dp))
                    Switch(
                        checked = item.aktif,
                        onCheckedChange = { onToggleAktif() },
                    )
                }
            }
        }
    }
}