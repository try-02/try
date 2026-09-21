package com.sentral.org.ui.screen.pos.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import androidx.compose.ui.geometry.Size as GeometrySize
import java.util.concurrent.ExecutionException
import co.touchlab.kermit.Logger

enum class ScanVisualState { IDLE, SUCCESS, ERROR }

@OptIn(ExperimentalGetImage::class)
@Composable
fun BarcodeScannerOverlay(
    onDismiss: () -> Unit,
    onBarcodeScanned: suspend (String) -> String?, // return nama produk jika sukses, null jika gagal
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    private companion object {
        private val log = Logger.withTag("ScanBO")
    }

    // Audio Beeper Kasir bawaan Android
    val toneGenerator = remember {
        try { ToneGenerator(AudioManager.STREAM_MUSIC, 100) } catch (_: Exception) { null }
    }
    DisposableEffect(Unit) {
        onDispose { toneGenerator?.release() }
    }

    // Camera & MLKit State
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_QR_CODE,
                ).build(),
        )
    }

    var isMultiScanMode by remember { mutableStateOf(false) }
    var scannedCountBatch by remember { mutableIntStateOf(0) }
    var lastScannedCodeText by remember { mutableStateOf("") }
    var scanErrorMessage by remember { mutableStateOf<String?>(null) }
    var scanVisualState by remember { mutableStateOf(ScanVisualState.IDLE) }
    var isProcessing by remember { mutableStateOf(false) }

    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    var lastScannedTime by remember { mutableLongStateOf(0L) }
    var pendingCode by remember { mutableStateOf<String?>(null) }
    var pendingCodeCount by remember { mutableIntStateOf(0) }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    // Permission check
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) onDismiss()
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    BackHandler { onDismiss() }

    LaunchedEffect(scanErrorMessage) {
        if (scanErrorMessage != null) {
            delay(2500L)
            scanErrorMessage = null
        }
    }

    fun attemptBind() {
        val provider = cameraProvider ?: return
        val previewView = previewViewRef ?: return
        if (executor.isShutdown) return

        try {
            val resolutionStrategy = ResolutionStrategy(
                Size(720, 1280),
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
            )
            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(resolutionStrategy)
                .build()

            val preview = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
/**
            analysis.setAnalyzer(executor) { proxy ->
                val mediaImage = proxy.image
                if (mediaImage == null || isProcessing) {
                    proxy.close()
                    return@setAnalyzer
                }

                try {
                    val input = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                    scanner.process(input)
                        .addOnSuccessListener { barcodes ->
                            val detectedBarcode = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                            if (detectedBarcode != null) {
                                val code = detectedBarcode.rawValue ?: return@addOnSuccessListener
                                val currentTime = System.currentTimeMillis()

                                if (code == pendingCode) {
                                    pendingCodeCount++
                                    // Butuh 2 frame identik konsekutif untuk validasi (anti-misread)
                                    if (pendingCodeCount >= 2) {
                                        val isSameCode = (code == lastScannedCode)
                                        val isTimeElapsed = (currentTime - lastScannedTime) > 1500L

                                        if ((!isSameCode || isTimeElapsed) && !isProcessing) {
                                            isProcessing = true
                                            lastScannedCode = code
                                            lastScannedTime = currentTime

                                            coroutineScope.launch {
                                                val productName = onBarcodeScanned(code)
                                                if (productName != null) {
                                                    // SUKSES: Audio beep + Haptic + Visual hijau
                                                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    scanVisualState = ScanVisualState.SUCCESS
                                                    scannedCountBatch++
                                                    lastScannedCodeText = productName
                                                    scanErrorMessage = null

                                                    if (!isMultiScanMode) {
                                                        delay(300L)
                                                        onDismiss()
                                                    }
                                                } else {
                                                    // GAGAL: Error tone + Visual merah
                                                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 250)
                                                    scanVisualState = ScanVisualState.ERROR
                                                    scanErrorMessage = "Produk tidak ditemukan ($code)"
                                                }
                                                delay(400L)
                                                scanVisualState = ScanVisualState.IDLE
                                                isProcessing = false
                                            }
                                        }
                                    }
                                } else {
                                    pendingCode = code
                                    pendingCodeCount = 1
                                }
                            } else {
                                pendingCode = null
                                pendingCodeCount = 0
                            }
                        }
                        .addOnCompleteListener { proxy.close() }
                } catch (e: Exception) {
                    proxy.close()
                }
            }

            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis,
            )
            cameraError = null
        } catch (e: Exception) {
            cameraError = "Gagal mengakses sensor kamera."
        }
    }
*/
analysis.setAnalyzer(executor) { proxy ->
    val mediaImage = proxy.image

    if (mediaImage == null || isProcessing) {
        proxy.close()
        return@setAnalyzer
    }

    try {
        val input = InputImage.fromMediaImage(
            mediaImage,
            proxy.imageInfo.rotationDegrees,
        )

        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                val detectedBarcode =
                    barcodes.firstOrNull {
                        !it.rawValue.isNullOrBlank()
                    }

                if (detectedBarcode != null) {
                    val code = detectedBarcode.rawValue
                        ?: return@addOnSuccessListener

                    val currentTime = System.currentTimeMillis()

                    if (code == pendingCode) {
                        pendingCodeCount++

                        if (pendingCodeCount >= 2) {
                            val isSameCode =
                                code == lastScannedCode

                            val isTimeElapsed =
                                (currentTime - lastScannedTime) > 1500L

                            if (
                                (!isSameCode || isTimeElapsed) &&
                                !isProcessing
                            ) {
                                isProcessing = true
                                lastScannedCode = code
                                lastScannedTime = currentTime

                                coroutineScope.launch {
                                    val productName =
                                        onBarcodeScanned(code)

                                    if (productName != null) {
                                        toneGenerator?.startTone(
                                            ToneGenerator.TONE_PROP_BEEP,
                                            150,
                                        )

                                        haptic.performHapticFeedback(
                                            HapticFeedbackType.LongPress
                                        )

                                        scanVisualState =
                                            ScanVisualState.SUCCESS

                                        scannedCountBatch++
                                        lastScannedCodeText = productName
                                        scanErrorMessage = null

                                        if (!isMultiScanMode) {
                                            delay(300L)
                                            onDismiss()
                                        }
                                    } else {
                                        toneGenerator?.startTone(
                                            ToneGenerator.TONE_PROP_NACK,
                                            250,
                                        )

                                        scanVisualState =
                                            ScanVisualState.ERROR

                                        scanErrorMessage =
                                            "Produk tidak ditemukan ($code)"
                                    }

                                    delay(400L)
                                    scanVisualState =
                                        ScanVisualState.IDLE

                                    isProcessing = false
                                }
                            }
                        }
                    } else {
                        pendingCode = code
                        pendingCodeCount = 1
                    }
                } else {
                    pendingCode = null
                    pendingCodeCount = 0
                }
            }
            .addOnFailureListener { e ->
                log.e(e) {
                    "Barcode scanning gagal: ${e.message}"
                }
            }
            .addOnCompleteListener {
                proxy.close()
            }

    } catch (e: IllegalArgumentException) {
        log.e(e) {
            "Input kamera tidak valid: ${e.message}"
        }

        proxy.close()
    }
}
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis,
            )
            cameraError = null
} catch (e: IllegalArgumentException) {
    log.e(e) {
        "Konfigurasi kamera tidak valid: ${e.message}"
    }
    cameraError = "Konfigurasi kamera tidak valid."
} catch (e: IllegalStateException) {
    log.e(e) {
        "Kamera sedang dalam kondisi tidak valid: ${e.message}"
    }
    cameraError = "Kamera sedang tidak dapat digunakan."
}
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            executor.shutdown()
            scanner.close()
        }
    }

    if (!hasCameraPermission) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black))
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { c: Context ->
                val previewView = PreviewView(c).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                previewViewRef = previewView
                val providerFuture = ProcessCameraProvider.getInstance(c)
/**                providerFuture.addListener({
                    try {
                        cameraProvider = providerFuture.get()
                        attemptBind()
                    } catch (e: Exception) {
                        cameraError = "Gagal memuat sistem kamera."
                    }
                }, ContextCompat.getMainExecutor(c))
                previewView
            },
        ) */
providerFuture.addListener(
    {
        try {
            cameraProvider = providerFuture.get()
            attemptBind()

        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()

            log.e(e) {
                "Thread kamera terinterupsi: ${e.message}"
            }

            cameraError = "Proses kamera terhenti."

        } catch (e: ExecutionException) {
            log.e(e) {
                "Gagal memuat sistem kamera: ${e.cause?.message ?: e.message}"
            }

            cameraError = "Gagal memuat sistem kamera."

        }
    },
    ContextCompat.getMainExecutor(c),
)
)

        // Viewfinder Cutout
        ScannerViewfinder(
            scanState = scanVisualState,
            modifier = Modifier.fillMaxSize(),
        )

        // Tombol Tutup
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp),
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Tutup", tint = Color.White)
        }

        // Banner Sukses
        AnimatedVisibility(
            visible = scannedCountBatch > 0 && scanErrorMessage == null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 16.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 6.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "$scannedCountBatch Item Masuk Keranjang",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // Banner Error (Barcode tidak ditemukan)
        AnimatedVisibility(
            visible = scanErrorMessage != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 16.dp, end = 60.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                tonalElevation = 6.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = scanErrorMessage.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // Kontrol Multi-Scan & Terakhir Dipindai
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.85f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Repeat, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Mode Multi-Scan", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                if (isMultiScanMode) "Kamera tetap buka untuk banyak barang" else "Kamera tutup setelah 1x scan",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Switch(
                        checked = isMultiScanMode,
                        onCheckedChange = { isMultiScanMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }

                if (lastScannedCodeText.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Terakhir: $lastScannedCodeText",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(if (scannedCountBatch > 0) "Selesai ($scannedCountBatch Item)" else "Selesai", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ScannerViewfinder(
    scanState: ScanVisualState,
    modifier: Modifier = Modifier,
) {
    val animatedColor by animateColorAsState(
        targetValue = when (scanState) {
            ScanVisualState.IDLE -> Color.White
            ScanVisualState.SUCCESS -> Color(0xFF84CC16) // Lime Success
            ScanVisualState.ERROR -> MaterialTheme.colorScheme.error
        },
        animationSpec = tween(200),
        label = "colorAnim",
    )
    val animatedStrokeDp by animateDpAsState(
        targetValue = if (scanState == ScanVisualState.IDLE) 4.dp else 8.dp,
        animationSpec = tween(200),
        label = "strokeAnim",
    )

    Box(
        modifier = modifier.drawWithCache {
            val boxWidth = size.width * 0.75f
            val boxHeight = size.height * 0.35f
            val left = (size.width - boxWidth) / 2f
            val top = (size.height - boxHeight) / 2f
            val right = left + boxWidth
            val bottom = top + boxHeight
            val cornerRadiusPx = 16.dp.toPx()
            val cornerLength = 40.dp.toPx()

            val outerRect = Rect(0f, 0f, size.width, size.height)
            val boxRect = RoundRect(Rect(left, top, right, bottom), CornerRadius(cornerRadiusPx))
            val dimmedPath = Path.combine(
                operation = PathOperation.Difference,
                path1 = Path().apply { addRect(outerRect) },
                path2 = Path().apply { addRoundRect(boxRect) },
            )

            onDrawWithContent {
                drawContent()
                drawPath(dimmedPath, Color.Black.copy(alpha = 0.55f))

                val strokeWidthPx = animatedStrokeDp.toPx()
                val color = animatedColor
                val strokeStyle = Stroke(width = strokeWidthPx)

                // 4 Sudut Viewfinder
                drawLine(color, Offset(left, top + cornerRadiusPx), Offset(left, top + cornerLength), strokeWidthPx)
                drawLine(color, Offset(left + cornerRadiusPx, top), Offset(left + cornerLength, top), strokeWidthPx)
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(left, top),
                    size = GeometrySize(cornerRadiusPx * 2, cornerRadiusPx * 2),
                    style = strokeStyle,
                )

                drawLine(color, Offset(right, top + cornerRadiusPx), Offset(right, top + cornerLength), strokeWidthPx)
                drawLine(color, Offset(right - cornerRadiusPx, top), Offset(right - cornerLength, top), strokeWidthPx)
                drawArc(
                    color = color,
                    startAngle = 270f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(right - cornerRadiusPx * 2, top),
                    size = GeometrySize(cornerRadiusPx * 2, cornerRadiusPx * 2),
                    style = strokeStyle,
                )

                drawLine(color, Offset(left, bottom - cornerRadiusPx), Offset(left, bottom - cornerLength), strokeWidthPx)
                drawLine(color, Offset(left + cornerRadiusPx, bottom), Offset(left + cornerLength, bottom), strokeWidthPx)
                drawArc(
                    color = color,
                    startAngle = 90f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(left, bottom - cornerRadiusPx * 2),
                    size = GeometrySize(cornerRadiusPx * 2, cornerRadiusPx * 2),
                    style = strokeStyle,
                )

                drawLine(color, Offset(right, bottom - cornerRadiusPx), Offset(right, bottom - cornerLength), strokeWidthPx)
                drawLine(color, Offset(right - cornerRadiusPx, bottom), Offset(right - cornerLength, bottom), strokeWidthPx)
                drawArc(
                    color = color,
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(right - cornerRadiusPx * 2, bottom - cornerRadiusPx * 2),
                    size = GeometrySize(cornerRadiusPx * 2, cornerRadiusPx * 2),
                    style = strokeStyle,
                )
            }
        },
    )
}