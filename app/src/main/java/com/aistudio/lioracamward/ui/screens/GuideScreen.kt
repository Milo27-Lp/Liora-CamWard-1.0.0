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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun GuideScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        // Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(8.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(CyberCardBorder)
                )
            ) {
                Text(text = "← Back", color = TextPrimary, fontSize = 12.sp)
            }

            Text(
                text = "DETECTION HANDBOOK",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = RadarCyan,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Intro Callout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberCard)
                    .border(1.dp, RadarNeonGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "PHYSICAL SWEEP METHODOLOGY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = RadarNeonGreen,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Automated sensors and RF detectors are powerful reconnaissance tools, but high-assurance security requires disciplined physical observation. Follow the multi-layer protocol below.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }
            }

            GuideSection(
                number = "01",
                title = "Optical Lens Retro-Reflection",
                content = "Every optical lens consists of curved glass elements coated to focus light onto a sensor. When struck by an on-axis light beam (like your phone's flashlight in a dark room), light reflects directly back along the incident path. Turn off room lights, activate the torch, and look for pinpoint glints that stay fixed as you tilt your perspective slightly."
            )

            GuideSection(
                number = "02",
                title = "Tri-Axis Magnetic Anomaly Scanning",
                content = "Covert cameras require continuous power or compact transformers to operate. Even pinhole cameras incorporate miniature switching regulators and copper coils that create localized magnetic disturbances. Hold the top of your phone within 2-5 cm of suspected objects. A sharp jump (>25 µT) over baseline indicates internal active electronics."
            )

            GuideSection(
                number = "03",
                title = "Top 7 Concealment Hotspots",
                content = "• Smoke Detectors (especially non-standard or pointing towards beds)\n• AC Power Outlets & USB Charging Adapters\n• Digital Alarm Clocks & Radio docks\n• Wall-Mounted Mirrors (perform fingernail touch test)\n• Picture frames & decorative canvas edges\n• Tissue boxes and fake plant containers\n• TV brackets and soundbars"
            )

            GuideSection(
                number = "04",
                title = "Bluetooth LE & Network Broadcasts",
                content = "Cheap surveillance cameras stream video over 2.4 GHz WiFi or pair via BLE during boot. Firmware from common OEM modules often reveals itself via distinctive device names (V380, XMeye, Yoosee, IPCam, Tuya). If you have access to the local router, inspect the DHCP client table for unknown MAC vendors."
            )

            GuideSection(
                number = "05",
                title = "What To Do If You Find A Camera",
                content = "1. Do NOT destroy or disconnect the device immediately (preserve forensic chain of evidence).\n2. Take high-resolution photos and video of the camera in its concealed position.\n3. Cover the lens with a towel or opaque tape.\n4. File a report with local law enforcement or the booking platform (Airbnb/hotel)."
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun GuideSection(
    number: String,
    title: String,
    content: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberCard)
            .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = number,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = RadarCyan,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}
