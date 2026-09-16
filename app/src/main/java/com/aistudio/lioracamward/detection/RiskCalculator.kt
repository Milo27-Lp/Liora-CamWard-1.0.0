package com.aistudio.lioracamward.detection

import com.aistudio.lioracamward.data.model.Finding
import com.aistudio.lioracamward.data.model.RiskLevel
import com.aistudio.lioracamward.data.model.ScanModule
import com.aistudio.lioracamward.data.model.Severity

/**
 * Deterministic, evidence-based risk scoring ported from Liora CamWard core.
 * Never random: the same set of findings always yields the identical risk level.
 *
 * Multi-module corroboration:
 *   If two or more INDEPENDENT modules emit suspicious or high findings,
 *   escalate directly to HIGH risk (e.g. optical glint + BLE surveillance match).
 */
object RiskCalculator {

    fun computeRiskLevel(findings: List<Finding>): RiskLevel {
        val alertFindings = findings.filter {
            it.severity == Severity.SUSPICIOUS || it.severity == Severity.HIGH
        }

        val highCount = alertFindings.count { it.severity == Severity.HIGH }
        val suspiciousCount = alertFindings.size

        // Multi-module corroboration check
        val modulesWithAlert = alertFindings.map { it.module }.toSet()
        if (modulesWithAlert.size >= 2) {
            return RiskLevel.HIGH
        }

        // Single-module thresholds
        return when {
            highCount >= 1 -> RiskLevel.HIGH
            suspiciousCount >= 3 -> RiskLevel.HIGH
            suspiciousCount >= 2 -> RiskLevel.MEDIUM
            suspiciousCount >= 1 -> RiskLevel.LOW
            else -> RiskLevel.CLEAR
        }
    }

    fun getRecommendedAction(riskLevel: RiskLevel): String {
        return when (riskLevel) {
            RiskLevel.CLEAR -> "No indicators found. The inspected area shows standard ambient magnetic and radio signatures."
            RiskLevel.LOW -> "Minor isolated anomaly observed. Re-scan the specific sector from multiple angles to verify."
            RiskLevel.MEDIUM -> "Suspicious device detected nearby. Inspect power outlets, smoke detectors, wall clocks, and picture frames in this perimeter."
            RiskLevel.HIGH -> "High probability of active surveillance hardware. Turn off ambient lights, look closely with the camera torch for pinhole lenses, and inspect anomalous fixtures."
        }
    }
}
