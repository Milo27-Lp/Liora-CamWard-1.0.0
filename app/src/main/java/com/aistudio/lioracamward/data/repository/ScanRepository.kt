package com.aistudio.lioracamward.data.repository

import com.aistudio.lioracamward.data.local.FindingEntity
import com.aistudio.lioracamward.data.local.ScanDao
import com.aistudio.lioracamward.data.local.ScanEntity
import com.aistudio.lioracamward.data.model.Finding
import com.aistudio.lioracamward.data.model.RiskLevel
import com.aistudio.lioracamward.data.model.ScanModule
import com.aistudio.lioracamward.data.model.Severity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScanRepository(private val scanDao: ScanDao) {

    val allScans: Flow<List<ScanEntity>> = scanDao.getAllScans()

    fun getFindingsForScan(scanId: String): Flow<List<Finding>> {
        return scanDao.getFindingsForScan(scanId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getScanById(scanId: String): ScanEntity? {
        return scanDao.getScanById(scanId)
    }

    suspend fun getFindingsListForScan(scanId: String): List<Finding> {
        return scanDao.getFindingsListForScan(scanId).map { it.toDomain() }
    }

    suspend fun saveScanSession(
        id: String,
        label: String,
        startedAt: Long,
        finishedAt: Long,
        riskLevel: RiskLevel,
        modulesRun: List<ScanModule>,
        findings: List<Finding>,
        notes: String = ""
    ) {
        val suspiciousCount = findings.count { it.severity == Severity.SUSPICIOUS }
        val highRiskCount = findings.count { it.severity == Severity.HIGH }

        val scanEntity = ScanEntity(
            id = id,
            label = label,
            startedAt = startedAt,
            finishedAt = finishedAt,
            riskLevel = riskLevel.name,
            modulesRun = modulesRun.joinToString(",") { it.name },
            findingsCount = findings.size,
            suspiciousCount = suspiciousCount,
            highRiskCount = highRiskCount,
            summaryNotes = notes
        )
        scanDao.insertScan(scanEntity)

        if (findings.isNotEmpty()) {
            val findingEntities = findings.map { finding ->
                FindingEntity(
                    id = finding.id,
                    scanId = id,
                    module = finding.module.name,
                    severity = finding.severity.name,
                    title = finding.title,
                    detail = finding.detail,
                    evidenceJson = finding.evidence.entries.joinToString(";") { "${it.key}=${it.value}" },
                    timestamp = finding.timestamp
                )
            }
            scanDao.insertFindings(findingEntities)
        }
    }

    suspend fun deleteScan(id: String) {
        scanDao.deleteScanById(id)
    }

    suspend fun clearHistory() {
        scanDao.clearAllScans()
    }

    private fun FindingEntity.toDomain(): Finding {
        val evidenceMap = if (evidenceJson.isBlank()) {
            emptyMap()
        } else {
            evidenceJson.split(";").mapNotNull { pair ->
                val parts = pair.split("=", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else null
            }.toMap()
        }

        return Finding(
            id = id,
            module = try { ScanModule.valueOf(module) } catch (e: Exception) { ScanModule.NETWORK },
            severity = try { Severity.valueOf(severity) } catch (e: Exception) { Severity.INFO },
            title = title,
            detail = detail,
            evidence = evidenceMap,
            timestamp = timestamp
        )
    }
}
