package com.aistudio.lioracamward.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.aistudio.lioracamward.ui.theme.DangerRed
import com.aistudio.lioracamward.ui.theme.RadarCyan
import com.aistudio.lioracamward.ui.theme.RadarNeonGreen
import kotlin.math.cos
import kotlin.math.sin

data class RadarBlip(
    val id: String,
    val angleDegrees: Float,
    val distanceRatio: Float, // 0.1 to 0.9
    val isAlert: Boolean = false,
    val label: String = ""
)

@Composable
fun RadarSweepView(
    modifier: Modifier = Modifier,
    isScanning: Boolean = true,
    blips: List<RadarBlip> = emptyList()
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BlipPulse"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) * 0.92f

            // Background concentric rings
            val ringCount = 4
            for (i in 1..ringCount) {
                val r = radius * (i.toFloat() / ringCount.toFloat())
                drawCircle(
                    color = RadarNeonGreen.copy(alpha = 0.15f),
                    radius = r,
                    center = center,
                    style = Stroke(width = 1.5f)
                )
            }

            // Crosshair lines
            drawLine(
                color = RadarNeonGreen.copy(alpha = 0.25f),
                start = Offset(center.x - radius, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = 1.5f
            )
            drawLine(
                color = RadarNeonGreen.copy(alpha = 0.25f),
                start = Offset(center.x, center.y - radius),
                end = Offset(center.x, center.y + radius),
                strokeWidth = 1.5f
            )

            // Outer boundary circle
            drawCircle(
                color = RadarCyan.copy(alpha = 0.4f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.5f)
            )

            // Sweeping beam arc & trail
            if (isScanning) {
                rotate(degrees = angle, pivot = center) {
                    val sweepBrush = Brush.sweepGradient(
                        0.0f to Color.Transparent,
                        0.85f to Color.Transparent,
                        0.97f to RadarNeonGreen.copy(alpha = 0.12f),
                        1.0f to RadarNeonGreen.copy(alpha = 0.5f),
                        center = center
                    )
                    drawCircle(
                        brush = sweepBrush,
                        radius = radius,
                        center = center
                    )

                    // Beam leading edge
                    drawLine(
                        color = RadarNeonGreen,
                        start = center,
                        end = Offset(center.x + radius, center.y),
                        strokeWidth = 2.5f
                    )
                }
            }

            // Render detected targets / blips
            for (blip in blips) {
                val rad = Math.toRadians(blip.angleDegrees.toDouble())
                val blipDist = radius * blip.distanceRatio.coerceIn(0.1f, 0.95f)
                val bx = center.x + (blipDist * cos(rad)).toFloat()
                val by = center.y + (blipDist * sin(rad)).toFloat()

                val blipColor = if (blip.isAlert) DangerRed else RadarCyan

                // Outer pulsing halo
                drawCircle(
                    color = blipColor.copy(alpha = 0.35f),
                    radius = 9f * pulseScale,
                    center = Offset(bx, by)
                )
                // Center solid core
                drawCircle(
                    color = blipColor,
                    radius = 4.5f,
                    center = Offset(bx, by)
                )
            }

            // Center radar transceiver dot
            drawCircle(
                color = RadarNeonGreen,
                radius = 5f,
                center = center
            )
        }
    }
}
