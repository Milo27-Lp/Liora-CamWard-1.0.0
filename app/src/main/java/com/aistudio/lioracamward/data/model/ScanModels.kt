package com.aistudio.lioracamward.data.model

enum class ScanModule(val displayName: String, val icon: String) {
    OPTICAL("Optical Glint", "📷"),
    MAGNETIC("Magnetometer", "🧲"),
    BLUETOOTH("Bluetooth LE", "📡"),
    NETWORK("Local Network", "🌐")
}

enum class Severity(val label: String) {
    INFO("Context Info"),
    SUSPICIOUS("Suspicious"),
    HIGH("High Risk")
}

enum class RiskLevel(val label: String, val description: String) {
    CLEAR("Clear", "No surveillance indicators detected"),
    LOW("Low Risk", "Minor non-specific signals observed"),
    MEDIUM("Medium Risk", "Anomalous physical or radio device detected"),
    HIGH("High Risk", "Multiple corroborating surveillance signatures detected")
}

data class Finding(
    val id: String = java.util.UUID.randomUUID().toString(),
    val module: ScanModule,
    val severity: Severity,
    val title: String,
    val detail: String,
    val evidence: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

data class MagneticReading(
    val magnitude: Float,
    val baseline: Float?,
    val delta: Float?,
    val isCalibrating: Boolean,
    val secondsRemainingCalibration: Int = 0
)

data class BleDeviceObservation(
    val name: String,
    val address: String,
    val rssi: Int,
    val isSuspicious: Boolean,
    val matchedSignature: String? = null
)

data class OpticalCluster(
    val relativeX: Float, // 0.0 to 1.0
    val relativeY: Float, // 0.0 to 1.0
    val brightness: Int,  // 0 to 255
    val compactness: Float,
    val framesPersisted: Int
)
