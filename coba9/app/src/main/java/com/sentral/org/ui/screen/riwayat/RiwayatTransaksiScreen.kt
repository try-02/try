package com.sentral.org.ui.screen.riwayat

import android.content.Intent
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.sentral.org.data.entity.TransaksiDenganDetail
import com.sentral.org.data.entity.TransaksiEntity
import com.sentral.org.data.model.MetodePembayaran
import com.sentral.org.data.model.QUANTITY_SCALE
import com.sentral.org.data.model.StatusTransaksi
import com.sentral.org.data.model.TujuanStokPengembalian
import com.sentral.org.ui.screen.pos.formatRupiah
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiwayatTransaksiScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RiwayatViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagedTransaksi: LazyPagingItems<TransaksiEntity> = viewModel.pagedTransaksi.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is RiwayatEvent.Pesan -> snackbarHostState.showSnackbar(event.teks)
                is RiwayatEvent.ReprintSukses -> snackbarHostState.showSnackbar("Salinan struk ${event.nomorTransaksi} berhasil dicetak")
                is RiwayatEvent.VoidSukses -> snackbarHostState.showSnackbar("Transaksi ${event.nomorTransaksi} berhasil dibatalkan (VOID)")
                is RiwayatEvent.ReturSukses -> snackbarHostState.showSnackbar("Retur ${event.nomorTransaksi} berhasil (Refund ${formatRupiah(event.totalRefund)})")
                is RiwayatEvent.FileSiapDibagikan -> {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = event.mimeType
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Bagikan Laporan Transaksi"))
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
                title = {
                    Text(
                        "Riwayat Transaksi",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (uiState.sedangEkspor) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(onClick = { viewModel.eksporLaporanExcel() }) {
                            Icon(
                                Icons.Filled.FileDownload,
                                contentDescription = "Ekspor Laporan Excel",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = uiState.filter.query,
                onValueChange = { viewModel.setQueryPencarian(it) },
                placeholder = { Text("Cari nomor transaksi...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.filter.query.isNotBlank()) {
                        IconButton(onClick = { viewModel.setQueryPencarian("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Hapus pencarian")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = uiState.filter.status == null,
                    onClick = { viewModel.setFilterStatus(null) },
                    label = { Text("Semua Status") },
                    shape = MaterialTheme.shapes.small,
                )
                FilterChip(
                    selected = uiState.filter.status == StatusTransaksi.SELESAI,
                    onClick = { viewModel.setFilterStatus(StatusTransaksi.SELESAI) },
                    label = { Text("Selesai") },
                    shape = MaterialTheme.shapes.small,
                )
                FilterChip(
                    selected = uiState.filter.status == StatusTransaksi.VOID,
                    onClick = { viewModel.setFilterStatus(StatusTransaksi.VOID) },
                    label = { Text("Void") },
                    shape = MaterialTheme.shapes.small,
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when (val refreshState = pagedTransaksi.loadState.refresh) {
                    is LoadState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is LoadState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Gagal memuat data: ${refreshState.error.message ?: "Kesalahan tidak diketahui"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(onClick = { pagedTransaksi.retry() }) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Coba Lagi")
                                }
                            }
                        }
                    }
                    is LoadState.NotLoading -> {
                        if (pagedTransaksi.itemCount == 0) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(80.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Filled.Receipt,
                                                contentDescription = null,
                                                modifier = Modifier.size(40.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        "Tidak ada transaksi",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(
                                    count = pagedTransaksi.itemCount,
                                    key = pagedTransaksi.itemKey { it.id },
                                ) { index ->
                                    val trx = pagedTransaksi[index]
                                    if (trx != null) {
                                        KartuTransaksi(
                                            transaksi = trx,
                                            onClick = { viewModel.bukaDetailTransaksi(trx.id) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Sheet Detail Transaksi
    uiState.detailTerpilih?.let { detail ->
        SheetDetailTransaksi(
            detail = detail,
            sedangReprint = uiState.sedangReprint,
            sedangMemprosesAksi = uiState.sedangMemprosesAksi,
            onDismiss = { viewModel.tutupDetailTransaksi() },
            onReprint = { viewModel.cetakUlangStruk(detail) },
            onBukaVoid = { viewModel.bukaDialogVoid() },
            onBukaRetur = { viewModel.bukaDialogRetur() },
        )
    }

    // Dialog Konfirmasi Void
    if (uiState.dialogVoidTerbuka) {
        DialogKonfirmasiVoid(
            alasan = uiState.alasanVoidInput,
            sedangMemproses = uiState.sedangMemprosesAksi,
            onAlasanChange = { viewModel.setAlasanVoid(it) },
            onKonfirmasi = { viewModel.konfirmasiVoid() },
            onDismiss = { viewModel.tutupDialogVoid() },
        )
    }

    // Sheet Form Retur Barang
    if (uiState.dialogReturTerbuka) {
        SheetFormRetur(
            items = uiState.daftarItemRetur,
            metodeRefund = uiState.metodeRefundRetur,
            catatan = uiState.catatanReturInput,
            sedangMemproses = uiState.sedangMemprosesAksi,
            onTambahQty = { viewModel.ubahQtyRetur(it, 1) },
            onKurangQty = { viewModel.ubahQtyRetur(it, -1) },
            onUbahTujuan = { id, tujuan -> viewModel.ubahTujuanRetur(id, tujuan) },
            onUbahMetodeRefund = { viewModel.setMetodeRefund(it) },
            onCatatanChange = { viewModel.setCatatanRetur(it) },
            onKonfirmasi = { viewModel.konfirmasiRetur() },
            onDismiss = { viewModel.tutupDialogRetur() },
        )
    }
}

@Composable
private fun KartuTransaksi(
    transaksi: TransaksiEntity,
    onClick: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val waktuFormatted = remember(transaksi.dibuatPada) { dateFormat.format(Date(transaksi.dibuatPada)) }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    transaksi.nomorTransaksi,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (transaksi.status) {
                        StatusTransaksi.SELESAI -> MaterialTheme.colorScheme.primaryContainer
                        StatusTransaksi.VOID -> MaterialTheme.colorScheme.errorContainer
                    },
                ) {
                    Text(
                        transaksi.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (transaksi.status) {
                            StatusTransaksi.SELESAI -> MaterialTheme.colorScheme.onPrimaryContainer
                            StatusTransaksi.VOID -> MaterialTheme.colorScheme.onErrorContainer
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "Kasir: ${transaksi.namaKasir} • $waktuFormatted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Total Belanja",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    formatRupiah(transaksi.total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetDetailTransaksi(
    detail: TransaksiDenganDetail,
    sedangReprint: Boolean,
    sedangMemprosesAksi: Boolean,
    onDismiss: () -> Unit,
    onReprint: () -> Unit,
    onBukaVoid: () -> Unit,
    onBukaRetur: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()) }
    val waktuFormatted = remember(detail.transaksi.dibuatPada) { dateFormat.format(Date(detail.transaksi.dibuatPada)) }
    val isSelesai = detail.transaksi.status == StatusTransaksi.SELESAI

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Text(
                        detail.transaksi.nomorTransaksi,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        waktuFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (detail.transaksi.status) {
                        StatusTransaksi.SELESAI -> MaterialTheme.colorScheme.primaryContainer
                        StatusTransaksi.VOID -> MaterialTheme.colorScheme.errorContainer
                    },
                ) {
                    Text(
                        detail.transaksi.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (detail.transaksi.status) {
                            StatusTransaksi.SELESAI -> MaterialTheme.colorScheme.onPrimaryContainer
                            StatusTransaksi.VOID -> MaterialTheme.colorScheme.onErrorContainer
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))

            Text(
                "Rincian Produk",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(detail.items.size) { index ->
                    val item = detail.items[index]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.namaProduk,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${item.jumlah / QUANTITY_SCALE}x @ ${formatRupiah(item.hargaSatuan)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (item.diskonItem > 0) {
                                Text(
                                    "Diskon: -${formatRupiah(item.diskonItem)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                        Text(
                            formatRupiah(item.totalBaris),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Total Akhir", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(formatRupiah(detail.transaksi.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    if (detail.transaksi.alasanPembatalan != null) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "Alasan Void: ${detail.transaksi.alasanPembatalan}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Baris Tombol Aksi Kasir (Void & Retur hanya jika status SELESAI)
            if (isSelesai) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = onBukaVoid,
                        enabled = !sedangMemprosesAksi,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(4.dp))
                        Text("Void", color = MaterialTheme.colorScheme.error)
                    }

                    OutlinedButton(
                        onClick = onBukaRetur,
                        enabled = !sedangMemprosesAksi,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Icon(Icons.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Retur")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Tombol Cetak Ulang Struk
            Button(
                onClick = onReprint,
                enabled = !sedangReprint,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                if (sedangReprint) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cetak Ulang Struk (Salinan)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DialogKonfirmasiVoid(
    alasan: String,
    sedangMemproses: Boolean,
    onAlasanChange: (String) -> Unit,
    onKonfirmasi: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text("Batalkan Transaksi (Void)?", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    "Transaksi akan dibatalkan permanen. Seluruh stok barang akan dikembalikan ke rak, dan uang tunai akan di-refund dari shift saat ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = alasan,
                    onValueChange = onAlasanChange,
                    label = { Text("Alasan Pembatalan") },
                    placeholder = { Text("Contoh: Pembeli batal, salah input...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onKonfirmasi,
                enabled = !sedangMemproses && alasan.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = MaterialTheme.shapes.medium,
            ) {
                if (sedangMemproses) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onError)
                } else {
                    Text("Ya, Batalkan", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !sedangMemproses) {
                Text("Batal")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetFormRetur(
    items: List<ItemReturUi>,
    metodeRefund: MetodePembayaran,
    catatan: String,
    sedangMemproses: Boolean,
    onTambahQty: (Long) -> Unit,
    onKurangQty: (Long) -> Unit,
    onUbahTujuan: (Long, TujuanStokPengembalian) -> Unit,
    onUbahMetodeRefund: (MetodePembayaran) -> Unit,
    onCatatanChange: (String) -> Unit,
    onKonfirmasi: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(20.dp),
        ) {
            Text(
                "Pengembalian Barang (Retur)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Tentukan item dan unit yang ingin dikembalikan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items.size) { index ->
                    val row = items[index]
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        row.item.namaProduk,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        "Sisa dapat diretur: ${row.sisaQtyScaled / QUANTITY_SCALE} unit",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onKurangQty(row.item.id) },
                                        enabled = row.qtyPilihanScaled > 0,
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(Icons.Filled.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                    Text(
                                        "${row.qtyPilihanScaled / QUANTITY_SCALE}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(32.dp),
                                        textAlign = TextAlign.Center,
                                    )
                                    IconButton(
                                        onClick = { onTambahQty(row.item.id) },
                                        enabled = row.qtyPilihanScaled < row.sisaQtyScaled,
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            if (row.qtyPilihanScaled > 0) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("Kondisi:", style = MaterialTheme.typography.labelSmall)
                                    FilterChip(
                                        selected = row.tujuan == TujuanStokPengembalian.NORMAL,
                                        onClick = { onUbahTujuan(row.item.id, TujuanStokPengembalian.NORMAL) },
                                        label = { Text("Bagus (Rak)") },
                                        shape = MaterialTheme.shapes.small,
                                    )
                                    FilterChip(
                                        selected = row.tujuan == TujuanStokPengembalian.RUSAK,
                                        onClick = { onUbahTujuan(row.item.id, TujuanStokPengembalian.RUSAK) },
                                        label = { Text("Rusak") },
                                        shape = MaterialTheme.shapes.small,
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = catatan,
                        onValueChange = onCatatanChange,
                        label = { Text("Catatan Retur") },
                        placeholder = { Text("Misal: barang cacat kemasan") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onKonfirmasi,
                enabled = !sedangMemproses && items.any { it.qtyPilihanScaled > 0 },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                if (sedangMemproses) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Proses Pengembalian", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}