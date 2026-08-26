package com.sentral.org.ui.screen.pos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentral.org.data.entity.ProdukEntity
import com.sentral.org.data.model.QUANTITY_SCALE
import com.sentral.org.data.model.StatusKeranjang
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosUtamaScreen(
    onNavigateToRiwayat: () -> Unit,
    onNavigateToTutupShift: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: KasirViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var dialogBayarTerbuka by rememberSaveable { mutableStateOf(false) }
    var hasilCheckout by remember { mutableStateOf<KasirEvent.CheckoutBerhasil?>(null) }
    var kataKunci by rememberSaveable { mutableStateOf("") }
    var kategoriTerpilih by rememberSaveable { mutableStateOf<String?>(null) }

    // Event sekali-tampil: pesan -> snackbar, checkout sukses -> dialog.
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is KasirEvent.Pesan -> snackbarHostState.showSnackbar(event.teks)
                is KasirEvent.CheckoutBerhasil -> hasilCheckout = event
            }
        }
    }

    val kategoriList = remember(state.produk) {
        state.produk.map { it.kategori }.distinct().sorted()
    }
    val produkTersaring = remember(state.produk, kataKunci, kategoriTerpilih) {
        state.produk.filter { p ->
            (kategoriTerpilih == null || p.kategori.equals(kategoriTerpilih, ignoreCase = true)) &&
                (kataKunci.isBlank() ||
                    p.nama.contains(kataKunci, ignoreCase = true) ||
                    p.sku.contains(kataKunci, ignoreCase = true) ||
                    p.barcode?.contains(kataKunci, ignoreCase = true) == true)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.PointOfSale,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("POS Kasir", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "Offline • ${state.jumlahJenisItem} item di keranjang",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToRiwayat) {
                        Icon(Icons.Filled.History, contentDescription = "Riwayat")
                    }
                    IconButton(onClick = onNavigateToTutupShift) {
                        Icon(Icons.Filled.Logout, contentDescription = "Tutup Shift")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PanelProduk(
                produk = produkTersaring,
                semuaKategori = kategoriList,
                kataKunci = kataKunci,
                onUbahKataKunci = { kataKunci = it },
                kategoriTerpilih = kategoriTerpilih,
                onPilihKategori = { kategoriTerpilih = if (kategoriTerpilih == it) null else it },
                totalProduk = state.produk.size,
                onProdukDipilih = viewModel::tambahProduk,
                modifier = Modifier.weight(0.55f),
            )
            PanelKeranjang(
                state = state,
                onKeranjangBaru = viewModel::keranjangBaru,
                onPilihKeranjang = viewModel::pilihKeranjang,
                onLanjutkan = viewModel::lanjutkanKeranjang,
                onTahan = viewModel::tahanKeranjang,
                onBatal = viewModel::batalkanKeranjang,
                onTambah = viewModel::tambahSatuan,
                onKurangi = viewModel::kurangiSatuan,
                onHapus = viewModel::hapusBaris,
                onBayarCash = { dialogBayarTerbuka = true },
                onBayarQris = viewModel::bayarQris,
                modifier = Modifier.weight(0.45f),
            )
        }
    }

    if (dialogBayarTerbuka) {
        DialogPembayaranTunai(
            total = state.subtotal,
            sedangProses = state.sedangProses,
            onKonfirmasi = { diterima ->
                dialogBayarTerbuka = false
                viewModel.bayarCash(diterima)
            },
            onTutup = { dialogBayarTerbuka = false },
        )
    }

    hasilCheckout?.let { hasil ->
        DialogCheckoutBerhasil(
            nomorTransaksi = hasil.nomorTransaksi,
            kembalian = hasil.kembalian,
            onTutup = { hasilCheckout = null },
        )
    }
}

// ---------- Panel kiri: pencarian + grid produk ----------

