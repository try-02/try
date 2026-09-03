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
import androidx.compose.material3.SplitButton
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

/** Fungsi pembantu sederhana untuk format Rupiah jika belum tersedia secara global
private fun formatRupiah(amount: Long): String {
    return "Rp %,d".format(amount).replace(',', '.')
}
*/
// ============================================================
// Kumpulan dialog layar kasir. Semua memakai bentuk extraLarge
// (28.dp) sesuai design system M3 Expressive.
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
        icon = if (ikonHapus) {
            {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        } else {
            null
        },
        title = { Text(judul, fontWeight = FontWeight.Bold) },
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) { Text(teksKonfirmasi) }
        },
        dismissButton = {
            TextButton(onClick = onTutup) { Text("Kembali") }
        },
    )
}

/**
 * Dialog pembayaran (spek 4.3): pilih metode via SplitButton (Tunai/QRIS)
 * (domain hanya punya TUNAI & QRIS), numpad besar untuk nominal tunai,
 * kembalian dihitung real-time.
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
    // QRIS tetap butuh total > 0 agar tidak bisa dikonfirmasi saat keranjang kosong/nol.
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
        title = {
            Column {
                Text("Pembayaran", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Total belanja ${formatRupiah(total)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Metode pembayaran dipilih lewat SplitButton di confirmButton di bawah.
                Spacer(Modifier.height(12.dp))

                when (metode) {
                    1 -> {
                        // --- Panel QRIS ---
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 22.dp),
                            ) {
                                Icon(
                                    Icons.Filled.QrCode2,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    formatRupiah(total),
                                    style = MaterialTheme.typography.headlineSmall,
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
                            shape = MaterialTheme.shapes.medium,
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
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.End,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ChipNominal("Pas", dipilih = cukup && diterima == total) {
                                teksInput = total.toString()
                            }
                            ChipNominal("50rb", dipilih = diterima == 50_000L) { teksInput = "50000" }
                            ChipNominal("100rb", dipilih = diterima == 100_000L) { teksInput = "100000" }
                            ChipNominal("200rb", dipilih = diterima == 200_000L) { teksInput = "200000" }
                        }

                        Spacer(Modifier.height(10.dp))
                        Numpad(onTekan = ::tekan)
                    }
                }
            }
        },
        confirmButton = {
            SplitButton( // <-- Ganti SplitButtonLayout menjadi SplitButton
                leadingButton = {
                    SplitButtonDefaults.LeadingButton(
                        // Tombol aktif hanya jika total valid DAN (QRIS ATAU tunai cukup)
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
                                "Selesaikan • ${if (metode == 1) "QRIS" else "Tunai"}",
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
            TextButton(onClick = onTutup) { Text("Batal") }
        },
    )
}

/** Chip nominal cepat fully-rounded. */
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
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (dipilih) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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
                        shape = MaterialTheme.shapes.small,
                        color = if (aksiHapus) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clickable(onClick = { onTekan(t) }),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (t == "⌫") {
                                Icon(
                                    Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Hapus satu angka",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
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

/** Dialog sukses setelah checkout: nomor transaksi + kembalian. */
@Composable
fun DialogCheckoutBerhasil(
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
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Kembalian",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    formatRupiah(kembalian),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onTutup,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Text("Selesai", style = MaterialTheme.typography.titleMedium)
            }
        },
    )
}
