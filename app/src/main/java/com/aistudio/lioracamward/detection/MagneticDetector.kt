package com.aistudio.lioracamward.detection

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.aistudio.lioracamward.data.model.Finding
import com.aistudio.lioracamward.data.model.MagneticReading
import com.aistudio.lioracamward.data.model.ScanModule
import com.aistudio.lioracamward.data.model.Severity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class MagneticDetector(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _reading = MutableStateFlow(MagneticReading(0f, null, null, false, 0))
    val reading: StateFlow<MagneticReading> = _reading.asStateFlow()

    private var isScanning = false
    private val baselineSamples = mutableListOf<Float>()
    private var baselineMagnitude: Float? = null
    private var baselineDone = false
    private var anomalyStreak = 0
    private var anomalyReported = false

    private var calibrationJob: Job? = null
    private var onFindingListener: ((Finding) -> Unit)? = null

    companion object {
        const val BASELINE_CALIBRATION_SECONDS = 3
        const val ANOMALY_DELTA_THRESHOLD_UT = 25f // Spy cameras & power converters elevate local field by 25-40 µT
        const val ANOMALY_CONFIRM_COUNT = 3
        const val DRIFT_CALM = 0.04f
        const val DRIFT_ACTIVE = 0.004f
    }

    val isAvailable: Boolean
        get() = magnetometer != null

    fun start(scope: CoroutineScope, onFinding: (Finding) -> Unit) {
        if (isScanning) return
        onFindingListener = onFinding
        if (!isAvailable) {
            onFinding(
                Finding(
                    module = ScanModule.MAGNETIC,
                    severity = Severity.INFO,
                    title = "Magnetometer Hardware Unavailable",
                    detail = "No physical magnetic flux sensor detected on this device/emulator. Optical camera glint, Bluetooth LE, and local network audits remain operational.",
                    evidence = mapOf("sensorAvailable" to "false")
                )
            )
            return
        }
        isScanning = true
        baselineSamples.clear()
        baselineMagnitude = null
        baselineDone = false
        anomalyStreak = 0
        anomalyReported = false

        _reading.value = MagneticReading(
            magnitude = 0f,
            baseline = null,
            delta = null,
            isCalibrating = true,
            secondsRemainingCalibration = BASELINE_CALIBRATION_SECONDS
        )

        sensorManager?.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)

        // Calibration timer countdown
        calibrationJob = scope.launch(Dispatchers.Default) {
            for (sec in BASELINE_CALIBRATION_SECONDS downTo 1) {
                _reading.value = _reading.value.copy(
                    isCalibrating = true,
                    secondsRemainingCalibration = sec
                )
                delay(1000)
            }

            // Calibration done
            if (baselineSamples.isNotEmpty()) {
                val avg = baselineSamples.average().toFloat()
                baselineMagnitude = avg
                baselineDone = true

                _reading.value = _reading.value.copy(
                    baseline = avg,
                    isCalibrating = false,
                    secondsRemainingCalibration = 0
                )

                onFindingListener?.invoke(
                    Finding(
                        module = ScanModule.MAGNETIC,
                        severity = Severity.INFO,
                        title = "Magnetic Baseline Calibrated: ${String.format("%.1f", avg)} µT",
                        detail = "Ambient magnetic field baseline established. Move the phone slowly near wall outlets, smoke detectors, clocks, and lamps. A sustained increase > 25 µT indicates hidden electronic circuitry.",
                        evidence = mapOf("baselineMicroTesla" to String.format("%.2f", avg))
                    )
                )
            }
        }
    }

    fun stop() {
        if (!isScanning) return
        isScanning = false
        calibrationJob?.cancel()
        calibrationJob = null
        sensorManager?.unregisterListener(this)
        _reading.value = _reading.value.copy(isCalibrating = false)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_MAGNETIC_FIELD) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)

        if (!baselineDone) {
            baselineSamples.add(magnitude)
            _reading.value = _reading.value.copy(magnitude = magnitude)
            return
        }

        val base = baselineMagnitude ?: return
        val signedDelta = magnitude - base
        val isAnomaly = signedDelta > ANOMALY_DELTA_THRESHOLD_UT

        // Drift baseline gradually to adapt to normal room movement
        baselineMagnitude = if (isAnomaly) {
            base * (1f - DRIFT_ACTIVE) + magnitude * DRIFT_ACTIVE
        } else {
            base * (1f - DRIFT_CALM) + magnitude * DRIFT_CALM
        }

        _reading.value = MagneticReading(
            magnitude = magnitude,
            baseline = baselineMagnitude,
            delta = signedDelta,
            isCalibrating = false,
            secondsRemainingCalibration = 0
        )

        if (isAnomaly) {
            anomalyStreak++
            if (anomalyStreak >= ANOMALY_CONFIRM_COUNT && !anomalyReported) {
                anomalyReported = true
                onFindingListener?.invoke(
                    Finding(
                        module = ScanModule.MAGNETIC,
                        severity = Severity.SUSPICIOUS,
                        title = "Magnetic Anomaly: +${String.format("%.1f", signedDelta)} µT",
                        detail = "Sustained anomalous magnetic field detected exceeding baseline by +${String.format("%.1f", signedDelta)} µT. Characteristic of miniature transformers, camera lens coils, or hidden battery-powered recording modules.",
                        evidence = mapOf(
                            "magnitude" to String.format("%.1f", magnitude),
                            "baseline" to String.format("%.1f", base),
                            "delta" to String.format("%.1f", signedDelta)
                        )
                    )
                )
            }
        } else {
            anomalyStreak = 0
            if (anomalyReported) {
                anomalyReported = false
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
