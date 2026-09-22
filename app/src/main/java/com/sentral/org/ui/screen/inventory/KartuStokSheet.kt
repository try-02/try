package com.sentral.org.ui.screen.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sentral.org.data.model.JenisPergerakanPersediaan
import com.sentral.org.data.model.formatQuantity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KartuStokSheet(
    produk: ProdukItemAdminUi,
    ledgerList: List<KartuStokItemUi>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text("Kartu Stok & Jejak Audit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(produk.nama, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Saldo Normal: ${formatQuantity(produk.stokNormalScaled)} unit | Saldo Rusak: ${formatQuantity(produk.stokRusakScaled)} unit",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))

            if (ledgerList.isEmpty()) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("Belum ada riwayat mutasi persediaan", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(ledgerList, key = { it.id }) { row ->
                        BarisKartuStok(row = row)
                    }
                }
            }
        }
    }
}

@Composable
private fun BarisKartuStok(
    row: KartuStokItemUi,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (row.jenis) {
                        JenisPergerakanPersediaan.STOK_AWAL -> MaterialTheme.colorScheme.primaryContainer
                        JenisPergerakanPersediaan.STOK_MASUK -> MaterialTheme.colorScheme.secondaryContainer
                        JenisPergerakanPersediaan.PENJUALAN -> MaterialTheme.colorScheme.surface
                        JenisPergerakanPersediaan.PEMBATALAN_PENJUALAN -> MaterialTheme.colorScheme.tertiaryContainer
                        JenisPergerakanPersediaan.PENGEMBALIAN_NORMAL, JenisPergerakanPersediaan.PENGEMBALIAN_RUSAK -> MaterialTheme.colorScheme.secondaryContainer
                        JenisPergerakanPersediaan.PENYESUAIAN -> MaterialTheme.colorScheme.primaryContainer
                        JenisPergerakanPersediaan.KERUSAKAN -> MaterialTheme.colorScheme.errorContainer
                        JenisPergerakanPersediaan.PEMULIHAN_KERUSAKAN -> MaterialTheme.colorScheme.secondaryContainer
                        JenisPergerakanPersediaan.PEMUSNAHAN -> MaterialTheme.colorScheme.errorContainer
                    },
                ) {
                    Text(
                        row.jenis.name.replace('_', ' '),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                Text(row.waktuFormatted, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        "Mutasi: ${if (row.perubahanJumlah >= 0) "+${formatQuantity(row.perubahanJumlah)}" else formatQuantity(row.perubahanJumlah)} unit",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (row.perubahanJumlah >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    if (row.perubahanJumlahRusak != 0L) {
                        Text(
                            "Mutasi Rusak: ${if (row.perubahanJumlahRusak > 0) "+${formatQuantity(row.perubahanJumlahRusak)}" else formatQuantity(row.perubahanJumlahRusak)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Saldo: ${formatQuantity(row.saldoJumlahSetelah)} unit",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Sebelum: ${formatQuantity(row.saldoJumlahSebelum)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (row.keterangan.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    row.keterangan,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}