package com.aistudio.lioracamward.ui.screens

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.aistudio.lioracamward.data.model.Finding
import com.aistudio.lioracamward.data.model.RiskLevel
import com.aistudio.lioracamward.data.model.ScanModule
import com.aistudio.lioracamward.data.model.Severity
import com.aistudio.lioracamward.data.repository.ScanRepository
import com.aistudio.lioracamward.detection.BleDetector
import com.aistudio.lioracamward.detection.MagneticDetector
import com.aistudio.lioracamward.detection.NetworkDetector
import com.aistudio.lioracamward.detection.OpticalDetector
import com.aistudio.lioracamward.detection.RiskCalculator
import com.aistudio.lioracamward.ui.components.FindingCard
import com.aistudio.lioracamward.ui.components.RiskBadge
import com.aistudio.lioracamward.ui.components.SignalGauge
import com.aistudio.lioracamward.ui.theme.CyberBackground
import com.aistudio.lioracamward.ui.theme.CyberCard
import com.aistudio.lioracamward.ui.theme.CyberCardBorder
import com.aistudio.lioracamward.ui.theme.DangerRed
import com.aistudio.lioracamward.ui.theme.RadarCyan
import com.aistudio.lioracamward.ui.theme.RadarNeonGreen
import com.aistudio.lioracamward.ui.theme.TextMuted
import com.aistudio.lioracamward.ui.theme.TextPrimary
import com.aistudio.lioracamward.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

