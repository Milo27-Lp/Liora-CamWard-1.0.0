package com.aistudio.lioracamward

import com.aistudio.lioracamward.data.model.Finding
import com.aistudio.lioracamward.data.model.RiskLevel
import com.aistudio.lioracamward.data.model.ScanModule
import com.aistudio.lioracamward.data.model.Severity
import com.aistudio.lioracamward.detection.RiskCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class RiskCalculatorTest {

    @Test
    fun testEmptyFindingsReturnsClear() {
        val level = RiskCalculator.computeRiskLevel(emptyList())
        assertEquals(RiskLevel.CLEAR, level)
    }

    @Test
    fun testSingleSuspiciousReturnsLowRisk() {
        val findings = listOf(
            Finding(
                module = ScanModule.MAGNETIC,
                severity = Severity.SUSPICIOUS,
                title = "Magnetic Anomaly",
                detail = "Delta elevated"
            )
        )
        val level = RiskCalculator.computeRiskLevel(findings)
        assertEquals(RiskLevel.LOW, level)
    }

    @Test
    fun testMultiModuleSuspiciousEscalatesToHigh() {
        val findings = listOf(
            Finding(
                module = ScanModule.MAGNETIC,
                severity = Severity.SUSPICIOUS,
                title = "Magnetic Anomaly",
                detail = "Delta elevated"
            ),
            Finding(
                module = ScanModule.OPTICAL,
                severity = Severity.SUSPICIOUS,
                title = "Optical Glint",
                detail = "Specular reflection"
            )
        )
        val level = RiskCalculator.computeRiskLevel(findings)
        assertEquals(RiskLevel.HIGH, level)
    }

    @Test
    fun testHighSeverityReturnsHighRisk() {
        val findings = listOf(
            Finding(
                module = ScanModule.BLUETOOTH,
                severity = Severity.HIGH,
                title = "Confirmed Surveillance Match",
                detail = "Known spy camera hardware signature"
            )
        )
        val level = RiskCalculator.computeRiskLevel(findings)
        assertEquals(RiskLevel.HIGH, level)
    }
}
