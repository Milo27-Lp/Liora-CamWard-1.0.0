package com.aistudio.lioracamward.ui.screens

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.aistudio.lioracamward.data.model.RiskLevel
import com.aistudio.lioracamward.data.repository.ScanRepository
import com.aistudio.lioracamward.ui.components.RadarBlip
import com.aistudio.lioracamward.ui.components.RadarSweepView
import com.aistudio.lioracamward.ui.components.RiskBadge
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
import java.net.Inet4Address
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

@Composable
fun HomeScreen(
    scanRepository: ScanRepository,
    onStartScan: () -> Unit,
    onViewHistory: () -> Unit,
    onViewGuide: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Recent scans from database
    val scans by scanRepository.allScans.collectAsState(initial = emptyList())
    val latestScan = scans.firstOrNull()

    // Live ambient sensor telemetry for home screen status
    var liveMagneticFlux by remember { mutableFloatStateOf(45.0f) }
    var hasMagneticSensor by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val magSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        hasMagneticSensor = magSensor != null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    liveMagneticFlux = sqrt(x * x + y * y + z * z)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (magSensor != null) {
            sensorManager?.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            if (magSensor != null) {
                sensorManager?.unregisterListener(listener)
            }
        }
    }

    // Network status
    val networkInfo = remember {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val network = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(network)
        if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            val ssid = wm?.connectionInfo?.ssid?.replace("\"", "")?.takeIf { it != "<unknown ssid>" } ?: "Wi-Fi"
            "WiFi: $ssid"
        } else if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            "Ethernet LAN"
        } else if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            "Cellular"
        } else {
            "Offline"
        }
    }

    // Bluetooth status
    val bleReady = remember {
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bm?.adapter?.isEnabled == true
    }

    // Camera permission
    val cameraGranted = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    // Active blips
    val ambientBlips = remember(scans) {
        listOf(
            RadarBlip("rf_ambient_1", 38f, 0.42f, false, "BLE Beacon"),
            RadarBlip("rf_ambient_2", 145f, 0.72f, false, "Local Host"),
            RadarBlip("rf_ambient_3", 265f, 0.58f, latestScan?.highRiskCount?.let { it > 0 } ?: false, "RF Target")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LIORA CAMWARD",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = RadarNeonGreen,
                letterSpacing = 4.sp,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "REAL-TIME PHYSICAL SURVEILLANCE DETECTOR",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.2.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Subsystem Readiness Grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CyberCard)
                .border(1.dp, CyberCardBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SensorPill(label = "OPTICAL", status = if (cameraGranted) "CAMERA READY" else "REQ PERMISSION", isReady = cameraGranted)
            SensorPill(label = "MAGNETIC", status = if (hasMagneticSensor) "${String.format("%.0f", liveMagneticFlux)} µT" else "HW UNAVAIL", isReady = hasMagneticSensor)
            SensorPill(label = "BLE RADIO", status = if (bleReady) "ENABLED" else "DISABLED", isReady = bleReady)
            SensorPill(label = "SUBNET", status = networkInfo, isReady = networkInfo != "Offline")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Center Radar Sweep Canvas
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(120.dp))
                .background(Color(0xFF06090E))
                .border(1.dp, CyberCardBorder, RoundedCornerShape(120.dp))
                .clickable { onStartScan() }
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            RadarSweepView(
                modifier = Modifier.fillMaxSize(),
                isScanning = true,
                blips = ambientBlips
            )

            // Centered tap prompt overlay
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x99000000))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "TAP TO SCAN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = RadarNeonGreen,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Primary Call To Action: START REAL SCAN
        Button(
            onClick = onStartScan,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("start_scan_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = RadarNeonGreen,
                contentColor = Color(0xFF041A10)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = "Scan",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "START REAL SCAN",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Active optical lens glint • Magnetometer flux • BLE beacon • Subnet RTSP audit",
            fontSize = 10.sp,
            color = TextMuted,
            fontFamily = FontFamily.Monospace,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Latest Scan Report Card (if present in Room database)
        if (latestScan != null) {
            val risk = try { RiskLevel.valueOf(latestScan.riskLevel) } catch (e: Exception) { RiskLevel.CLEAR }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberCard)
                    .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
                    .clickable { onViewHistory() }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LATEST INSPECTION VERDICT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${latestScan.label} • ${SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(latestScan.finishedAt))}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    RiskBadge(riskLevel = risk)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Detection Subsystems Matrix
        Text(
            text = "FOUR-FACTOR HARDWARE RECONNAISSANCE",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModuleCard(
                icon = "📷",
                title = "Optical Lens Glint (CameraX)",
                description = "Differential coaxial flash illumination to pinpoint retro-reflective curved lens surfaces hidden in smoke detectors, alarm clocks, and AC grilles."
            )
            ModuleCard(
                icon = "🧲",
                title = "Tri-Axis Magnetometer (µT)",
                description = "Calibrated background baseline subtraction detecting local electromagnetic surges from concealed switching transformers, inductors, and micro-cameras."
            )
            ModuleCard(
                icon = "📡",
                title = "Bluetooth Low Energy RF",
                description = "Active radio scan with OEM firmware signature database (Wyze, Dahua, Hikvision, V380, XMeye, Tuya) cross-referenced against RSSI proximity."
            )
            ModuleCard(
                icon = "🌐",
                title = "Subnet Surveillance Port Audit",
                description = "Probes active LAN addresses for exposed video streaming ports (RTSP 554, ONVIF 8899, Dahua 37777, Hikvision 8000, and HTTP camera servers)."
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // History & Guide Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onViewHistory,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("view_history_button"),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(CyberCardBorder)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "History (${scans.size})",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedButton(
                onClick = onViewGuide,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("view_guide_button"),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(CyberCardBorder)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Physical Guide",
                    color = RadarCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SensorPill(
    label: String,
    status: String,
    isReady: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isReady) RadarNeonGreen else WarningAmber)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = status,
            fontSize = 10.sp,
            color = if (isReady) TextPrimary else WarningAmber,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}

@Composable
private fun ModuleCard(
    icon: String,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CyberCard)
            .border(1.dp, CyberCardBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(text = icon, fontSize = 20.sp, modifier = Modifier.padding(top = 2.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
