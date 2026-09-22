package com.sentral.org.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.sentral.org.data.entity.TransaksiDenganDetail
import com.sentral.org.data.model.QUANTITY_SCALE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dhatim.fastexcel.Workbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExcelReportExporter(
    private val context: Context,
) {
    /**
     * Menulis data transaksi ke dalam format Excel (.xlsx) secara streaming menggunakan FastExcel.
     * Menerima penyedia data bertahap (fetchChunk) untuk mencegah beban memori pada dataset masif.
     */
    suspend fun exportTransaksiStreaming(fetchChunk: suspend (limit: Int, offset: Int) -> List<TransaksiDenganDetail>): Uri =
        withContext(Dispatchers.IO) {
            val reportsDir = File(context.filesDir, "reports")
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }

            // ===== PEMBERSIHAN FILE USANG (Mencegah Storage Leak) =====
            // Hanya simpan maksimal 3 file laporan terbaru, sisanya dihapus permanen
            reportsDir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(3)?.forEach { oldFile ->
                try {
                    oldFile.delete()
                } catch (_: Exception) {
                }
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
            val fileName = "Laporan_Transaksi_$timeStamp.xlsx"
            val file = File(reportsDir, fileName)

            FileOutputStream(file).use { os ->
                val wb = Workbook(os, "POS Kasir", "1.0")
                val ws = wb.newWorksheet("Riwayat Transaksi")

                // Baris Header
                val headers =
                    listOf(
                        "No. Transaksi",
                        "Waktu",
                        "Kasir",
                        "Status",
                        "Rincian Produk",
                        "Subtotal (Rp)",
                        "Diskon (Rp)",
                        "Pajak (Rp)",
                        "Total (Rp)",
                        "Pembayaran",
                    )
                headers.forEachIndexed { col, title ->
                    ws.value(0, col, title)
                    ws.style(0, col).bold().set()
                }

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                var rowIdx = 1
                var offset = 0
                val chunkSize = 300 // Chunk 300 baris per batch: sangat ringan di RAM (<5MB)

                while (true) {
                    val batch = fetchChunk(chunkSize, offset)
                    if (batch.isEmpty()) break

                    for (item in batch) {
                        val trx = item.transaksi
                        val waktuStr = dateFormat.format(Date(trx.dibuatPada))
                        val itemSummary =
                            item.items.joinToString("\n") {
                                "- ${it.namaProduk} (${com.sentral.org.data.model.formatQuantity(it.jumlah)}x @ Rp ${it.hargaSatuan})"
                            }
                        val paymentSummary =
                            item.pembayaran.joinToString(", ") {
                                "${it.metode.name}: Rp ${it.jumlah}"
                            }

                        ws.value(rowIdx, 0, trx.nomorTransaksi)
                        ws.value(rowIdx, 1, waktuStr)
                        ws.value(rowIdx, 2, trx.namaKasir)
                        ws.value(rowIdx, 3, trx.status.name)
                        ws.value(rowIdx, 4, itemSummary)
                        ws.value(rowIdx, 5, trx.subtotal)
                        ws.value(rowIdx, 6, trx.diskon)
                        ws.value(rowIdx, 7, trx.pajak)
                        ws.value(rowIdx, 8, trx.total)
                        ws.value(rowIdx, 9, paymentSummary)

                        rowIdx++
                    }

                    offset += batch.size
                    if (batch.size < chunkSize) break
                }

                if (rowIdx == 1) {
                    throw IllegalStateException("Tidak ada data transaksi yang cocok untuk diekspor")
                }

                wb.finish()
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        }
}