@Composable
private fun PanelProduk(
    produk: List<ProdukEntity>,
    semuaKategori: List<String>,
    kataKunci: String,
    onUbahKataKunci: (String) -> Unit,
    kategoriTerpilih: String?,
    onPilihKategori: (String) -> Unit,
    totalProduk: Int,
    onProdukDipilih: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "Produk",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "$totalProduk produk tersedia",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = kataKunci,
                onValueChange = onUbahKataKunci,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cari nama / SKU / barcode…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
            )

            if (semuaKategori.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    semuaKategori.forEach { kategori ->
                        FilterChip(
                            selected = kategoriTerpilih == kategori,
                            onClick = { onPilihKategori(kategori) },
                            label = { Text(kategori) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (produk.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (totalProduk == 0) {
                                "Belum ada produk aktif.\nTambahkan produk & buka shift untuk mulai."
                            } else {
                                "Tidak ada produk yang cocok."
                            },
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 116.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(produk, key = { it.id }) { p ->
                        KartuProduk(p, onClick = { onProdukDipilih(p.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun KartuProduk(produk: ProdukEntity, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .clickable(onClick = onClick)
                .padding(12.dp),
        ) {
            Text(
                produk.nama,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                formatRupiah(produk.harga),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            if (produk.kategori.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    produk.kategori,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------- Panel kanan: keranjang ----------

@Composable
private fun PanelKeranjang(
    state: KasirUiState,
    onKeranjangBaru: () -> Unit,
    onPilihKeranjang: (Long) -> Unit,
    onLanjutkan: (Long) -> Unit,
    onTahan: () -> Unit,
    onBatal: () -> Unit,
    onTambah: (Long) -> Unit,
    onKurangi: (Long) -> Unit,
    onHapus: (Long) -> Unit,
    onBayarCash: () -> Unit,
    onBayarQris: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Keranjang",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onKeranjangBaru) { Text("+ Baru") }
            }

            if (state.keranjangTerbuka.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    state.keranjangTerbuka.forEach { keranjang ->
                        val ditahan = keranjang.status == StatusKeranjang.DITAHAN
                        FilterChip(
                            selected = keranjang.id == state.keranjangAktifId,
                            onClick = {
                                if (ditahan) onLanjutkan(keranjang.id) else onPilihKeranjang(keranjang.id)
                            },
                            label = { Text(if (ditahan) "#${keranjang.id} ⏸" else "#${keranjang.id}") },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (state.baris.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.ShoppingCartCheckout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Keranjang kosong.\nSentuh produk untuk menambah.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.baris.forEach { baris ->
                        BarisItem(baris, onTambah, onKurangi, onHapus)
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "Total",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    formatRupiah(state.subtotal),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onTahan,
                    enabled = state.keranjangAktifId != null && !state.sedangProses,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tahan")
                }
                OutlinedButton(
                    onClick = onBatal,
                    enabled = state.keranjangAktifId != null && !state.sedangProses,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Batalkan")
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onBayarCash,
                enabled = state.baris.isNotEmpty() && !state.sedangProses,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Filled.PointOfSale, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (state.sedangProses) "Memproses…" else "Bayar Tunai",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onBayarQris,
                enabled = state.baris.isNotEmpty() && !state.sedangProses,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(Icons.Filled.QrCode2, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Bayar QRIS")
            }
        }
    }
}

@Composable
private fun BarisItem(
    baris: BarisKeranjangUi,
    onTambah: (Long) -> Unit,
    onKurangi: (Long) -> Unit,
    onHapus: (Long) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    baris.nama,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${formatRupiah(baris.hargaSatuan)} × ${baris.jumlahScaled / QUANTITY_SCALE}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Stepper qty ala kasir modern
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onKurangi(baris.produkId) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Kurangi",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Text(
                        (baris.jumlahScaled / QUANTITY_SCALE).toString(),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(28.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(
                        onClick = { onTambah(baris.produkId) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Tambah",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            IconButton(onClick = { onHapus(baris.produkId) }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Hapus baris",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ---------- Dialog pembayaran tunai dengan numpad ----------

@Composable
private fun DialogPembayaranTunai(
    total: Long,
    sedangProses: Boolean,
    onKonfirmasi: (Long) -> Unit,
    onTutup: () -> Unit,
) {
    var teksInput by rememberSaveable { mutableStateOf("") }
    val diterima = teksInput.filter(Char::isDigit).toLongOrNull() ?: 0L
    val selisih = diterima - total
    val cukup = total > 0 && diterima >= total

    fun tekan(tombol: String) {
        when (tombol) {
            "C" -> teksInput = ""
            "⌫" -> teksInput = teksInput.dropLast(1)
            else -> teksInput = (teksInput.filter(Char::isDigit) + tombol).take(12)
        }
    }

    AlertDialog(
        onDismissRequest = onTutup,
        title = { Text("Bayar Tunai", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total belanja", style = MaterialTheme.typography.labelMedium)
                Text(
                    formatRupiah(total),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (teksInput.isBlank()) "Rp 0" else formatRupiah(diterima),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    )
                }

                if (diterima > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (cukup) "Kembalian: ${formatRupiah(selisih)}" else "Kurang ${formatRupiah(-selisih)}",
                        color = if (cukup) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.height(10.dp))
                Numpad(onTekan = ::tekan)

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = diterima == total && total > 0, onClick = { teksInput = total.toString() }, label = { Text("Pas") })
                    FilterChip(selected = diterima == 50_000L, onClick = { teksInput = "50000" }, label = { Text("50rb") })
                    FilterChip(selected = diterima == 100_000L, onClick = { teksInput = "100000" }, label = { Text("100rb") })
                    FilterChip(selected = diterima == 200_000L, onClick = { teksInput = "200000" }, label = { Text("200rb") })
                }
            }
        },
        confirmButton = {
            Button(
                enabled = cukup && !sedangProses,
                onClick = { onKonfirmasi(diterima) },
            ) { Text(if (sedangProses) "Memproses…" else "Selesaikan") }
        },
        dismissButton = {
            TextButton(onClick = onTutup) { Text("Batal") }
        },
    )
}

@Composable
private fun Numpad(onTekan: (String) -> Unit) {
    val tombol = listOf(
        listOf("7", "8", "9"),
        listOf("4", "5", "6"),
        listOf("1", "2", "3"),
        listOf("C", "0", "⌫"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        tombol.forEach { barisTombol ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                barisTombol.forEach { t ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clickable(onClick = { onTekan(t) }),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(t, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ---------- Dialog checkout sukses ----------

@Composable
private fun DialogCheckoutBerhasil(
    nomorTransaksi: String,
    kembalian: Long,
    onTutup: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onTutup,
        icon = {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(56.dp),
            )
        },
        title = {
            Text(
                "Transaksi Berhasil",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        nomorTransaksi,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("Kembalian", style = MaterialTheme.typography.labelMedium)
                Text(
                    formatRupiah(kembalian),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        confirmButton = {
            Button(onClick = onTutup, modifier = Modifier.fillMaxWidth()) { Text("Selesai") }
        },
    )
}
