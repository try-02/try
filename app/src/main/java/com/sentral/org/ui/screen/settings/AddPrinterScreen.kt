package com.sentral.org.ui.screen.settings

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sentral.org.data.entity.PrinterEntity
import com.sentral.org.ui.navigation.PosRoute
import com.sentral.org.ui.viewmodel.AddPrinterViewModel
import com.sentral.org.ui.viewmodel.BluetoothDeviceUi
import com.sentral.org.ui.viewmodel.PrinterTestResult
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddPrinterScreen(
    onBack: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddPrinterViewModel = koinViewModel(),
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Bluetooth, 1 = WiFi
    var showTestDialog by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<PrinterTestResult?>(null) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect states
    val bluetoothDevices by viewModel.bluetoothDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val isBluetoothEnabled by viewModel.isBluetoothEnabled.collectAsState()

    val requiredPermissions =
        remember {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                )
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

    fun hasPermissions(): Boolean =
        requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    // Launcher untuk meminta pengaktifan Bluetooth sistem
    val enableBluetoothLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.setBluetoothEnabled(true)
                viewModel.startBluetoothScan()
            } else {
                viewModel.setBluetoothEnabled(false)
                scope.launch {
                    snackbarHostState.showSnackbar("Bluetooth harus aktif untuk memindai printer.")
                }
            }
        }

    // Launcher untuk izin runtime Bluetooth / Lokasi
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                if (viewModel.checkBluetoothEnabled()) {
                    viewModel.startBluetoothScan()
                } else {
                    enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                }
            } else {
                val activity = context as? Activity
                val anyPermanentlyDenied =
                    permissions.keys.any { perm ->
                        activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
                    }
                if (anyPermanentlyDenied) {
                    showPermissionDeniedDialog = true
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("Izin perangkat di sekitar dibutuhkan untuk mendeteksi printer.")
                    }
                }
            }
        }

    val requestScanOrPrerequisites = {
        if (!hasPermissions()) {
            permissionLauncher.launch(requiredPermissions)
        } else if (!viewModel.checkBluetoothEnabled()) {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } else {
            viewModel.startBluetoothScan()
        }
    }

    // Dengarkan perubahan status adapter Bluetooth hardware di runtime
    DisposableEffect(context) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    c: Context?,
                    intent: Intent?,
                ) {
                    if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        when (state) {
                            BluetoothAdapter.STATE_ON -> {
                                viewModel.setBluetoothEnabled(true)
                                if (hasPermissions() && selectedTab == 0) {
                                    viewModel.startBluetoothScan()
                                }
                            }

                            BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF -> {
                                viewModel.setBluetoothEnabled(false)
                            }
                        }
                    }
                }
            }
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }

    // Cek awal saat layar dibuka
    LaunchedEffect(Unit) {
        if (!hasPermissions()) {
            permissionLauncher.launch(requiredPermissions)
        } else if (viewModel.checkBluetoothEnabled()) {
            viewModel.startBluetoothScan()
        } else {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }

    // Dengarkan pesan kegagalan dari ViewModel
    LaunchedEffect(Unit) {
        viewModel.scanMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tambah Printer",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                windowInsets = WindowInsets.statusBars,
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            // Tab row
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Bluetooth",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    icon = { Icon(Icons.Filled.Bluetooth, contentDescription = null) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "WiFi",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    icon = { Icon(Icons.Filled.Wifi, contentDescription = null) },
                )
            }

            // Content
            when (selectedTab) {
                0 -> {
                    BluetoothTab(
                        devices = bluetoothDevices,
                        isScanning = isScanning,
                        scanProgress = scanProgress,
                        isBluetoothEnabled = isBluetoothEnabled,
                        onEnableBluetooth = {
                            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                        },
                        onRescan = requestScanOrPrerequisites,
                        onSelectDevice = { device ->
                            testResult = null
                            showTestDialog = true
                            viewModel.testBluetoothConnection(device)
                        },
                    )
                }

                1 -> {
                    WifiTab(
                        onTestConnection = { name, ip, port ->
                            testResult = null
                            showTestDialog = true
                            viewModel.testWifiConnection(name, ip, port)
                        },
                    )
                }
            }
        }

        // Test connection dialog
        AnimatedVisibility(
            visible = showTestDialog,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            TestConnectionDialog(
                result = testResult,
                onDismiss = {
                    showTestDialog = false
                    testResult = null
                },
                onSave = { printer ->
                    viewModel.savePrinter(printer) {
                        onSave()
                    }
                },
            )
        }
    }

    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Izin Bluetooth Diperlukan", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Aplikasi membutuhkan izin 'Perangkat di sekitar' untuk mendeteksi printer kasir. Silakan aktifkan izin di Pengaturan Aplikasi.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDeniedDialog = false
                        val intent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        context.startActivity(intent)
                    },
                ) {
                    Text("Buka Pengaturan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text("Batal")
                }
            },
        )
    }

    // Observe test result changes
    LaunchedEffect(Unit) {
        viewModel.testResult.collect { result ->
            testResult = result
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BluetoothTab(
    devices: List<BluetoothDeviceUi>,
    isScanning: Boolean,
    scanProgress: Float,
    isBluetoothEnabled: Boolean,
    onEnableBluetooth: () -> Unit,
    onRescan: () -> Unit,
    onSelectDevice: (BluetoothDeviceUi) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(20.dp),
    ) {
        // Banner peringatan bila Bluetooth dimatikan
        AnimatedVisibility(visible = !isBluetoothEnabled) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp),
                ) {
                    Icon(
                        Icons.Filled.BluetoothDisabled,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Bluetooth Tidak Aktif",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            "Nyalakan Bluetooth untuk mendeteksi printer kasir di sekitar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onEnableBluetooth,
                        shape = MaterialTheme.shapes.small,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                    ) {
                        Text("Nyalakan", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Header with rescan button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "Perangkat Bluetooth",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (!isBluetoothEnabled) {
                        "Bluetooth mati"
                    } else if (isScanning) {
                        "Memindai perangkat..."
                    } else {
                        "${devices.size} perangkat ditemukan"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedButton(
                onClick = onRescan,
                enabled = !isScanning,
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Scan Ulang", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Progress indicator
        AnimatedVisibility(visible = isScanning) {
            Column {
                LinearProgressIndicator(
                    progress = { scanProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Pastikan printer dalam mode pairing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
            }
        }

        // Device list
        if (devices.isEmpty() && !isScanning) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(96.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Tidak ada perangkat ditemukan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap 'Scan Ulang' untuk memindai kembali",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(devices, key = { it.address }) { device ->
                    BluetoothDeviceCard(
                        device = device,
                        onClick = { onSelectDevice(device) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BluetoothDeviceCard(
    device: BluetoothDeviceUi,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp,
            ),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color =
                    if (device.isPaired) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Bluetooth,
                        contentDescription = null,
                        tint =
                            if (device.isPaired) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    device.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (device.isPaired) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            "Paired",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Pilih",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WifiTab(onTestConnection: (name: String, ip: String, port: Int) -> Unit) {
    var printerName by rememberSaveable { mutableStateOf("") }
    var ipAddress by rememberSaveable { mutableStateOf("") }
    var portText by rememberSaveable { mutableStateOf("9100") }

    val isValid =
        printerName.isNotBlank() &&
            ipAddress.isNotBlank() &&
            isValidIpAddress(ipAddress) &&
            portText.toIntOrNull() in 1..65535

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(20.dp),
    ) {
        Text(
            "Konfigurasi Printer WiFi",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Masukkan alamat IP dan port printer thermal Anda",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = printerName,
            onValueChange = { printerName = it },
            label = { Text("Nama Printer") },
            placeholder = { Text("Contoh: Printer Kasir 1") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
        )

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = ipAddress,
            onValueChange = { ipAddress = it },
            label = { Text("Alamat IP") },
            placeholder = { Text("192.168.1.100") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = ipAddress.isNotBlank() && !isValidIpAddress(ipAddress),
            supportingText = {
                if (ipAddress.isNotBlank() && !isValidIpAddress(ipAddress)) {
                    Text("Format IP tidak valid")
                }
            },
            shape = MaterialTheme.shapes.medium,
        )

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = portText,
            onValueChange = { portText = it.filter { c -> c.isDigit() } },
            label = { Text("Port") },
            placeholder = { Text("9100") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = portText.toIntOrNull() !in 1..65535,
            supportingText = {
                if (portText.toIntOrNull() !in 1..65535) {
                    Text("Port harus antara 1-65535")
                }
            },
            shape = MaterialTheme.shapes.medium,
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                val port = portText.toIntOrNull() ?: 9100
                onTestConnection(printerName, ipAddress, port)
            },
            enabled = isValid,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            shape = MaterialTheme.shapes.large,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
        ) {
            Icon(Icons.Filled.Wifi, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                "Test Koneksi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TestConnectionDialog(
    result: PrinterTestResult?,
    onDismiss: () -> Unit,
    onSave: (PrinterEntity) -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        icon = {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color =
                    when (result) {
                        is PrinterTestResult.Success -> MaterialTheme.colorScheme.primaryContainer
                        is PrinterTestResult.Failed -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when (result) {
                        is PrinterTestResult.Testing -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        is PrinterTestResult.Success -> {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp),
                            )
                        }

                        is PrinterTestResult.Failed ->
                            Icon(
                                Icons.Filled.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(32.dp),
                            )

                        null ->
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                    }
                }
            }
        },
        title = {
            Text(
                when (result) {
                    is PrinterTestResult.Testing -> "Menguji Koneksi..."
                    is PrinterTestResult.Success -> "Koneksi Berhasil!"
                    is PrinterTestResult.Failed -> "Koneksi Gagal"
                    null -> "Menguji Koneksi..."
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            when (result) {
                is PrinterTestResult.Testing -> {
                    Text(
                        "Sedang mencoba terhubung ke printer. Harap tunggu...",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                is PrinterTestResult.Success -> {
                    Column {
                        Text(
                            "Printer siap digunakan!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Tap 'Simpan' untuk menambahkan printer ke daftar.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                is PrinterTestResult.Failed ->
                    Column {
                        Text(
                            result.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Pastikan printer menyala dan terhubung ke jaringan yang sama.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                null ->
                    Text(
                        "Mempersiapkan...",
                        style = MaterialTheme.typography.bodyLarge,
                    )
            }
        },
        confirmButton = {
            when (result) {
                is PrinterTestResult.Success -> {
                    Button(
                        onClick = { onSave(result.printer) },
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text("Simpan", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }

                is PrinterTestResult.Failed -> {
                    Button(
                        onClick = onDismiss,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text("Coba Lagi", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }

                else -> {}
            }
        },
        dismissButton = {
            if (result !is PrinterTestResult.Testing) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text("Batal", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
    )
}

private fun isValidIpAddress(ip: String): Boolean {
    val parts = ip.split(".")
    if (parts.size != 4) return false
    return parts.all { part ->
        val num = part.toIntOrNull() ?: return@all false
        num in 0..255
    }
}
