package com.sentral.org.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.dantsu.escposprinter.EscPosCharsetEncoding
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.tcp.TcpConnection
import com.dantsu.escposprinter.exceptions.EscPosConnectionException
import com.dantsu.escposprinter.exceptions.EscPosEncodingException
import com.dantsu.escposprinter.exceptions.EscPosParserException
import com.sentral.org.data.entity.PrinterEntity
import com.sentral.org.data.repository.PrinterRepository
import com.sentral.org.data.service.PrinterService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class BluetoothDeviceUi(
    val name: String,
    val address: String,
    val isPaired: Boolean,
)

sealed interface PrinterTestResult {
    data object Testing : PrinterTestResult

    data class Success(
        val printer: PrinterEntity,
    ) : PrinterTestResult

    data class Failed(
        val message: String,
    ) : PrinterTestResult
}

@SuppressLint("MissingPermission")
class AddPrinterViewModel(
    application: Application,
    private val printerRepo: PrinterRepository,
    private val printerService: PrinterService, // ← TAMBAH
) : AndroidViewModel(application) {
    private val _bluetoothDevices = MutableStateFlow<List<BluetoothDeviceUi>>(emptyList())
    val bluetoothDevices: StateFlow<List<BluetoothDeviceUi>> = _bluetoothDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(true)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _scanMessage = MutableSharedFlow<String>()
    val scanMessage: SharedFlow<String> = _scanMessage.asSharedFlow()

    private val _testResult = MutableSharedFlow<PrinterTestResult>()
    val testResult: SharedFlow<PrinterTestResult> = _testResult.asSharedFlow()

    private var scanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private val foundDevices = mutableMapOf<String, BluetoothDeviceUi>()
    private var isClassicReceiverRegistered = false

    private val classicDiscoveryReceiver =
        object : android.content.BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device =
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            } ?: return

                        val name =
                            try {
                                device.name ?: "Unknown Device"
                            } catch (_: SecurityException) {
                                "Unknown Device"
                            }
                        val address = device.address
                        val isPaired = device.bondState == BluetoothDevice.BOND_BONDED

                        if (!foundDevices.containsKey(address)) {
                            foundDevices[address] =
                                BluetoothDeviceUi(
                                    name = name,
                                    address = address,
                                    isPaired = isPaired,
                                )
                            _bluetoothDevices.value = foundDevices.values.toList()
                        }
                    }
                }
            }
        }

    fun checkBluetoothEnabled(): Boolean {
        val context = getApplication<Application>()
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter
        val enabled = adapter != null && adapter.isEnabled
        _isBluetoothEnabled.value = enabled
        return enabled
    }

    fun setBluetoothEnabled(enabled: Boolean) {
        _isBluetoothEnabled.value = enabled
        if (!enabled) {
            stopScan()
        }
    }

    companion object {
        private const val TAG = "AddPrinterVM"
        private val log = Logger.withTag(TAG)
        private const val SCAN_DURATION_MS = 15000L // 15 detik
        private const val CONNECTION_TIMEOUT_MS = 10000L // 10 detik
        private const val PRINTER_DPI = 203
        private const val PRINTER_WIDTH_MM = 80f // 80mm printer
        private const val CHARS_PER_LINE = 48 // Standard untuk 80mm printer
        private val CHARSET_UTF8 = EscPosCharsetEncoding("UTF-8", 28)
    }

    fun startBluetoothScan() {
        if (_isScanning.value) return

        val context = getApplication<Application>()
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null) {
            _isBluetoothEnabled.value = false
            viewModelScope.launch {
                _scanMessage.emit("Perangkat ini tidak mendukung Bluetooth.")
            }
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            _isBluetoothEnabled.value = false
            _isScanning.value = false
            viewModelScope.launch {
                _scanMessage.emit("Bluetooth sedang tidak aktif. Harap nyalakan Bluetooth terlebih dahulu.")
            }
            return
        }

        _isBluetoothEnabled.value = true

        viewModelScope.launch {
            _isScanning.value = true
            _scanProgress.value = 0f
            foundDevices.clear()
            _bluetoothDevices.value = emptyList()

            try {
                // Muat langsung perangkat yang sudah di-pair di Android Settings dengan guard SecurityException
                val pairedDevices = bluetoothAdapter.bondedDevices.orEmpty()
                for (device in pairedDevices) {
                    val name =
                        try {
                            device.name ?: "Unknown Device"
                        } catch (_: SecurityException) {
                            "Printer Bluetooth"
                        }
                    val address = device.address
                    foundDevices[address] =
                        BluetoothDeviceUi(
                            name = name,
                            address = address,
                            isPaired = true,
                        )
                }
                _bluetoothDevices.value = foundDevices.values.toList()
            } catch (e: SecurityException) {
                log.e(e) { "Izin BLUETOOTH_CONNECT tidak tersedia untuk mengakses bondedDevices" }
                _scanMessage.emit("Izin koneksi Bluetooth belum diberikan.")
            }

            // 1. Jalankan pemindaian Bluetooth Classic (SPP/RFCOMM) via startDiscovery
            try {
                val app = getApplication<Application>()
                if (!isClassicReceiverRegistered) {
                    val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                    androidx.core.content.ContextCompat.registerReceiver(
                        app,
                        classicDiscoveryReceiver,
                        filter,
                        androidx.core.content.ContextCompat.RECEIVER_EXPORTED,
                    )
                    isClassicReceiverRegistered = true
                }
                if (bluetoothAdapter.isDiscovering) {
                    bluetoothAdapter.cancelDiscovery()
                }
                bluetoothAdapter.startDiscovery()
            } catch (e: SecurityException) {
                log.w { "Tidak dapat memulai Classic Discovery: ${e.message}" }
            }

            // 2. Jalankan pemindaian BLE sebagai pelengkap
            scanner = bluetoothAdapter.bluetoothLeScanner
            if (scanner != null) {
                scanCallback =
                    object : ScanCallback() {
                        override fun onScanResult(
                            callbackType: Int,
                            result: ScanResult,
                        ) {
                            val device = result.device
                            val name =
                                try {
                                    device.name ?: "Unknown Device"
                                } catch (_: SecurityException) {
                                    "Unknown Device"
                                }
                            val address = device.address
                            val isPaired = device.bondState == BluetoothDevice.BOND_BONDED

                            if (!foundDevices.containsKey(address)) {
                                foundDevices[address] =
                                    BluetoothDeviceUi(
                                        name = name,
                                        address = address,
                                        isPaired = isPaired,
                                    )
                                _bluetoothDevices.value = foundDevices.values.toList()
                            }
                        }

                        override fun onScanFailed(errorCode: Int) {
                            log.w { "BLE Scan gagal dengan kode: $errorCode" }
                        }
                    }

                try {
                    scanner?.startScan(scanCallback)
                } catch (e: SecurityException) {
                    log.w { "Tidak dapat memulai BLE Scan: ${e.message}" }
                }
            }

            // Progress animation
            val startTime = System.currentTimeMillis()
            while (_isScanning.value) {
                val elapsed = System.currentTimeMillis() - startTime
                _scanProgress.value = (elapsed.toFloat() / SCAN_DURATION_MS).coerceIn(0f, 1f)

                if (elapsed >= SCAN_DURATION_MS) {
                    stopScan()
                    break
                }

                delay(100)
            }
        }
    }

    private fun stopScan() {
        // Hentikan Bluetooth Classic Discovery
        try {
            val context = getApplication<Application>()
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter
            if (adapter?.isDiscovering == true) {
                adapter.cancelDiscovery()
            }
            if (isClassicReceiverRegistered) {
                context.unregisterReceiver(classicDiscoveryReceiver)
                isClassicReceiverRegistered = false
            }
        } catch (_: Exception) {
        }

        // Hentikan BLE Scanner
        try {
            scanner?.stopScan(scanCallback)
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }

        scanCallback = null
        _isScanning.value = false
        _scanProgress.value = 0f
    }

