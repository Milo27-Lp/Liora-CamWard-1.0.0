package com.aistudio.lioracamward.detection

import com.aistudio.lioracamward.data.model.Finding
import com.aistudio.lioracamward.data.model.OpticalCluster
import com.aistudio.lioracamward.data.model.ScanModule
import com.aistudio.lioracamward.data.model.Severity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer

class OpticalDetector {

    private val _clusters = MutableStateFlow<List<OpticalCluster>>(emptyList())
    val clusters: StateFlow<List<OpticalCluster>> = _clusters.asStateFlow()

    private val _torchActive = MutableStateFlow(false)
    val torchActive: StateFlow<Boolean> = _torchActive.asStateFlow()

    private val hotspotHistory = mutableMapOf<String, Int>()
    private var onFindingListener: ((Finding) -> Unit)? = null
    private var reportedKeys = mutableSetOf<String>()

    companion object {
        const val BRIGHTNESS_THRESHOLD = 240 // 0-255 scale
        const val MIN_CLUSTER_PX = 3
        const val MAX_CLUSTER_PX = 250
        const val PERSISTENCE_FRAMES_REQUIRED = 5
    }

    fun setTorch(active: Boolean) {
        _torchActive.value = active
    }

    fun start(onFinding: (Finding) -> Unit) {
        onFindingListener = onFinding
        hotspotHistory.clear()
        reportedKeys.clear()
        _clusters.value = emptyList()
    }

    fun stop() {
        hotspotHistory.clear()
        reportedKeys.clear()
        _clusters.value = emptyList()
    }

    /**
     * Analyzes downsampled luminance buffer (Y channel) from CameraX ImageProxy
     */
    fun processLumaPlane(buffer: ByteBuffer, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return

        val sampleW = 80
        val sampleH = 60
        val stepX = (width / sampleW).coerceAtLeast(1)
        val stepY = (height / sampleH).coerceAtLeast(1)

        val detectedThisFrame = mutableListOf<OpticalCluster>()
        val seenGridKeys = mutableSetOf<String>()

        val rowBytes = ByteArray(width)

        for (sy in 0 until sampleH) {
            val srcY = sy * stepY
            if (srcY >= height) break

            buffer.position(srcY * width)
            val available = buffer.remaining().coerceAtMost(width)
            buffer.get(rowBytes, 0, available)

            for (sx in 0 until sampleW) {
                val srcX = sx * stepX
                if (srcX >= available) break

                val luma = rowBytes[srcX].toInt() and 0xFF
                if (luma >= BRIGHTNESS_THRESHOLD) {
                    val relX = sx.toFloat() / sampleW.toFloat()
                    val relY = sy.toFloat() / sampleH.toFloat()

                    // Exclude borders to avoid bezel glares
                    if (relX in 0.1f..0.9f && relY in 0.1f..0.9f) {
                        val gridKey = "${(relX * 10).toInt()}:${(relY * 10).toInt()}"
                        seenGridKeys.add(gridKey)

                        val currentCount = (hotspotHistory[gridKey] ?: 0) + 1
                        hotspotHistory[gridKey] = currentCount

                        val cluster = OpticalCluster(
                            relativeX = relX,
                            relativeY = relY,
                            brightness = luma,
                            compactness = 0.85f,
                            framesPersisted = currentCount
                        )
                        detectedThisFrame.add(cluster)

                        if (currentCount >= PERSISTENCE_FRAMES_REQUIRED && !reportedKeys.contains(gridKey)) {
                            reportedKeys.add(gridKey)
                            onFindingListener?.invoke(
                                Finding(
                                    module = ScanModule.OPTICAL,
                                    severity = Severity.SUSPICIOUS,
                                    title = "Optical Lens Glint Highlight Detected",
                                    detail = "Pinpoint specular reflection observed at screen coordinate (${(relX * 100).toInt()}%, ${(relY * 100).toInt()}%). Lenses create characteristic circular reflections when illuminated by the flashlight. Move slightly to check if reflection alters with viewing angle.",
                                    evidence = mapOf(
                                        "coordinates" to "${(relX * 100).toInt()}%, ${(relY * 100).toInt()}%",
                                        "brightness" to "$luma / 255",
                                        "persistedFrames" to "$currentCount"
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }

        // Clean stale hotspot counters
        val keysToRemove = hotspotHistory.keys.filter { !seenGridKeys.contains(it) }
        keysToRemove.forEach { hotspotHistory.remove(it) }

        _clusters.value = detectedThisFrame
    }
}
