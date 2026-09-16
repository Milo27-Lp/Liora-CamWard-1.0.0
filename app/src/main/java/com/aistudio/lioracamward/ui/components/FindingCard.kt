package com.aistudio.lioracamward.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.lioracamward.data.model.Finding
import com.aistudio.lioracamward.data.model.Severity
import com.aistudio.lioracamward.ui.theme.CyberCard
import com.aistudio.lioracamward.ui.theme.CyberCardBorder
import com.aistudio.lioracamward.ui.theme.DangerRed
import com.aistudio.lioracamward.ui.theme.InfoBlue
import com.aistudio.lioracamward.ui.theme.RadarCyan
import com.aistudio.lioracamward.ui.theme.TextMuted
import com.aistudio.lioracamward.ui.theme.TextPrimary
import com.aistudio.lioracamward.ui.theme.TextSecondary
import com.aistudio.lioracamward.ui.theme.WarningAmber

@Composable
fun FindingCard(
    finding: Finding,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val borderColor = when (finding.severity) {
        Severity.HIGH -> DangerRed
        Severity.SUSPICIOUS -> WarningAmber
        Severity.INFO -> CyberCardBorder
    }

    val severityBadgeColor = when (finding.severity) {
        Severity.HIGH -> DangerRed
        Severity.SUSPICIOUS -> WarningAmber
        Severity.INFO -> InfoBlue
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberCard)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = finding.module.icon, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = finding.module.displayName.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = RadarCyan,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.8.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(severityBadgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = finding.severity.label.uppercase(),
                        color = severityBadgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = finding.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = finding.detail,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = TextSecondary
            )

            if (finding.evidence.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expanded) "Hide Evidence ▲" else "View Hardware Evidence ▼",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RadarCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF090E16))
                            .padding(10.dp)
                    ) {
                        finding.evidence.forEach { (key, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = value,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
