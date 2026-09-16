package com.aistudio.lioracamward.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey val id: String,
    val label: String,
    val startedAt: Long,
    val finishedAt: Long,
    val riskLevel: String, // CLEAR, LOW, MEDIUM, HIGH
    val modulesRun: String, // Comma-separated module names
    val findingsCount: Int,
    val suspiciousCount: Int,
    val highRiskCount: Int,
    val summaryNotes: String
)
