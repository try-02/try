package com.sentral.org.ui.screen.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sentral.org.data.model.QUANTITY_SCALE
import com.sentral.org.data.model.formatQuantity

enum class ModePenyesuaian(
    val label: String,
) {
    RESTOCK("Restock"),
    OPNAME("Opname"),
    RUSAK("Rusak"),
    PEMUSNAHAN("Musnahkan"),
}

@Composable
fun DialogPenyesuaianStok(
    produk: ProdukItemAdminUi,
    onDismiss: () -> Unit,
    onRestock: (jumlahScaled: Long, alasan: String) -> Unit,
    onOpname: (stokFisikScaled: Long, alasan: String) -> Unit,
    onBarangRusak: (jumlahScaled: Long, alasan: String) -> Unit,
    onPemusnahan: (jumlahScaled: Long, alasan: String) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var inputJumlah by rememberSaveable { mutableStateOf("") }
    var inputAlasan by rememberSaveable { mutableStateOf("") }
    var pesanError by remember { mutableStateOf<String?>(null) }

    val parsedQtyScaled: Long =
        remember(inputJumlah) {
            val num = inputJumlah.replace(',', '.').toDoubleOrNull() ?: 0.0
            (num * QUANTITY_SCALE).toLong()
        }

    val selisihOpname: Long =
        remember(parsedQtyScaled, selectedTab, produk.stokNormalScaled) {
            if (selectedTab == 1) parsedQtyScaled - produk.stokNormalScaled else 0L
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Column {
                Text("Penyesuaian Persediaan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(produk.nama, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Stok Baik: ${formatQuantity(produk.stokNormalScaled)} • Stok Rusak: ${formatQuantity(produk.stokRusakScaled)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    ModePenyesuaian.entries.forEachIndexed { idx, mode ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = {
                                selectedTab = idx
                                inputJumlah = ""
                                pesanError = null
                            },
                            text = { Text(mode.label, style = MaterialTheme.typography.labelMedium) },
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                val labelInput =
                    when (selectedTab) {
                        0 -> "Jumlah Masuk (Unit / Kg)"
                        1 -> "Stok Fisik Dihitung (Unit / Kg)"
                        2 -> "Jumlah Barang Rusak (Unit / Kg)"
                        else -> "Jumlah Dimusnahkan (Unit / Kg)"
                    }

                OutlinedTextField(
                    value = inputJumlah,
                    onValueChange = { v ->
                        if (v.isEmpty() || v.matches(Regex("^\\d*([.,]\\d{0,3})?$"))) {
                            inputJumlah = v
                            pesanError = null
                        }
                    },
                    label = { Text(labelInput) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (selectedTab == 1 && inputJumlah.isNotBlank()) {
                    // Penentuan palet 3-state: Defisit (Merah), Pas (Lavender), Surplus (Hijau Lime)
                    val (containerColor, contentColor, labelStatus) =
                        when {
                            selisihOpname < 0L -> {
                                Triple(
                                    MaterialTheme.colorScheme.errorContainer,
                                    MaterialTheme.colorScheme.onErrorContainer,
                                    "Defisit (Fisik Kurang)",
                                )
                            }

                            selisihOpname > 0L -> {
                                Triple(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    MaterialTheme.colorScheme.onSecondaryContainer,
                                    "Surplus (Fisik Lebih)",
                                )
                            }

                            else ->
                                Triple(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.onPrimaryContainer,
                                    "Cocok (Fisik Pas)",
                                )
                        }

                    val tanda =
                        when {
                            selisihOpname > 0L -> "+${formatQuantity(selisihOpname)}"
                            selisihOpname < 0L -> formatQuantity(selisihOpname) // Otomatis menghasilkan "-X"
                            else -> "0"
                        }

                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = containerColor,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    labelStatus,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor,
                                )
                                Text(
                                    "Sistem: ${formatQuantity(produk.stokNormalScaled)} → Fisik: ${formatQuantity(parsedQtyScaled)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = contentColor.copy(alpha = 0.8f),
                                )
                            }
                            Text(
                                "$tanda unit",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium,
                                color = contentColor,
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = inputAlasan,
                    onValueChange = {
                        inputAlasan = it
                        pesanError = null
                    },
                    label = { Text("Alasan / Keterangan (Wajib)") },
                    placeholder = {
                        Text(
                            when (selectedTab) {
                                0 -> "Misal: Faktur Pembelian INV-001"
                                1 -> "Misal: Hasil opname rak B"
                                2 -> "Misal: Kemasan sobek / kedaluwarsa"
                                else -> "Misal: Dibuang ke limbah"
                            },
                        )
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (pesanError != null) {
                    Text(pesanError.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (inputAlasan.trim().isBlank()) {
                        pesanError = "Keterangan alasan wajib diisi"
                        return@Button
                    }
                    if (parsedQtyScaled <= 0 && selectedTab != 1) {
                        pesanError = "Jumlah harus lebih dari 0"
                        return@Button
                    }

                    when (selectedTab) {
                        0 -> onRestock(parsedQtyScaled, inputAlasan.trim())
                        1 -> onOpname(parsedQtyScaled, inputAlasan.trim())
                        2 -> onBarangRusak(parsedQtyScaled, inputAlasan.trim())
                        3 -> onPemusnahan(parsedQtyScaled, inputAlasan.trim())
                    }
                },
                shape = MaterialTheme.shapes.medium,
            ) {
                Text("Terapkan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
    )
}
