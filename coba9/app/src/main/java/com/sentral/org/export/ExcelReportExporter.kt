package com.sentral.org.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.sentral.org.data.entity.TransaksiDenganDetail
import com.sentral.org.data.model.QUANTITY_SCALE
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dhatim.fastexcel.Workbook

class ExcelReportExporter(private val context: Context) {

    /**
     * Menulis data transaksi ke dalam format Excel (.xlsx) secara streaming menggunakan FastExcel.
     * Mengembalikan Uri FileProvider yang aman untuk dibagikan / dibuka aplikasi lain.
     */
    suspend fun exportTransaksi(list: List<TransaksiDenganDetail>): Uri = withContext(Dispatchers.IO) {
        // Folder 'reports' sesuai dengan deklarasi di file_paths.xml (<files-path path="reports/" />)
        val reportsDir = File(context.filesDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
        val fileName = "Laporan_Transaksi_$timeStamp.xlsx"
        val file = File(reportsDir, fileName)

        FileOutputStream(file).use { os ->
            val wb = Workbook(os, "POS Kasir", "1.0")
            val ws = wb.newWorksheet("Riwayat Transaksi")

            // ===== Baris Header =====
            val headers = listOf(
                "No. Transaksi", "Waktu", "Kasir", "Status",
                "Rincian Produk", "Subtotal (Rp)", "Diskon (Rp)", "Pajak (Rp)", "Total (Rp)", "Pembayaran"
            )
            headers.forEachIndexed { col, title ->
                ws.value(0, col, title)
                ws.style(0, col).bold().set()
            }

            // ===== Baris Data =====
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            var rowIdx = 1

            for (item in list) {
                val trx = item.transaksi
                val waktuStr = dateFormat.format(Date(trx.dibuatPada))
                val itemSummary = item.items.joinToString("\n") {
                    "- ${it.namaProduk} (${it.jumlah / QUANTITY_SCALE}x @ Rp ${it.hargaSatuan})"
                }
                val paymentSummary = item.pembayaran.joinToString(", ") {
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

            wb.finish()
        }

        // Generate content:// URI via FileProvider
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }
}