/**
     fun testBluetoothConnection(device: BluetoothDeviceUi) {
     if (!checkBluetoothEnabled()) {
     viewModelScope.launch {
     _testResult.emit(PrinterTestResult.Failed("Bluetooth tidak aktif. Silakan nyalakan Bluetooth."))
     }
     return
     }

     // Matikan proses discovery terlebih dahulu agar bandwidth RFCOMM tidak terganggu
     stopScan()

     viewModelScope.launch {
     _testResult.emit(PrinterTestResult.Testing)

     val result = withContext(Dispatchers.IO) {
     withTimeoutOrNull(CONNECTION_TIMEOUT_MS) {
     try {
     val context = getApplication<Application>()
     val bluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
     val bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.address)
     val connection = BluetoothConnection(bluetoothDevice)

     val printer = EscPosPrinter(
     connection,
     PRINTER_DPI,
     PRINTER_WIDTH_MM,
     CHARS_PER_LINE,
     CHARSET_UTF8,
     )

     // Test print
     printer.printFormattedText("[C]TEST CONNECTION\n")
     printer.disconnectPrinter()

     // Create printer entity
     PrinterEntity(
     nama = device.name,
     tipeKoneksi = "BLUETOOTH",
     isDefault = false,
     prioritas = 1,
     karakterPerBaris = CHARS_PER_LINE,
     lebarKertas = "80mm",
     mendukungStatus = true,
     alamatBluetooth = device.address,
     alamatWifi = null,
     portWifi = null,
     usbVendorId = null,
     usbProductId = null,
     dibuatPada = System.currentTimeMillis(),
     gagalStatusBerturut = 0,
     dinonaktifkanOtomatis = false,
     )
     } catch (e: Exception) {
     null
     }
     }
     }

     if (result != null) {
     _testResult.emit(PrinterTestResult.Success(result))
     } else {
     _testResult.emit(PrinterTestResult.Failed("Tidak dapat terhubung ke printer. Pastikan printer menyala dan dalam jangkauan."))
     }
     }
     } */
    fun testBluetoothConnection(device: BluetoothDeviceUi) {
        if (!checkBluetoothEnabled()) {
            viewModelScope.launch {
                _testResult.emit(
                    PrinterTestResult.Failed(
                        "Bluetooth tidak aktif. Silakan nyalakan Bluetooth.",
                    ),
                )
            }
            return
        }

        stopScan()

        viewModelScope.launch {
            _testResult.emit(PrinterTestResult.Testing)

            try {
                val result =
                    withContext(Dispatchers.IO) {
                        withTimeoutOrNull(CONNECTION_TIMEOUT_MS) {
                            val context = getApplication<Application>()

                            val bluetoothAdapter =
                                (
                                    context.getSystemService(
                                        Context.BLUETOOTH_SERVICE,
                                    ) as BluetoothManager
                                ).adapter

                            val bluetoothDevice =
                                bluetoothAdapter.getRemoteDevice(device.address)

                            val connection =
                                BluetoothConnection(bluetoothDevice)

                            val printer =
                                EscPosPrinter(
                                    connection,
                                    PRINTER_DPI,
                                    PRINTER_WIDTH_MM,
                                    CHARS_PER_LINE,
                                    CHARSET_UTF8,
                                )

                            try {
                                printer.printFormattedText(
                                    "[C]TEST CONNECTION\n",
                                )

                                PrinterEntity(
                                    nama = device.name,
                                    tipeKoneksi = "BLUETOOTH",
                                    isDefault = false,
                                    prioritas = 1,
                                    karakterPerBaris = CHARS_PER_LINE,
                                    lebarKertas = "80mm",
                                    mendukungStatus = true,
                                    alamatBluetooth = device.address,
                                    alamatWifi = null,
                                    portWifi = null,
                                    usbVendorId = null,
                                    usbProductId = null,
                                    dibuatPada = System.currentTimeMillis(),
                                    gagalStatusBerturut = 0,
                                    dinonaktifkanOtomatis = false,
                                )
                            } finally {
                                printer.disconnectPrinter()
                            }
                        }
                    }

                if (result != null) {
                    _testResult.emit(
                        PrinterTestResult.Success(result),
                    )
                } else {
                    _testResult.emit(
                        PrinterTestResult.Failed(
                            "Timeout: tidak dapat terhubung ke printer.",
                        ),
                    )
                }
            } catch (e: EscPosConnectionException) {
                log.e(e) {
                    "Bluetooth printer connection failed: ${e.message}"
                }

                _testResult.emit(
                    PrinterTestResult.Failed(
                        "Koneksi printer gagal: ${e.message}",
                    ),
                )
            } catch (e: EscPosEncodingException) {
                log.e(e) {
                    "Bluetooth printer encoding failed: ${e.message}"
                }

                _testResult.emit(
                    PrinterTestResult.Failed(
                        "Gagal encode data printer: ${e.message}",
                    ),
                )
            } catch (e: EscPosParserException) {
                log.e(e) {
                    "Bluetooth printer parser failed: ${e.message}"
                }

                _testResult.emit(
                    PrinterTestResult.Failed(
                        "Format data printer tidak valid: ${e.message}",
                    ),
                )
            } catch (e: SecurityException) {
                log.e(e) {
                    "Bluetooth permission denied: ${e.message}"
                }

                _testResult.emit(
                    PrinterTestResult.Failed(
                        "Izin Bluetooth tidak tersedia.",
                    ),
                )
            } catch (e: IllegalArgumentException) {
                log.e(e) {
                    "Invalid Bluetooth address: ${e.message}"
                }

                _testResult.emit(
                    PrinterTestResult.Failed(
                        "Alamat Bluetooth tidak valid.",
                    ),
                )
            }
        }
    }

