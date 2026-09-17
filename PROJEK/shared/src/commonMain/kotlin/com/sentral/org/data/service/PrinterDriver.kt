package com.sentral.org.data.service

import com.sentral.org.data.model.PrintResult
import com.sentral.org.data.model.ReceiptData

/**
 * Abstraksi driver printer. Implementasi konkret ada di platform module
 * (Android pakai DantSu ESCPOS, iOS bisa pakai SDK printer lain).
 * 
 * Semua method suspend karena melibatkan I/O yang lambat.
 */
interface PrinterDriver {
    
    /**
     * Nama unik driver untuk logging. Misal: "ESC/POS Bluetooth", "ESC/POS USB".
     */
    val name: String

    /**
     * Test koneksi ke printer. Return true jika printer responsif.
     * Dipanggil oleh health monitor untuk cek status berkala.
     */
    suspend fun testConnection(): Boolean

    /**
     * Cetak struk. Implementasi bertanggung jawab:
     * 1. Buka koneksi (jika belum terbuka)
     * 2. Kirim data bytes ke printer
     * 3. Tunggu konfirmasi (jika didukung)
     * 4. Tutup koneksi (atau biarkan terbuka untuk reuse)
     */
    suspend fun print(receipt: ReceiptData): PrintResult

    /**
     * Tutup koneksi dan release resource.
     */
    suspend fun disconnect()
}

/**
 * Driver dummy untuk testing dan sebagai default saat belum ada printer 
 * terkonfigurasi. Selalu return success tanpa melakukan apa-apa.
 */
class NoOpPrinterDriver : PrinterDriver {
    override val name: String = "NoOp"
    override suspend fun testConnection(): Boolean = true
    override suspend fun print(receipt: ReceiptData): PrintResult = PrintResult.Success
    override suspend fun disconnect() {}
}