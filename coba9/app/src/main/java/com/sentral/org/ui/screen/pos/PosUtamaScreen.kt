package com.sentral.org.ui.screen.pos

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentral.org.data.entity.KeranjangEntity
import com.sentral.org.data.entity.ProdukEntity
import com.sentral.org.data.model.QUANTITY_SCALE
import com.sentral.org.data.model.StatusKeranjang
import org.koin.androidx.compose.koinViewModel

import com.sentral.org.data.model.PrinterStatus.SIBUK
import com.sentral.org.data.model.PrinterStatus.ERROR
import com.sentral.org.data.model.PrinterStatus.DINONAKTIFKAN
import com.sentral.org.data.model.PrinterStatus.SIAP

private enum class TabBawah(val label: String) {
    PRODUK("Kasir"),
    PESANAN("Pesanan"),
    LAPORAN("Laporan"),
    PENGATURAN("Setelan"),
}

/**
 * Layar kasir utama — Compact POS Smartphone Edition.
 * Layout sangat padat, mengutamakan kecepatan transaksi di layar kecil.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PosUtamaScreen(
    onNavigateToRiwayat: () -> Unit,
    onNavigateToTutupShift: () -> Unit,
    onNavigateToPrinterSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: KasirViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profil by viewModel.profilToko.collectAsStateWithLifecycle()
    val stokPerProduk by viewModel.stokPerProduk.collectAsStateWithLifecycle()
    val printerStatus by viewModel.printerStatus.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var dialogBayarTerbuka by rememberSaveable { mutableStateOf(false) }
    var metodeBayar by rememberSaveable { mutableIntStateOf(0) }
    var konfirmasiBatal by rememberSaveable { mutableStateOf(false) }
    var hasilCheckout by remember { mutableStateOf<KasirEvent.CheckoutBerhasil?>(null) }
    var sheetKeranjangTerbuka by rememberSaveable { mutableStateOf(false) }

    val kataKunci = rememberSaveable { mutableStateOf("") }
    val kategoriTerpilih = rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is KasirEvent.Pesan -> snackbarHostState.showSnackbar(event.teks)
                is KasirEvent.HapusBaris -> snackbarHostState.showSnackbar(
                    message = "\"${event.nama}\" dihapus",
                    actionLabel = "UNDO",
                ).let { hasil ->
                    if (hasil == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                        viewModel.restoreBaris(event.produkId, event.jumlahScaled)
                    }
                }
                is KasirEvent.CheckoutBerhasil -> hasilCheckout = event
            }
        }
    }

    val semuaKategori = remember(state.produk) {
        state.produk.map { it.kategori }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val produkTersaring = remember(state.produk, kataKunci.value, kategoriTerpilih.value) {
        state.produk.filter { p ->
            (kategoriTerpilih.value == null ||
                p.kategori.equals(kategoriTerpilih.value, ignoreCase = true)) &&
                (kataKunci.value.isBlank() ||
                    p.nama.contains(kataKunci.value, ignoreCase = true) ||
                    p.sku.contains(kataKunci.value, ignoreCase = true) ||
                    p.barcode?.contains(kataKunci.value, ignoreCase = true) == true)
        }
    }

    val adaKeranjang = state.baris.isNotEmpty()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                // TopAppBar ultra-compact (48dp height)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 12.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                profil?.namaToko?.trim()?.take(1)?.uppercase()
                                    .orEmpty().ifEmpty { "P" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        profil?.namaToko?.takeIf { it.isNotBlank() } ?: "POS Kasir",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    
                    // Actions (compact icons)
                    IconButton(onClick = { sheetKeranjangTerbuka = true }, modifier = Modifier.size(40.dp)) {
                        BadgedBox(
                            badge = {
                                if (state.jumlahJenisItem > 0) {
                                    Badge { Text("${state.jumlahJenisItem}") }
                                }
                            },
                        ) {
                            Icon(Icons.Filled.ShoppingBag, contentDescription = "Keranjang", modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    IconButton(onClick = { onNavigateToPrinterSettings() }, modifier = Modifier.size(40.dp)) {
                        BadgedBox(
                            badge = {
                                when (printerStatus) {
                                    SIBUK -> Badge { Text("●", color = MaterialTheme.colorScheme.secondary) }
                                    ERROR, DINONAKTIFKAN -> Badge { Text("!", color = MaterialTheme.colorScheme.onError) }
                                    else -> {}
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Print,
                                contentDescription = "Printer",
                                tint = when (printerStatus) {
                                    SIAP -> MaterialTheme.colorScheme.primary
                                    SIBUK -> MaterialTheme.colorScheme.secondary
                                    ERROR, DINONAKTIFKAN -> MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    
                    IconButton(onClick = onNavigateToRiwayat, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.History, contentDescription = "Riwayat", modifier = Modifier.size(20.dp))
                    }
                    
                    IconButton(onClick = onNavigateToTutupShift, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Keluar", modifier = Modifier.size(20.dp))
                    }
                }

                // Sticky Summary Dock (Ultra thin)
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 16.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (!adaKeranjang) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Icon(
                                    Icons.Filled.ShoppingBag,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (!adaKeranjang) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "${state.jumlahJenisItem} item",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!adaKeranjang) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            formatRupiah(state.subtotal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (!adaKeranjang) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                TabBawah.entries.forEachIndexed { index, t ->
                    val terpilih = tab == index
                    NavigationBarItem(
                        selected = terpilih,
                        onClick = { tab = index },
                        icon = {
                            when (t) {
                                TabBawah.PRODUK -> Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(20.dp))
                                TabBawah.PESANAN -> BadgedBox(
                                    badge = {
                                        val ditahan = state.keranjangTerbuka.count { it.status == StatusKeranjang.DITAHAN }
                                        if (ditahan > 0) Badge { Text("$ditahan") }
                                    },
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(20.dp))
                                }
                                TabBawah.LAPORAN -> Icon(Icons.Filled.Insights, contentDescription = null, modifier = Modifier.size(20.dp))
                                TabBawah.PENGATURAN -> Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                        },
                        label = { Text(t.label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> PanelProduk(
                    produk = produkTersaring,
                    semuaKategori = semuaKategori,
                    stokPerProduk = stokPerProduk,
                    kataKunci = kataKunci.value,
                    onUbahKataKunci = { kataKunci.value = it },
                    kategoriTerpilih = kategoriTerpilih.value,
                    onPilihKategori = {
                        kategoriTerpilih.value = when {
                            it.isEmpty() -> null
                            kategoriTerpilih.value == it -> null
                            else -> it
                        }
                    },
                    onProdukDipilih = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.tambahProduk(it)
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                1 -> TabPesananAktif(
                    keranjang = state.keranjangTerbuka,
                    keranjangAktifId = state.keranjangAktifId,
                    onPilih = { viewModel.pilihKeranjang(it); tab = 0 },
                    onLanjutkan = { viewModel.lanjutkanKeranjang(it); tab = 0 },
                    modifier = Modifier.fillMaxSize(),
                )

                2 -> PlaceholderTab(
                    ikon = Icons.Filled.Insights,
                    judul = "Laporan",
                    pesan = "Ringkasan penjualan offline akan hadir di sini.",
                )

                else -> PlaceholderTab(
                    ikon = Icons.Filled.Settings,
                    judul = "Pengaturan",
                    pesan = "Printer, pajak, dan backup dikonfigurasi di sini.",
                )
            }

            if (sheetKeranjangTerbuka) {
                val density = LocalDensity.current
                val tinggiLayarDp = with(density) { androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.height.toDp() }
                ModalBottomSheet(
                    onDismissRequest = { sheetKeranjangTerbuka = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    PanelKeranjang(
                        state = state,
                        onHapusBaris = viewModel::batalkanBarisDenganUndo,
                        onKeranjangBaru = viewModel::keranjangBaru,
                        onPilihKeranjang = viewModel::pilihKeranjang,
                        onLanjutkan = viewModel::lanjutkanKeranjang,
                        onTahan = viewModel::tahanKeranjang,
                        onBatal = { konfirmasiBatal = true },
                        onTambah = viewModel::tambahSatuan,
                        onKurangi = viewModel::kurangiSatuan,
                        onBayarCash = {
                            metodeBayar = 0
                            sheetKeranjangTerbuka = false
                            dialogBayarTerbuka = true
                        },
                        onBayarQris = {
                            metodeBayar = 1
                            sheetKeranjangTerbuka = false
                            dialogBayarTerbuka = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(tinggiLayarDp * 0.75f),
                    )
                }
            }
        }
    }

    if (dialogBayarTerbuka) {
        DialogPembayaran(
            total = state.subtotal,
            sedangProses = state.sedangProses,
            onKonfirmasiTunai = { diterima ->
                dialogBayarTerbuka = false
                viewModel.bayarCash(diterima)
            },
            onKonfirmasiQris = {
                dialogBayarTerbuka = false
                viewModel.bayarQris()
            },
            metodeAwal = metodeBayar,
            onTutup = { dialogBayarTerbuka = false },
        )
    }

    if (konfirmasiBatal) {
        DialogKonfirmasi(
            judul = "Batalkan keranjang?",
            deskripsi = "Semua item di keranjang #${state.keranjangAktifId ?: "-"} akan " +
                "dihapus dan keranjang tidak bisa dipakai lagi.",
            teksKonfirmasi = "Ya, Batalkan",
            ikonHapus = true,
            onKonfirmasi = viewModel::batalkanKeranjang,
            onTutup = { konfirmasiBatal = false },
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

@Composable
private fun TabPesananAktif(
    keranjang: List<KeranjangEntity>,
    keranjangAktifId: Long?,
    onPilih: (Long) -> Unit,
    onLanjutkan: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (keranjang.isEmpty()) {
        PlaceholderTab(
            ikon = Icons.AutoMirrored.Filled.ReceiptLong,
            judul = "Belum ada pesanan",
            pesan = "Keranjang yang ditahan atau baru dibuat\nakan muncul di sini.",
            modifier = modifier,
        )
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(keranjang, key = { _, k -> k.id }) { _, k ->
            val ditahan = k.status == StatusKeranjang.DITAHAN
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                border = BorderStroke(
                    1.dp,
                    if (k.id == keranjangAktifId) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.fillMaxWidth().clickable { onPilih(k.id) },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        val locale = androidx.compose.ui.platform.LocalLocale.current.platformLocale
                        val waktu = remember(k.dibuatPada, locale) {
                            java.text.SimpleDateFormat("HH:mm", locale).format(java.util.Date(k.dibuatPada))
                        }
                        Text(
                            "#${k.id} • ${if (ditahan) "Ditahan" else "Aktif"}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${k.namaKasir} • $waktu",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(
                        onClick = { onLanjutkan(k.id) },
                        enabled = ditahan,
                        shape = CircleShape,
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(if (ditahan) "Lanjut" else "Dipilih", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderTab(
    ikon: androidx.compose.ui.graphics.vector.ImageVector,
    judul: String,
    pesan: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        ikon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(judul, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                pesan,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PanelProduk(
    produk: List<ProdukEntity>,
    semuaKategori: List<String>,
    stokPerProduk: Map<Long, Long>,
    kataKunci: String,
    onUbahKataKunci: (String) -> Unit,
    kategoriTerpilih: String?,
    onPilihKategori: (String) -> Unit,
    onProdukDipilih: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        // Compact Search Bar
        OutlinedTextField(
            value = kataKunci,
            onValueChange = onUbahKataKunci,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp)
                .height(48.dp),
            placeholder = { Text("Cari nama / SKU…", style = MaterialTheme.typography.bodySmall) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (kataKunci.isNotEmpty()) {
                    IconButton(onClick = { onUbahKataKunci("") }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Bersihkan", modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = MaterialTheme.shapes.small,
        )

        if (semuaKategori.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                FilterChip(
                    selected = kategoriTerpilih == null,
                    onClick = { onPilihKategori("") },
                    label = { Text("Semua", style = MaterialTheme.typography.labelMedium) },
                    shape = CircleShape,
                    modifier = Modifier.height(32.dp),
                )
                semuaKategori.forEach { kategori ->
                    FilterChip(
                        selected = kategoriTerpilih == kategori,
                        onClick = { onPilihKategori(kategori) },
                        label = { Text(kategori, style = MaterialTheme.typography.labelMedium) },
                        shape = CircleShape,
                        modifier = Modifier.height(32.dp),
                    )
                }
            }
        }

        if (produk.isEmpty()) {
            PlaceholderTab(
                ikon = Icons.Filled.Inventory2,
                judul = if (semuaKategori.isNotEmpty()) "Tidak cocok" else "Belum ada produk",
                pesan = if (semuaKategori.isNotEmpty()) "Coba kata kunci lain." else "Tambahkan produk untuk mulai.",
                modifier = Modifier.weight(1f),
            )
        } else {
            // Grid Produk yang Sangat Padat
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(produk, key = { it.id }) { p ->
                    KartuProduk(
                        nama = p.nama,
                        harga = p.harga,
                        stok = stokPerProduk[p.id],
                        onTap = { onProdukDipilih(p.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun KartuProduk(
    nama: String,
    harga: Long,
    stok: Long?,
    onTap: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val ditekan by interaction.collectIsPressedAsState()
    val skala by animateFloatAsState(
        targetValue = if (ditekan) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "skalaKartu",
    )

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .graphicsLayer { scaleX = skala; scaleY = skala }
            .fillMaxWidth()
            .height(110.dp)
            .clickable(interactionSource = interaction, indication = null, onClick = onTap),
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(24.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            nama.trim().take(1).uppercase().ifBlank { "?" },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                if (stok != null && stok in 1..5) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer) {
                        Text(
                            "$stok",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                nama,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                formatRupiah(harga),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PanelKeranjang(
    state: KasirUiState,
    onHapusBaris: (Long) -> Unit,
    onKeranjangBaru: () -> Unit,
    onPilihKeranjang: (Long) -> Unit,
    onLanjutkan: (Long) -> Unit,
    onTahan: () -> Unit,
    onBatal: () -> Unit,
    onTambah: (Long) -> Unit,
    onKurangi: (Long) -> Unit,
    onBayarCash: () -> Unit,
    onBayarQris: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        ) {
            Text("Keranjang", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = onKeranjangBaru) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(2.dp))
                Text("Baru", style = MaterialTheme.typography.labelLarge)
            }
        }

        if (state.keranjangTerbuka.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                state.keranjangTerbuka.forEach { keranjang ->
                    val ditahan = keranjang.status == StatusKeranjang.DITAHAN
                    FilterChip(
                        selected = keranjang.id == state.keranjangAktifId,
                        onClick = {
                            if (ditahan) onLanjutkan(keranjang.id) else onPilihKeranjang(keranjang.id)
                        },
                        label = { Text("#${keranjang.id}", style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = if (ditahan) {
                            { Icon(Icons.Filled.Pause, contentDescription = "Ditahan", modifier = Modifier.size(12.dp)) }
                        } else null,
                        shape = CircleShape,
                        modifier = Modifier.height(32.dp),
                    )
                }
            }
        }

        if (state.baris.isEmpty()) {
            PlaceholderTab(
                ikon = Icons.Filled.ShoppingCartCheckout,
                judul = "Keranjang kosong",
                pesan = "Sentuh produk untuk menambah.",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(state.baris, key = { _, b -> b.itemId }) { _, baris ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        positionalThreshold = { distance -> distance * 0.5f },
                    )
                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            onHapusBaris(baris.produkId)
                        }
                    }
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(
                                contentAlignment = Alignment.CenterEnd,
                                modifier = Modifier.fillMaxSize().padding(end = 8.dp),
                            ) {
                                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.errorContainer) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(10.dp))
                                }
                            }
                        },
                    ) {
                        BarisItem(
                            baris = baris,
                            onTambah = onTambah,
                            onKurangi = onKurangi,
                        )
                    }
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column {
                        Text("Subtotal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${state.jumlahJenisItem} item", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        formatRupiah(state.subtotal),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onTahan,
                        enabled = state.keranjangAktifId != null && !state.sedangProses && state.baris.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(Icons.Filled.Pause, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("Tahan", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = onBatal,
                        enabled = state.keranjangAktifId != null && !state.sedangProses && state.baris.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(2.dp))
                        Text("Batal", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = onBayarCash,
                    enabled = state.baris.isNotEmpty() && !state.sedangProses,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.Filled.PointOfSale, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (state.sedangProses) "Memproses…" else "Bayar Tunai",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onBayarQris,
                    enabled = state.baris.isNotEmpty() && !state.sedangProses,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Bayar QRIS", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
private fun BarisItem(
    baris: BarisKeranjangUi,
    onTambah: (Long) -> Unit,
    onKurangi: (Long) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    baris.nama,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${formatRupiah(baris.hargaSatuan)} × ${baris.jumlahScaled / QUANTITY_SCALE}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                formatRupiah(baris.totalBaris),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(start = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onKurangi(baris.produkId) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Kurangi", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Text(
                        (baris.jumlahScaled / QUANTITY_SCALE).toString(),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(18.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    IconButton(onClick = { onTambah(baris.produkId) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}
