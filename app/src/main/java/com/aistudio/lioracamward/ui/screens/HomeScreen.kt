package com.aistudio.lioracamward.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.lioracamward.ui.components.RadarBlip
import com.aistudio.lioracamward.ui.components.RadarSweepView
import com.aistudio.lioracamward.ui.theme.CyberBackground
import com.aistudio.lioracamward.ui.theme.CyberCard
import com.aistudio.lioracamward.ui.theme.CyberCardBorder
import com.aistudio.lioracamward.ui.theme.RadarCyan
import com.aistudio.lioracamward.ui.theme.RadarNeonGreen
import com.aistudio.lioracamward.ui.theme.TextMuted
import com.aistudio.lioracamward.ui.theme.TextPrimary
import com.aistudio.lioracamward.ui.theme.TextSecondary
import com.aistudio.lioracamward.ui.theme.WarningAmber

@Composable
fun HomeScreen(
    onStartScan: () -> Unit,
    onViewHistory: () -> Unit,
    onViewGuide: () -> Unit
) {
    val scrollState = rememberScrollState()

    val sampleBlips = listOf(
        RadarBlip("b1", 45f, 0.4f, false),
        RadarBlip("b2", 160f, 0.75f, false),
        RadarBlip("b3", 290f, 0.55f, true)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 24.dp),
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
                text = "TACTICAL SURVEILLANCE & SPY DEVICE DETECTOR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.2.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Center Radar Sweep Canvas Preview
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(120.dp))
                .background(Color(0xFF06090E))
                .border(1.dp, CyberCardBorder, RoundedCornerShape(120.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            RadarSweepView(
                modifier = Modifier.fillMaxSize(),
                isScanning = true,
                blips = sampleBlips
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Honest Security Disclaimer Notice
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CyberCard)
                .border(1.dp, WarningAmber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⚠️", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SAFETY & PRIVACY PROTOCOL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.8.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Liora CamWard analyzes physical sensor signals (optical reflections, magnetometer deviations, and BLE radio signatures). Results are indicative and should always be corroborated with careful manual inspection.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Detection Modules Matrix
        Text(
            text = "DETECTION SUBSYSTEMS",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 12.sp,
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
                title = "Optical Reflection Glint",
                description = "Differential flash reflection analysis to detect camera lenses hidden in fixtures"
            )
            ModuleCard(
                icon = "🧲",
                title = "Tri-Axis Magnetometer",
                description = "Calibrated baseline monitoring for anomalous electromagnetic fields & coils"
            )
            ModuleCard(
                icon = "📡",
                title = "Bluetooth Low Energy",
                description = "Radio scan with OEM firmware signature database (Wyze, Dahua, Hikvision, V380)"
            )
            ModuleCard(
                icon = "🌐",
                title = "Network Reconnaissance",
                description = "Subnet analysis, connection classification, and local streaming vector audit"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Start Scan Action Button (Primary CTA)
        Button(
            onClick = onStartScan,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("start_scan_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = RadarNeonGreen,
                contentColor = Color(0xFF041A10)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "START FULL INSPECTION",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                fontFamily = FontFamily.SansSerif
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                    text = "History",
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
                    text = "Detection Guide",
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
