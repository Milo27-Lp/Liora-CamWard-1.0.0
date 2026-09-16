package com.aistudio.lioracamward.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.lioracamward.data.local.ScanEntity
import com.aistudio.lioracamward.data.model.RiskLevel
import com.aistudio.lioracamward.data.repository.ScanRepository
import com.aistudio.lioracamward.ui.components.RiskBadge
import com.aistudio.lioracamward.ui.theme.CyberBackground
import com.aistudio.lioracamward.ui.theme.CyberCard
import com.aistudio.lioracamward.ui.theme.CyberCardBorder
import com.aistudio.lioracamward.ui.theme.DangerRed
import com.aistudio.lioracamward.ui.theme.RadarNeonGreen
import com.aistudio.lioracamward.ui.theme.TextMuted
import com.aistudio.lioracamward.ui.theme.TextPrimary
import com.aistudio.lioracamward.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    scanRepository: ScanRepository,
    onSelectScan: (scanId: String) -> Unit,
    onStartNewScan: () -> Unit
) {
    val scans by scanRepository.allScans.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "INSPECTION LOGS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = RadarNeonGreen,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${scans.size} sessions archived locally",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (scans.isNotEmpty()) {
                TextButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Text(
                        text = "Clear All",
                        color = DangerRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (scans.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🛡️", fontSize = 42.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Inspections Logged",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Run a sweep in your room or hotel to establish\nyour first surveillance audit record.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onStartNewScan,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RadarNeonGreen,
                            contentColor = Color(0xFF041A10)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Start First Scan",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(scans, key = { it.id }) { scan ->
                    ScanHistoryItem(
                        scan = scan,
                        onClick = { onSelectScan(scan.id) }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(text = "Clear All Inspection Logs?")
            },
            text = {
                Text(text = "This will permanently remove all locally archived surveillance audits and captured findings.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            scanRepository.clearHistory()
                            showClearDialog = false
                        }
                    }
                ) {
                    Text(text = "Delete Everything", color = DangerRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}

@Composable
private fun ScanHistoryItem(
    scan: ScanEntity,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(scan.startedAt))
    val durationSeconds = ((scan.finishedAt - scan.startedAt) / 1000).coerceAtLeast(1)

    val riskLevel = try {
        RiskLevel.valueOf(scan.riskLevel)
    } catch (e: Exception) {
        RiskLevel.CLEAR
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberCard)
            .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = scan.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                RiskBadge(riskLevel = riskLevel)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$formattedDate (${durationSeconds}s duration)",
                fontSize = 11.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val modules = scan.modulesRun.split(",")
                    modules.forEach { mod ->
                        val icon = when (mod.trim()) {
                            "OPTICAL" -> "📷"
                            "MAGNETIC" -> "🧲"
                            "BLUETOOTH" -> "📡"
                            else -> "🌐"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF090E16))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = icon, fontSize = 11.sp)
                        }
                    }
                }

                Text(
                    text = "${scan.findingsCount} findings",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
            }
        }
    }
}