@Composable
fun ScanScreen(
    scanRepository: ScanRepository,
    onFinishScan: (scanId: String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val scanId = remember { UUID.randomUUID().toString() }
    val startTime = remember { System.currentTimeMillis() }

    // Detectors
    val magneticDetector = remember { MagneticDetector(context) }
    val bleDetector = remember { BleDetector(context) }
    val opticalDetector = remember { OpticalDetector() }
    val networkDetector = remember { NetworkDetector(context) }

    val magneticReading by magneticDetector.reading.collectAsState()
    val bleDevices by bleDetector.devices.collectAsState()
    val opticalClusters by opticalDetector.clusters.collectAsState()
    val torchActive by opticalDetector.torchActive.collectAsState()

    val findings = remember { mutableStateListOf<Finding>() }
    var currentRiskLevel by remember { mutableStateOf(RiskLevel.CLEAR) }
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Camera control references
    var cameraControlRef by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }

    // Trigger haptic feedback
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
    fun triggerVibrate(severity: Severity) {
        if (severity == Severity.INFO) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timing = if (severity == Severity.HIGH) {
                longArrayOf(0, 200, 100, 250)
            } else {
                longArrayOf(0, 150)
            }
            vibrator?.vibrate(VibrationEffect.createWaveform(timing, -1))
        } else {
            vibrator?.vibrate(200)
        }
    }

    // Permission launcher for Camera & BLE
    val permissionsToRequest = remember {
        val list = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_SCAN)
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        list.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        cameraPermissionGranted = perms[Manifest.permission.CAMERA] == true
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionsToRequest)
    }

    // Start sensors and modules
    LaunchedEffect(Unit) {
        fun handleNewFinding(finding: Finding) {
            findings.add(0, finding)
            currentRiskLevel = RiskCalculator.computeRiskLevel(findings.toList())
            triggerVibrate(finding.severity)
        }

        // 1. Start Magnetometer
        magneticDetector.start(scope) { handleNewFinding(it) }

        // 2. Start BLE Scanner
        bleDetector.startScan { handleNewFinding(it) }

        // 3. Start Optical Glint Detector
        opticalDetector.start { handleNewFinding(it) }

        // 4. Run Network Audit
        networkDetector.scanNetwork { handleNewFinding(it) }
    }

    // Teardown
    DisposableEffect(Unit) {
        onDispose {
            magneticDetector.stop()
            bleDetector.stopScan()
            opticalDetector.stop()
            cameraControlRef?.enableTorch(false)
        }
    }

    fun completeScan() {
        if (isSaving) return
        isSaving = true
        scope.launch {
            val finishTime = System.currentTimeMillis()
            val label = "Inspection " + SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(startTime))
            val modulesRun = listOf(ScanModule.OPTICAL, ScanModule.MAGNETIC, ScanModule.BLUETOOTH, ScanModule.NETWORK)

            scanRepository.saveScanSession(
                id = scanId,
                label = label,
                startedAt = startTime,
                finishedAt = finishTime,
                riskLevel = currentRiskLevel,
                modulesRun = modulesRun,
                findings = findings.toList(),
                notes = RiskCalculator.getRecommendedAction(currentRiskLevel)
            )

            onFinishScan(scanId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
    ) {
        // Top Viewfinder Area (CameraX preview + HUD Overlay)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(Color(0xFF04070A))
        ) {
            if (cameraPermissionGranted) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        val cameraExecutor = Executors.newSingleThreadExecutor()

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                val plane = imageProxy.planes[0]
                                opticalDetector.processLumaPlane(
                                    plane.buffer,
                                    imageProxy.width,
                                    imageProxy.height
                                )
                                imageProxy.close()
                            }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                val cam = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                                cameraControlRef = cam.cameraControl
                            } catch (e: Exception) {
                                // Camera bind error fallback
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Camera permission requested for Optical Lens scan...",
                        fontSize = 12.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Camera Viewfinder HUD Reticle & Glint Highlights
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Torch Toggle Button (Top Right)
                IconButton(
                    onClick = {
                        val nextState = !torchActive
                        opticalDetector.setTorch(nextState)
                        cameraControlRef?.enableTorch(nextState)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(Color(0xAA000000))
                        .testTag("torch_toggle_button")
                ) {
                    Icon(
                        imageVector = if (torchActive) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Torch Toggle",
                        tint = if (torchActive) RadarNeonGreen else Color.White
                    )
                }

                // Optical Status Label (Top Left)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC080D14))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (torchActive) RadarNeonGreen else RadarCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (torchActive) "LENS REFLECTION ACTIVE" else "TAP FLASH TO ACTIVATE GLINT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (torchActive) RadarNeonGreen else TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Center Viewfinder Target Crosshair
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.Center)
                        .border(1.dp, RadarNeonGreen.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                )

                // Render detected glint clusters as glowing markers
                for (cluster in opticalClusters) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(
                                start = (cluster.relativeX * 280).dp,
                                top = (cluster.relativeY * 200).dp
                            )
                            .size(24.dp)
                            .border(2.dp, DangerRed, CircleShape)
                    )
                }

                // Live Camera Hint (Bottom Center)
                Text(
                    text = "Slowly pan across walls, power sockets, and ceiling fixtures",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Live Risk Level & Sensor Metrics Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberCard)
                .border(1.dp, CyberCardBorder)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SURVEILLANCE RISK VERDICT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentRiskLevel.description,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
                RiskBadge(riskLevel = currentRiskLevel)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Magnetometer Live Gauge Component
            SignalGauge(reading = magneticReading)
        }

        // Live Findings Stream & Observed BLE Devices
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE FINDINGS (${findings.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "BLE Devices: ${bleDevices.size}",
                        fontSize = 11.sp,
                        color = RadarCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (findings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Calibrating sensors and monitoring signals...\nWalk through the room with the flashlight on.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            items(findings, key = { it.id }) { finding ->
                FindingCard(finding = finding)
            }
        }

        // Bottom Action Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberCard)
                .border(1.dp, CyberCardBorder)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("abort_scan_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF221115),
                    contentColor = DangerRed
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { completeScan() },
                modifier = Modifier
                    .weight(2f)
                    .height(48.dp)
                    .testTag("complete_scan_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RadarNeonGreen,
                    contentColor = Color(0xFF041A10)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (isSaving) "Saving..." else "COMPLETE & SAVE REPORT",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
