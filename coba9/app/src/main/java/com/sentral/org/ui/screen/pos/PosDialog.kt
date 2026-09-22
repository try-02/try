package com.sentral.org.ui.screen.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.sentral.org.data.model.MoneyMath
import com.sentral.org.data.model.QUANTITY_SCALE
import com.sentral.org.data.model.formatQuantity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import com.sentral.org.ui.screen.pos.formatRupiah

// ============================================================
// Kumpulan dialog layar kasir — Material 3 Expressive Edition.
// Dialog lebih bold, hierarki jelas, dengan ciri khas CobaApp.
// ============================================================

/** Dialog konfirmasi generik untuk aksi destruktif ringan. */
@Composable
fun DialogKonfirmasi(
    judul: String,
    deskripsi: String,
    teksKonfirmasi: String,
    onKonfirmasi: () -> Unit,
    onTutup: () -> Unit,
    ikonHapus: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onTutup,
        shape = MaterialTheme.shapes.extraLarge,
        icon = if (ikonHapus) {
            {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        } else {
            null
        },
        title = {
            Text(
                judul,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                deskripsi,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onKonfirmasi()
                    onTutup()
                },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.height(44.dp),
            ) {
                Text(teksKonfirmasi, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onTutup) {
                Text("Kembali", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

/**
 * Dialog pembayaran expressif: pilih metode via SplitButton (Tunai/QRIS),
 * numpad dengan feedback visual yang jelas, kembalian dihitung real-time.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DialogPembayaran(
    total: Long,
    sedangProses: Boolean,
    onKonfirmasiTunai: (Long) -> Unit,
    onKonfirmasiQris: () -> Unit,
    metodeAwal: Int = 0, // 0 = Tunai, 1 = QRIS
    onTutup: () -> Unit,
) {
    var metode by rememberSaveable { mutableIntStateOf(metodeAwal) }
    var teksInput by rememberSaveable { mutableStateOf("") }
    val diterima = teksInput.filter(Char::isDigit).toLongOrNull() ?: 0L
    val selisih = diterima - total
    val cukup = total > 0 && diterima >= total
    val totalValid = total > 0

    fun tekan(tombol: String) {
        when (tombol) {
            "C" -> teksInput = ""
            "⌫" -> teksInput = teksInput.dropLast(1)
            else -> teksInput = (teksInput.filter(Char::isDigit) + tombol).take(12)
        }
    }

    AlertDialog(
        onDismissRequest = onTutup,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Column {
                Text(
                    "Pembayaran",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Total: ${formatRupiah(total)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (metode) {
                    1 -> {
                        // --- Panel QRIS (expressif dengan gradient) ---
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.secondaryContainer,
                                            MaterialTheme.colorScheme.primaryContainer,
                                        )
                                    ),
                                    shape = MaterialTheme.shapes.large,
                                )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp),
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(64.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Filled.QrCode2,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    formatRupiah(total),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Minta pelanggan scan kode QRIS di mesin EDC.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }

                    else -> {
                        // --- Panel tunai: input + chip nominal + numpad ---
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Text(
                                    "Uang diterima",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    if (teksInput.isBlank()) "Rp 0" else formatRupiah(diterima),
                                    textAlign = TextAlign.End,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = if (cukup) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                if (diterima > 0) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        if (cukup) "Kembalian: ${formatRupiah(selisih)}"
                                        else "Kurang ${formatRupiah(-selisih)}",
                                        color = if (cukup) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.End,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            ChipNominal("Pas", dipilih = cukup && diterima == total, modifier = Modifier.weight(1f)) {
                                teksInput = total.toString()
                            }
                            ChipNominal("50rb", dipilih = diterima == 50_000L, modifier = Modifier.weight(1f)) { teksInput = "50000" }
                            ChipNominal("100rb", dipilih = diterima == 100_000L, modifier = Modifier.weight(1f)) { teksInput = "100000" }
                            ChipNominal("200rb", dipilih = diterima == 200_000L, modifier = Modifier.weight(1f)) { teksInput = "200000" }
                        }

                        Spacer(Modifier.height(12.dp))
                        Numpad(onTekan = ::tekan)
                    }
                }
            }
        },
        confirmButton = {
            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.LeadingButton(
                        enabled = !sedangProses && totalValid && (metode == 1 || cukup),
                        onClick = {
                            if (metode == 1) onKonfirmasiQris() else onKonfirmasiTunai(diterima)
                        },
                    ) {
                        if (sedangProses) {
                            ContainedLoadingIndicator(
                                modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                            )
                        } else {
                            Icon(
                                imageVector = if (metode == 1) Icons.Filled.QrCode2 else Icons.Filled.PointOfSale,
                                modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                                contentDescription = null,
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text(
                                "Selesaikan",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                },
                trailingButton = {
                    SplitButtonDefaults.TrailingButton(
                        checked = metode == 1,
                        onCheckedChange = { metode = if (it) 1 else 0 },
                    ) {
                        Icon(
                            imageVector = if (metode == 1) Icons.Filled.QrCode2 else Icons.Filled.PointOfSale,
                            modifier = Modifier.size(SplitButtonDefaults.TrailingIconSize),
                            contentDescription = "Ganti metode pembayaran",
                        )
                    }
                },
            )
        },
        dismissButton = {
            TextButton(onClick = onTutup) {
                Text("Batal", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

/** Chip nominal cepat expressif dengan shape yang lebih bold. */
@Composable
private fun ChipNominal(
    label: String,
    dipilih: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (dipilih) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (dipilih) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Numpad(onTekan: (String) -> Unit) {
    val tombol = listOf(
        listOf("7", "8", "9"),
        listOf("4", "5", "6"),
        listOf("1", "2", "3"),
        listOf("C", "0", "⌫"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tombol.forEach { barisTombol ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                barisTombol.forEach { t ->
                    val aksiHapus = t == "C" || t == "⌫"
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (aksiHapus) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clickable(onClick = { onTekan(t) }),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (t == "⌫") {
                                Icon(
                                    Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Hapus satu angka",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(24.dp),
                                )
                            } else {
                                Text(
                                    t,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (aksiHapus) MaterialTheme.colorScheme.onErrorContainer
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Dialog sukses setelah checkout: dengan gradient dan celebratory feel. */
@Composable
fun DialogCheckoutBerhasil(
    nomorTransaksi: String,
    kembalian: Long,
    onTutup: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onTutup,
        shape = MaterialTheme.shapes.extraLarge,
        icon = {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.primary,
                            )
                        ),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }
        },
        title = {
            Text(
                "Transaksi Berhasil!",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        nomorTransaksi,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Kembalian",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatRupiah(kembalian),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onTutup,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text("Selesai", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        },
    )
}

/**
 * Dialog input kuantitas manual: mendukung bilangan desimal (koma / titik)
 * untuk produk timbangan / pecahan (mis. 0.5 kg, 1.25 unit).
 */
@Composable
fun DialogUbahJumlah(
    namaProduk: String,
    hargaSatuan: Long,
    jumlahAwalScaled: Long,
    onKonfirmasi: (Long) -> Unit,
    onTutup: () -> Unit,
) {
    var inputTeks by rememberSaveable {
        mutableStateOf(formatQuantity(jumlahAwalScaled).replace(',', '.'))
    }

    // Normalisasi input ke kuantitas scaled (mis. 0.5 -> 500)
    val parsedScaled: Long = remember(inputTeks) {
        val clean = inputTeks.replace(',', '.').trim()
        val num = clean.toDoubleOrNull() ?: 0.0
        if (num > 0.0) (num * QUANTITY_SCALE).toLong() else 0L
    }

    val totalBaris = remember(parsedScaled, hargaSatuan) {
        if (parsedScaled > 0) MoneyMath.lineTotal(hargaSatuan, parsedScaled) else 0L
    }

    AlertDialog(
        onDismissRequest = onTutup,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Column {
                Text(
                    "Ubah Jumlah Barang",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    namaProduk,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = inputTeks,
                    onValueChange = { baru: String ->
                        // Hanya izinkan angka, maksimal 1 tanda koma/titik, dan maks 3 digit pecahan
                        if (baru.isEmpty() || baru.matches(Regex("^\\d*([.,]\\d{0,3})?$"))) {
                            inputTeks = baru
                        }
                    },
                    label = { Text("Kuantitas (Unit / Kg)") },
                    placeholder = { Text("Contoh: 0.5 atau 1.25") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Chip shortcut penambahan cepat
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    listOf("0.25", "0.5", "1", "2").forEach { preset ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { inputTeks = preset },
                        ) {
                            Text(
                                text = preset,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                }

                // Kalkulasi harga live
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Subtotal Baris:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            formatRupiah(totalBaris),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onKonfirmasi(parsedScaled)
                    onTutup()
                },
                enabled = parsedScaled >= 0,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(if (parsedScaled == 0L) "Hapus Item" else "Terapkan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onTutup) {
                Text("Batal")
            }
        },
    )
}