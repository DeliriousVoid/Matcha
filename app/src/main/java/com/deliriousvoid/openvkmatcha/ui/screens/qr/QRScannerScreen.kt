package com.deliriousvoid.openvkmatcha.ui.screens.qr

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    onScanResult: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сканирование QR") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                AndroidView(
                    factory = { context ->
                        val previewView = PreviewView(context)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            val scanner = BarcodeScanning.getClient()
                            val analysisExecutor = Executors.newSingleThreadExecutor()

                            imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                processImageProxy(scanner, imageProxy, onScanResult)
                            }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                Log.e("QRScanner", "Use case binding failed", e)
                            }
                        }, ContextCompat.getMainExecutor(context))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                ScannerOverlay(modifier = Modifier.fillMaxSize())
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Разрешение на камеру не получено")
            }
        }
    }
}

@Composable
private fun ScannerOverlay(modifier: Modifier = Modifier) {
    val overlayColor = Color.Black.copy(alpha = 0.6f)
    val strokeColor = MaterialTheme.colorScheme.primary
    val strokeWidth = 4.dp
    val cornerLength = 40.dp

    Canvas(modifier = modifier.graphicsLayer(alpha = 0.99f)) {
        val width = size.width
        val height = size.height
        val scannerSize = width * 0.7f
        val left = (width - scannerSize) / 2
        val top = (height - scannerSize) / 2
        val right = left + scannerSize
        val bottom = top + scannerSize

        // Draw darkened background
        drawRect(color = overlayColor)

        // Clear the center square
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(scannerSize, scannerSize),
            cornerRadius = CornerRadius(12.dp.toPx()),
            blendMode = BlendMode.Clear
        )

        val strokePx = strokeWidth.toPx()
        val cornerPx = cornerLength.toPx()

        // Draw corners
        // Top Left
        drawPath(
            path = Path().apply {
                moveTo(left, top + cornerPx)
                lineTo(left, top)
                lineTo(left + cornerPx, top)
            },
            color = strokeColor,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )

        // Top Right
        drawPath(
            path = Path().apply {
                moveTo(right - cornerPx, top)
                lineTo(right, top)
                lineTo(right, top + cornerPx)
            },
            color = strokeColor,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )

        // Bottom Left
        drawPath(
            path = Path().apply {
                moveTo(left, bottom - cornerPx)
                lineTo(left, bottom)
                lineTo(left + cornerPx, bottom)
            },
            color = strokeColor,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )

        // Bottom Right
        drawPath(
            path = Path().apply {
                moveTo(right - cornerPx, bottom)
                lineTo(right, bottom)
                lineTo(right, bottom - cornerPx)
            },
            color = strokeColor,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImageProxy(
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onScanResult: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let {
                        onScanResult(it)
                    }
                }
            }
            .addOnFailureListener {
                Log.e("QRScanner", "Barcode scanning failed", it)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
