package com.sentral.org.data.service

import com.sentral.org.data.dao.PrinterDao
import com.sentral.org.data.entity.PrinterEntity
import com.sentral.org.data.model.PrintResult
import com.sentral.org.data.model.PrinterStatus
import com.sentral.org.data.model.ReceiptData
import com.sentral.org.data.model.suspendRunCatching
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Service yang mengelola antrian cetak dan health tracking printer.
 * 
 * DESAIN:
 * - Auto-load default printer saat inisialisasi
 * - Queue berbasis Channel (unlimited buffer) agar tidak ada job yang hilang
 * - Proses sequential: satu job selesai baru lanjut berikutnya
 * - Health tracking: gagal 3x berturut-turut → printer dinonaktifkan otomatis
 */
class PrinterService(
    private val printerDao: PrinterDao,
    private val driverFactory: (PrinterEntity) -> PrinterDriver,
    private val scope: CoroutineScope,
) {
    private val printQueue = Channel<PrintJob>(Channel.UNLIMITED)
    private val _status = MutableStateFlow(PrinterStatus.SIAP)
    val status: StateFlow<PrinterStatus> = _status.asStateFlow()

    private var activeDriver: PrinterDriver = NoOpPrinterDriver()
    private var activePrinter: PrinterEntity? = null
    private var isInitialized = false

    /**
     * Ambang batas kegagalan sebelum printer dinonaktifkan otomatis.
     */
    private companion object {
        const val MAX_CONSECUTIVE_FAILURES = 3
    }

    data class PrintJob(
        val receipt: ReceiptData,
        val onResult: (PrintResult) -> Unit = {},
    )

    init {
        // ===== AUTO-LOAD: Load default printer saat service dibuat =====
        scope.launch {
            initializeDefaultPrinter()
        }
        
        // Mulai worker queue
        scope.launch { processQueue() }
    }

    /**
     * Load printer default dari database dan set sebagai active driver.
     * Dipanggil otomatis saat PrinterService diinisialisasi.
     */
    private suspend fun initializeDefaultPrinter() {
        try {
            val defaultPrinter = printerDao.getDefault()
            if (defaultPrinter != null && !defaultPrinter.dinonaktifkanOtomatis) {
                activeDriver = driverFactory(defaultPrinter)
                activePrinter = defaultPrinter
                _status.value = PrinterStatus.SIAP
                android.util.Log.e("PrinterService", "✅ Initialized with printer: ${defaultPrinter.nama}")
            } else {
                android.util.Log.w("PrinterService", "⚠️ No default printer found, using NoOp driver")
            }
            isInitialized = true
        } catch (e: Exception) {
            android.util.Log.e("PrinterService", "❌ Failed to initialize printer: ${e.message}")
            isInitialized = true
        }
    }

    /**
     * Enqueue job cetak. Return immediately, tidak menunggu cetak selesai.
     */
    fun enqueue(receipt: ReceiptData, onResult: (PrintResult) -> Unit = {}) {
        // Pastikan printer sudah di-initialize sebelum enqueue
        if (!isInitialized) {
            scope.launch {
                initializeDefaultPrinter()
                printQueue.trySend(PrintJob(receipt, onResult))
            }
        } else {
            printQueue.trySend(PrintJob(receipt, onResult))
        }
    }

    /**
     * Ganti printer aktif. Dipanggil saat user pilih printer di settings.
     */
    suspend fun setActivePrinter(printerId: Long): Result<Unit> = suspendRunCatching {
        val printer = printerDao.getDefault()
            ?: throw IllegalArgumentException("Printer default tidak ditemukan")
        
        activeDriver.disconnect()
        activeDriver = driverFactory(printer)
        activePrinter = printer
        _status.value = PrinterStatus.SIAP
        android.util.Log.e("PrinterService", "🔄 Switched to printer: ${printer.nama}")
    }

    /**
     * Reload printer dari database. Dipanggil setelah add/edit/delete printer.
     */
    suspend fun reloadPrinter() {
        initializeDefaultPrinter()
    }

    /**
     * Worker utama: proses job satu per satu dari queue.
     */
    private suspend fun processQueue() {
        for (job in printQueue) {
            // Tunggu sampai printer ter-initialize
            while (!isInitialized) {
                kotlinx.coroutines.delay(100)
            }
            
            _status.value = PrinterStatus.SIBUK
            
            val result = activeDriver.print(job.receipt)
            
            when (result) {
                is PrintResult.Success -> {
                    onPrintSuccess()
                }
                is PrintResult.Failure -> {
                    onPrintFailure(result)
                }
            }
            
            job.onResult(result)
            
            if (_status.value == PrinterStatus.SIBUK) {
                _status.value = PrinterStatus.SIAP
            }
        }
    }

    private suspend fun onPrintSuccess() {
        val printer = activePrinter ?: return
        // Reset counter kegagalan
        printerDao.updateHealth(printer.id, failures = 0, disabled = false)
    }

    private suspend fun onPrintFailure(failure: PrintResult.Failure) {
        val printer = activePrinter ?: return
        
        val current = printer.gagalStatusBerturut + 1
        val shouldDisable = current >= MAX_CONSECUTIVE_FAILURES
        
        printerDao.updateHealth(
            id = printer.id,
            failures = current,
            disabled = shouldDisable,
        )
        
        if (shouldDisable) {
            _status.value = PrinterStatus.DINONAKTIFKAN
            android.util.Log.e("PrinterService", "🚫 Printer disabled after $current failures")
        } else {
            _status.value = PrinterStatus.ERROR
            android.util.Log.e("PrinterService", "⚠️ Print failed (${current}/$MAX_CONSECUTIVE_FAILURES): ${failure.message}") // ini log w loh
        }
    }

    /**
     * Cek koneksi printer aktif. Dipanggil berkala oleh health monitor.
     */
    suspend fun checkHealth(): Boolean {
        return activeDriver.testConnection()
    }

    /**
     * Release semua resource. Dipanggil saat aplikasi ditutup.
     */
    suspend fun shutdown() {
        activeDriver.disconnect()
    }
}