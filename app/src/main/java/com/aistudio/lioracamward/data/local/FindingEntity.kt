package com.aistudio.lioracamward.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "findings",
    foreignKeys = [
        ForeignKey(
            entity = ScanEntity::class,
            parentColumns = ["id"],
            childColumns = ["scanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("scanId")]
)
data class FindingEntity(
    @PrimaryKey val id: String,
    val scanId: String,
    val module: String, // OPTICAL, MAGNETIC, BLUETOOTH, NETWORK
    val severity: String, // INFO, SUSPICIOUS, HIGH
    val title: String,
    val detail: String,
    val evidenceJson: String,
    val timestamp: Long
)
