package com.aistudio.lioracamward.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.aistudio.lioracamward.data.local.ScanEntity
import com.aistudio.lioracamward.data.model.Finding
import com.aistudio.lioracamward.data.model.RiskLevel
import com.aistudio.lioracamward.data.repository.ScanRepository
import com.aistudio.lioracamward.detection.RiskCalculator
import com.aistudio.lioracamward.ui.components.FindingCard
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScanDetailScreen(
    scanId: String,
    scanRepository: ScanRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var scanEntity by remember { mutableStateOf<ScanEntity?>(null) }
    val findings by scanRepository.getFindingsForScan(scanId).collectAsState(initial = emptyList())

    LaunchedEffect(scanId) {
        scanEntity = scanRepository.getScanById(scanId)
    }

    val scan = scanEntity

    fun shareReport() {
        scan ?: return
        val reportText = buildString {
            appendLine("=== LIORA CAMWARD INSPECTION REPORT ===")
            appendLine("Session: ${scan.label}")
            appendLine("Risk Level: ${scan.riskLevel}")
            appendLine("Findings Count: ${scan.findingsCount}")
            appendLine("Suspicious Findings: ${scan.suspiciousCount}")
            appendLine("High Risk Findings: ${scan.highRiskCount}")
            appendLine("Advice: ${scan.summaryNotes}")
            appendLine("\n--- DETAILED FINDINGS ---")
            findings.forEachIndexed { i, f ->
                appendLine("${i + 1}. [${f.module.displayName} - ${f.severity.label}] ${f.title}")
                appendLine("   Detail: ${f.detail}")
                if (f.evidence.isNotEmpty()) {
                    appendLine("   Evidence: ${f.evidence.entries.joinToString(", ") { "${it.key}=${it.value}" }}")
                }
            }
            appendLine("\nGenerated with Liora CamWard Android")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, reportText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Inspection Audit")
        context.startActivity(shareIntent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        // Top Navigation Bar
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

            Button(
                onClick = { shareReport() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = RadarCyan,
                    contentColor = Color(0xFF031B24)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("share_report_button")
            ) {
                Text(text = "Share Report", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (scan == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Loading inspection details...", color = TextMuted)
            }
        } else {
            val risk = try {
                RiskLevel.valueOf(scan.riskLevel)
            } catch (e: Exception) {
                RiskLevel.CLEAR
            }

            val dateFormat = SimpleDateFormat("MMMM d, yyyy · HH:mm", Locale.getDefault())
            val formattedDate = dateFormat.format(Date(scan.startedAt))
            val durationSeconds = ((scan.finishedAt - scan.startedAt) / 1000).coerceAtLeast(1)

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Risk Overview Banner Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberCard)
                            .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = scan.label,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                RiskBadge(riskLevel = risk)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "$formattedDate (${durationSeconds}s sweep)",
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Actionable recommendation
                            Text(
                                text = "SECURITY RECOMMENDATION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = RadarNeonGreen,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = RiskCalculator.getRecommendedAction(risk),
                                fontSize = 13.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Findings Section Title
                item {
                    Text(
                        text = "CAPTURED EVIDENCE & FINDINGS (${findings.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (findings.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberCard)
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Zero physical or radio anomalies recorded during this sweep.",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                items(findings, key = { it.id }) { finding ->
                    FindingCard(finding = finding)
                }

                // Delete button at bottom of detail
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                scanRepository.deleteScan(scan.id)
                                onBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(DangerRed.copy(alpha = 0.5f))
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Delete This Audit", color = DangerRed, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
