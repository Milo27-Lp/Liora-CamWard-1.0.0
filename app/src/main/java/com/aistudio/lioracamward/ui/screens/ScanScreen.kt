package com.aistudio.lioracamward.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
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
import com.aistudio.lioracamward.audio.ScanAudioPlayer
import com.aistudio.lioracamward.detection.BleDetector
import com.aistudio.lioracamward.detection.MagneticDetector
import com.aistudio.lioracamward.detection.NetworkDetector
import com.aistudio.lioracamward.detection.OpticalDetector
import com.aistudio.lioracamward.detection.RiskCalculator
import com.aistudio.lioracamward.ui.components.FindingCard
import com.aistudio.lioracamward.ui.components.RadarBlip
import com.aistudio.lioracamward.ui.components.RadarSweepView
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
import com.aistudio.lioracamward.ui.theme.WarningAmber
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

enum class ScanViewportMode(val label: String) {
    CAMERA("Camera Glint"),
    RADAR("Live Radar"),
    DIAGNOSTICS("Sensors")
}

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

    // Active Viewport Mode: Camera Glint HUD vs Tactical Radar vs Diagnostics
    var viewportMode by remember { mutableStateOf(ScanViewportMode.CAMERA) }

    // Detectors
    val magneticDetector = remember { MagneticDetector(context) }
    val bleDetector = remember { BleDetector(context) }
    val opticalDetector = remember { OpticalDetector() }
    val networkDetector = remember { NetworkDetector(context) }
    val audioPlayer = remember { ScanAudioPlayer() }

    val isAudioMuted by audioPlayer.isMuted.collectAsState()
    val magneticReading by magneticDetector.reading.collectAsState()
    val bleDevices by bleDetector.devices.collectAsState()
    val networkDevices by networkDetector.devices.collectAsState()
    val opticalClusters by opticalDetector.clusters.collectAsState()
    val torchActive by opticalDetector.torchActive.collectAsState()

    val findings = remember { mutableStateListOf<Finding>() }
    var currentRiskLevel by remember { mutableStateOf(RiskLevel.CLEAR) }
    var isSaving by remember { mutableStateOf(false) }

    // Check camera permission state immediately
    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Camera control references
    var cameraControlRef by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }

    // Haptic feedback
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
            @Suppress("DEPRECATION")
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
        if (!cameraPermissionGranted) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    // Start all 4 surveillance detection modules
    LaunchedEffect(Unit) {
        fun handleNewFinding(finding: Finding) {
            findings.add(0, finding)
            currentRiskLevel = RiskCalculator.computeRiskLevel(findings.toList())
            triggerVibrate(finding.severity)
            audioPlayer.playFindingSound(finding.severity)
        }

        // 1. Start Magnetometer
        magneticDetector.start(scope) { handleNewFinding(it) }

        // 2. Start BLE Scanner
        bleDetector.startScan { handleNewFinding(it) }

        // 3. Start Optical Glint Detector
        opticalDetector.start { handleNewFinding(it) }

        // 4. Start Real Subnet & Surveillance Port Reconnaissance
        networkDetector.startNetworkScan(scope) { handleNewFinding(it) }
    }

    // Teardown
    DisposableEffect(Unit) {
        onDispose {
            magneticDetector.stop()
            bleDetector.stopScan()
            opticalDetector.stop()
            networkDetector.stop()
            cameraControlRef?.enableTorch(false)
            audioPlayer.release()
        }
    }

    // Compute live radar blips from real detected devices
    val liveRadarBlips = remember(bleDevices, networkDevices) {
        val blips = mutableListOf<RadarBlip>()

        // 1. Real BLE devices plotted by RSSI and MAC address hash
        for (ble in bleDevices) {
            val dist = ((-ble.rssi - 30f) / 65f).coerceIn(0.15f, 0.9f)
            val angle = (kotlin.math.abs(ble.address.hashCode()) % 360).toFloat()
            blips.add(
                RadarBlip(
                    id = "ble_${ble.address}",
                    angleDegrees = angle,
                    distanceRatio = dist,
                    isAlert = ble.isSuspicious,
                    label = ble.name
                )
            )
        }

        // 2. Real Network surveillance devices plotted on outer ring
        for (net in networkDevices) {
            val angle = (kotlin.math.abs(net.ip.hashCode()) % 360).toFloat()
            blips.add(
                RadarBlip(
                    id = "net_${net.ip}",
                    angleDegrees = angle,
                    distanceRatio = 0.78f,
                    isAlert = net.isSuspicious,
                    label = net.ip
                )
            )
        }

        blips.toList()
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
        // Viewport Selector Header (Camera Glint / Live Radar / Sensors) + Audio Feedback Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberCard)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ScanViewportMode.values().forEach { mode ->
                    val isSelected = viewportMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) RadarNeonGreen.copy(alpha = 0.15f) else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) RadarNeonGreen else CyberCardBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { viewportMode = mode }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = when (mode) {
                                    ScanViewportMode.CAMERA -> Icons.Default.CameraAlt
                                    ScanViewportMode.RADAR -> Icons.Default.Radar
                                    ScanViewportMode.DIAGNOSTICS -> Icons.Default.Hub
                                },
                                contentDescription = mode.label,
                                tint = if (isSelected) RadarNeonGreen else TextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = mode.label,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) RadarNeonGreen else TextMuted,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Audio Feedback Mute / Unmute Toggle Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (!isAudioMuted) RadarNeonGreen.copy(alpha = 0.15f) else Color(0xFF141920))
                    .border(
                        width = 1.dp,
                        color = if (!isAudioMuted) RadarNeonGreen else CyberCardBorder,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { audioPlayer.toggleMute() }
                    .padding(horizontal = 9.dp, vertical = 7.dp)
                    .testTag("audio_mute_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isAudioMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = if (isAudioMuted) "Unmute Audio Feedback" else "Mute Audio Feedback",
                        tint = if (!isAudioMuted) RadarNeonGreen else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isAudioMuted) "MUTED" else "SOUND",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isAudioMuted) RadarNeonGreen else TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Top Viewport Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(Color(0xFF04070A))
        ) {
            when (viewportMode) {
                ScanViewportMode.CAMERA -> {
                    // Optical Camera Viewfinder
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
                                            buffer = plane.buffer,
                                            width = imageProxy.width,
                                            height = imageProxy.height,
                                            rowStride = plane.rowStride,
                                            pixelStride = plane.pixelStride
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
                                        // Camera fallback
                                    }
                                }, ContextCompat.getMainExecutor(ctx))

                                previewView
                            }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Camera Permission Required",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Camera and torch are required for coaxial optical lens glint detection.",
                                fontSize = 12.sp,
                                color = TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { permissionLauncher.launch(permissionsToRequest) },
                                colors = ButtonDefaults.buttonColors(containerColor = RadarNeonGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Enable Camera",
                                    color = Color(0xFF041A10),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Viewfinder HUD Reticle & Glint Highlights
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
                                .border(1.dp, if (torchActive) RadarNeonGreen else Color.Transparent, CircleShape)
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
                                .border(1.dp, CyberCardBorder, RoundedCornerShape(6.dp))
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
                                    text = if (torchActive) "COAXIAL FLASH ACTIVE" else "TAP FLASH FOR OPTICAL GLINT",
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
                                .size(90.dp)
                                .align(Alignment.Center)
                                .border(1.dp, RadarNeonGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        )

                        // Render detected glint clusters as glowing markers
                        for (cluster in opticalClusters) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(
                                        start = (cluster.relativeX * 280).dp,
                                        top = (cluster.relativeY * 190).dp
                                    )
                                    .size(24.dp)
                                    .border(2.dp, DangerRed, CircleShape)
                            )
                        }

                        // Live Hint
                        Text(
                            text = "Pan slowly across outlets, lamps, clocks, and ceiling fixtures",
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xDD000000))
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                ScanViewportMode.RADAR -> {
                    // Tactical Radar View with REAL Blips
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        RadarSweepView(
                            modifier = Modifier.size(230.dp),
                            isScanning = true,
                            blips = liveRadarBlips
                        )

                        // Radar Stats HUD (Bottom Left / Right)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TARGETS: ${liveRadarBlips.size}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = RadarCyan,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "SWEEP: ACTIVE 360°",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = RadarNeonGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                ScanViewportMode.DIAGNOSTICS -> {
                    // Live Sensor Matrix Diagnostics
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DiagnosticCard(
                                modifier = Modifier.weight(1f),
                                title = "MAGNETOMETER",
                                value = String.format("%.1f µT", magneticReading.magnitude),
                                status = if (magneticReading.delta != null) "+${String.format("%.1f", magneticReading.delta)} µT" else "Calibrating",
                                isAlert = (magneticReading.delta ?: 0f) > 25f
                            )
                            DiagnosticCard(
                                modifier = Modifier.weight(1f),
                                title = "BLE RADIO",
                                value = "${bleDevices.size} Observed",
                                status = if (bleDevices.any { it.isSuspicious }) "THREAT MATCH" else "Scanning",
                                isAlert = bleDevices.any { it.isSuspicious }
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DiagnosticCard(
                                modifier = Modifier.weight(1f),
                                title = "SUBNET RECON",
                                value = "${networkDevices.size} Active Hosts",
                                status = if (networkDevices.any { it.isSuspicious }) "CAM PORT 554/8000" else "Clear",
                                isAlert = networkDevices.any { it.isSuspicious }
                            )
                            DiagnosticCard(
                                modifier = Modifier.weight(1f),
                                title = "OPTICAL GLINTS",
                                value = "${opticalClusters.size} Reflections",
                                status = if (opticalClusters.isNotEmpty()) "SUSPICIOUS PINPOINT" else "Clear",
                                isAlert = opticalClusters.isNotEmpty()
                            )
                        }
                    }
                }
            }
        }

        // Live Risk Level & Sensor Metrics Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberCard)
                .border(1.dp, CyberCardBorder)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "REAL-TIME SURVEILLANCE RISK",
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

            Spacer(modifier = Modifier.height(8.dp))

            // Magnetometer Live Gauge
            SignalGauge(reading = magneticReading)
        }

        // Live Findings Stream
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
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
                        text = "REAL FINDINGS (${findings.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (!isAudioMuted) RadarNeonGreen.copy(alpha = 0.12f) else Color(0xFF141920))
                                .border(1.dp, if (!isAudioMuted) RadarNeonGreen.copy(alpha = 0.4f) else CyberCardBorder, RoundedCornerShape(4.dp))
                                .clickable { audioPlayer.toggleMute() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("audio_badge_toggle")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isAudioMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = if (!isAudioMuted) RadarNeonGreen else TextMuted,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isAudioMuted) "MUTED" else "AUDIO ACTIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isAudioMuted) RadarNeonGreen else TextMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RF: ${bleDevices.size} | Net: ${networkDevices.size}",
                            fontSize = 11.sp,
                            color = RadarCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
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
                            text = "Scanning real physical sensors & radio spectrum...\nMove device close to suspected surfaces.",
                            color = TextMuted,
                            fontSize = 12.sp,
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
                .padding(14.dp),
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
                    text = if (isSaving) "SAVING AUDIT..." else "COMPLETE & SAVE REPORT",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun DiagnosticCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    status: String,
    isAlert: Boolean
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CyberCard)
            .border(1.dp, if (isAlert) DangerRed else CyberCardBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isAlert) DangerRed else TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = status,
                fontSize = 10.sp,
                color = if (isAlert) DangerRed else RadarNeonGreen,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
