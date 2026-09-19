package com.sentral.org.export

import android.net.Uri
import com.sentral.org.data.model.TransaksiFilter
import com.sentral.org.data.repository.TransaksiRepository

class TransaksiExportUseCase(
    private val transaksiRepo: TransaksiRepository,
    private val excelExporter: ExcelReportExporter,
) {
    suspend fun exportToExcel(filter: TransaksiFilter): Result<Uri> = runCatching {
        val data = transaksiRepo.getTransaksiForExport(filter)
        if (data.isEmpty()) {
            throw IllegalStateException("Tidak ada transaksi untuk diekspor pada filter ini")
        }
        excelExporter.exportTransaksi(data)
    }
}