/**
     fun testWifiConnection(name: String, ipAddress: String, port: Int) {
     viewModelScope.launch {
     _testResult.emit(PrinterTestResult.Testing)

     val result = withContext(Dispatchers.IO) {
     withTimeoutOrNull(CONNECTION_TIMEOUT_MS) {
     try {
     val connection = TcpConnection(ipAddress, port, 5000)
     val printer = EscPosPrinter(
     connection,
     PRINTER_DPI,
     PRINTER_WIDTH_MM,
     CHARS_PER_LINE,
     CHARSET_UTF8,
     )

     // Test print
     printer.printFormattedText("[C]TEST CONNECTION\n")
     printer.disconnectPrinter()

     // Create printer entity
     PrinterEntity(
     nama = name,
     tipeKoneksi = "WIFI",
     isDefault = false,
     prioritas = 1,
     karakterPerBaris = CHARS_PER_LINE,
     lebarKertas = "80mm",
     mendukungStatus = true,
     alamatBluetooth = null,
     alamatWifi = ipAddress,
     portWifi = port,
     usbVendorId = null,
     usbProductId = null,
     dibuatPada = System.currentTimeMillis(),
     gagalStatusBerturut = 0,
     dinonaktifkanOtomatis = false,
     )
     } catch (e: Exception) {
     null
     }
     }
     }

     if (result != null) {
     _testResult.emit(PrinterTestResult.Success(result))
     } else {
     _testResult.emit(PrinterTestResult.Failed("Tidak dapat terhubung ke printer. Periksa IP address dan port."))
     }
     }
     } */
    fun testWifiConnection(
        name: String,
        ipAddress: String,
        port: Int,
    ) {
        viewModelScope.launch {
            _testResult.emit(PrinterTestResult.Testing)

            try {
                val result =
                    withContext(Dispatchers.IO) {
                        withTimeoutOrNull(CONNECTION_TIMEOUT_MS) {
                            val connection =
                                TcpConnection(ipAddress, port, 5000)

                            val printer =
                                EscPosPrinter(
                                    connection,
                                    PRINTER_DPI,
                                    PRINTER_WIDTH_MM,
                                    CHARS_PER_LINE,
                                    CHARSET_UTF8,
                                )

                            try {
                                printer.printFormattedText(
                                    "[C]TEST CONNECTION\n",
                                )

                                PrinterEntity(
                                    nama = name,
                                    tipeKoneksi = "WIFI",
                                    isDefault = false,
                                    prioritas = 1,
                                    karakterPerBaris = CHARS_PER_LINE,
                                    lebarKertas = "80mm",
                                    mendukungStatus = true,
                                    alamatBluetooth = null,
                                    alamatWifi = ipAddress,
                                    portWifi = port,
                                    usbVendorId = null,
                                    usbProductId = null,
                                    dibuatPada = System.currentTimeMillis(),
                                    gagalStatusBerturut = 0,
                                    dinonaktifkanOtomatis = false,
                                )
                            } finally {
                                printer.disconnectPrinter()
                            }
                        }
                    }

                if (result != null) {
                    _testResult.emit(
                        PrinterTestResult.Success(result),
                    )
                } else {
                    _testResult.emit(
                        PrinterTestResult.Failed(
                            "Timeout: tidak dapat terhubung ke printer.",
                        ),
                    )
                }
            } catch (e: EscPosConnectionException) {
                log.e(e) {
                    "WiFi printer connection failed: ${e.message}"
                }

                _testResult.emit(
                    PrinterTestResult.Failed(
                        "Koneksi printer gagal: ${e.message}",
                    ),
                )
            } catch (e: EscPosEncodingException) {
                log.e(e) {
                    "WiFi printer encoding failed: ${e.message}"
                }

                _testResult.emit(
                    PrinterTestResult.Failed(
                        "Gagal encode data printer: ${e.message}",
                    ),
                )
            } catch (e: EscPosParserException) {
                log.e(e) {
                    "WiFi printer parser failed: ${e.message}"
                }

                _testResult.emit(
                    PrinterTestResult.Failed(
                        "Format data printer tidak valid: ${e.message}",
                    ),
                )
            } catch (e: IllegalArgumentException) {
                log.e(e) {
                    "Invalid WiFi configuration: ${e.message}"
                }

                _testResult.emit(
                    PrinterTestResult.Failed(
                        "IP address atau port tidak valid.",
                    ),
                )
            }
        }
    }

    fun savePrinter(
        printer: PrinterEntity,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) {
                val existingDefault = printerRepo.getDefault()
                val printerToSave =
                    if (existingDefault == null) {
                        printer.copy(isDefault = true)
                    } else {
                        printer
                    }

                val savedId = printerRepo.insert(printerToSave)
                log.d { "💾 Printer saved: ${printerToSave.nama}, isDefault=${printerToSave.isDefault}, id=$savedId" }

                printerService.reloadPrinter()
                log.d { "🔄 PrinterService reloaded" }
            }
            onComplete()
        }
    }

    override fun onCleared() {
        stopScan()
    }
}
