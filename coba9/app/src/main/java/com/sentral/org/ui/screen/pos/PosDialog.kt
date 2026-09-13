package com.sentral.org.ui.screen.pos

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
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import com.sentral.org.ui.screen.pos.formatRupiah

// ============================================================
// Kumpulan dialog layar kasir — Compact Edition.
// Semua dialog dipadatkan untuk smartphone: spacing lebih rapat,
// font lebih kecil, ikon lebih kecil, height dikurangi.
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
        shape = MaterialTheme.shapes.medium,
        icon = if (ikonHapus) {
            {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp),
                )
            }
        } else {
            null
        },
        title = {
            Text(
                judul,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                deskripsi,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onKonfirmasi()
                    onTutup()
                },
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(teksKonfirmasi, style = MaterialTheme.typography.labelLarge)
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
 * Dialog pembayaran compact: pilih metode via SplitButton (Tunai/QRIS),
 * numpad padat untuk nominal tunai, kembalian dihitung real-time.
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
        shape = MaterialTheme.shapes.medium,
        title = {
            Column {
                Text(
                    "Pembayaran",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Total: ${formatRupiah(total)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (metode) {
                    1 -> {
                        // --- Panel QRIS (compact) ---
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                            ) {
                                Icon(
                                    Icons.Filled.QrCode2,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    formatRupiah(total),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Text(
                                    "Minta pelanggan scan kode QRIS di mesin EDC.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }

                    else -> {
                        // --- Panel tunai: input + chip nominal + numpad ---
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    "Uang diterima",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    if (teksInput.isBlank()) "Rp 0" else formatRupiah(diterima),
                                    textAlign = TextAlign.End,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (cukup) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                if (diterima > 0) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        if (cukup) "Kembalian: ${formatRupiah(selisih)}"
                                        else "Kurang ${formatRupiah(-selisih)}",
                                        color = if (cukup) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.End,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            ChipNominal("Pas", dipilih = cukup && diterima == total) {
                                teksInput = total.toString()
                            }
                            ChipNominal("50rb", dipilih = diterima == 50_000L) { teksInput = "50000" }
                            ChipNominal("100rb", dipilih = diterima == 100_000L) { teksInput = "100000" }
                            ChipNominal("200rb", dipilih = diterima == 200_000L) { teksInput = "200000" }
                        }

                        Spacer(Modifier.height(6.dp))
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

/** Chip nominal cepat compact. */
@Composable
private fun ChipNominal(label: String, dipilih: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (dipilih) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (dipilih) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        tombol.forEach { barisTombol ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                barisTombol.forEach { t ->
                    val aksiHapus = t == "C" || t == "⌫"
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (aksiHapus) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable(onClick = { onTekan(t) }),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (t == "⌫") {
                                Icon(
                                    Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Hapus satu angka",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            } else {
                                Text(
                                    t,
                                    style = MaterialTheme.typography.titleSmall,
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

/** Dialog sukses setelah checkout: nomor transaksi + kembalian (compact). */
@Composable
fun DialogCheckoutBerhasil(
    nomorTransaksi: String,
    kembalian: Long,
    onTutup: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onTutup,
        shape = MaterialTheme.shapes.medium,
        icon = {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(44.dp),
            )
        },
        title = {
            Text(
                "Transaksi Berhasil",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        nomorTransaksi,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Kembalian",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    formatRupiah(kembalian),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onTutup,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text("Selesai", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}
