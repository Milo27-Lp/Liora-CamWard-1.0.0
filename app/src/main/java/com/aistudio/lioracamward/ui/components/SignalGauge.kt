package com.aistudio.lioracamward.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.lioracamward.data.model.MagneticReading
import com.aistudio.lioracamward.ui.theme.CyberCard
import com.aistudio.lioracamward.ui.theme.CyberCardBorder
import com.aistudio.lioracamward.ui.theme.DangerRed
import com.aistudio.lioracamward.ui.theme.RadarCyan
import com.aistudio.lioracamward.ui.theme.RadarNeonGreen
import com.aistudio.lioracamward.ui.theme.TextMuted
import com.aistudio.lioracamward.ui.theme.TextPrimary
import com.aistudio.lioracamward.ui.theme.TextSecondary
import com.aistudio.lioracamward.ui.theme.WarningAmber

@Composable
fun SignalGauge(
    reading: MagneticReading,
    modifier: Modifier = Modifier
) {
    val maxScale = 120f
    val currentRatio = (reading.magnitude / maxScale).coerceIn(0f, 1f)
    val animatedRatio by animateFloatAsState(targetValue = currentRatio, label = "GaugeRatio")

    val delta = reading.delta ?: 0f
    val isAlert = delta > 25f

    val gaugeColor = when {
        reading.isCalibrating -> RadarCyan
        isAlert -> DangerRed
        delta > 12f -> WarningAmber
        else -> RadarNeonGreen
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberCard)
            .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🧲", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MAGNETOMETER",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (reading.isCalibrating) {
                    Text(
                        text = "CALIBRATING (${reading.secondsRemainingCalibration}s)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = RadarCyan,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    val deltaSign = if (delta >= 0f) "+" else ""
                    Text(
                        text = "Δ $deltaSign${String.format("%.1f", delta)} µT",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = gaugeColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Gauge meter bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFF090E16))
                    .border(1.dp, Color(0xFF1E2B3E), RoundedCornerShape(7.dp))
            ) {
                // Active fill bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedRatio)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(RadarNeonGreen, gaugeColor)
                            )
                        )
                )

                // Baseline reference notch
                val baseline = reading.baseline
                if (baseline != null && baseline > 0f) {
                    val baseRatio = (baseline / maxScale).coerceIn(0.05f, 0.95f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(baseRatio)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(Color.White)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Live: ${String.format("%.1f", reading.magnitude)} µT",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (reading.baseline != null) "Base: ${String.format("%.1f", reading.baseline)} µT" else "Calibrating baseline...",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
