package com.aistudio.lioracamward.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.aistudio.lioracamward.data.model.Severity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Procedural low-latency acoustic feedback engine for physical surveillance scanning.
 * Generates smooth, subtle tactical acoustic cues without external audio asset dependencies.
 */
class ScanAudioPlayer {

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val sampleRate = 22050
    private var lastPlayTime = 0L
    private val minIntervalMs = 120L

    private var infoTrack: AudioTrack? = null
    private var suspiciousTrack: AudioTrack? = null
    private var alertTrack: AudioTrack? = null

    init {
        try {
            // Subtle 680Hz soft sonar blip (70ms)
            val infoPcm = generateTonePcm(frequency = 680.0, durationMs = 70, peakVolume = 0.14f)
            // Melodic two-tone ascending chirp 760Hz -> 1020Hz (110ms)
            val suspiciousPcm = generateDualTonePcm(f1 = 760.0, f2 = 1020.0, durationMs = 110, peakVolume = 0.18f)
            // Tactical staccato pulse 1320Hz double-pip (130ms)
            val alertPcm = generatePulseTonePcm(frequency = 1320.0, durationMs = 130, peakVolume = 0.22f)

            infoTrack = createStaticTrack(infoPcm)
            suspiciousTrack = createStaticTrack(suspiciousPcm)
            alertTrack = createStaticTrack(alertPcm)
        } catch (e: Exception) {
            // Audio hardware initialization safety fallback
        }
    }

    /**
     * Toggles the audio feedback mute state.
     * Returns the new mute state (true if muted, false if unmuted).
     */
    fun toggleMute(): Boolean {
        val next = !_isMuted.value
        _isMuted.value = next
        if (!next) {
            // Provide immediate subtle acoustic feedback when unmuting
            playSubtleFeedback(infoTrack)
        }
        return next
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
    }

    /**
     * Plays a subtle sound cue corresponding to the detected surveillance finding severity.
     */
    fun playFindingSound(severity: Severity) {
        if (_isMuted.value) return

        val now = System.currentTimeMillis()
        if (now - lastPlayTime < minIntervalMs) return
        lastPlayTime = now

        val track = when (severity) {
            Severity.INFO -> infoTrack
            Severity.SUSPICIOUS -> suspiciousTrack
            Severity.HIGH -> alertTrack
        }

        playSubtleFeedback(track)
    }

    private fun playSubtleFeedback(track: AudioTrack?) {
        if (track == null) return
        try {
            synchronized(track) {
                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    track.pause()
                    track.reloadStaticData()
                    track.setPlaybackHeadPosition(0)
                    track.play()
                }
            }
        } catch (e: Exception) {
            // Safely recover if audio hardware output was interrupted
        }
    }

    /**
     * Releases audio hardware buffers when scanning concludes.
     */
    fun release() {
        listOf(infoTrack, suspiciousTrack, alertTrack).forEach { track ->
            try {
                track?.stop()
                track?.release()
            } catch (e: Exception) {
                // Ignore on teardown
            }
        }
        infoTrack = null
        suspiciousTrack = null
        alertTrack = null
    }

    private fun createStaticTrack(pcmData: ByteArray): AudioTrack? {
        return try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(pcmData.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcmData, 0, pcmData.size)
            track
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Synthesizes a soft, exponential-decay sine wave with anti-click envelope.
     */
    private fun generateTonePcm(frequency: Double, durationMs: Int, peakVolume: Float): ByteArray {
        val numSamples = (sampleRate * durationMs) / 1000
        val pcm = ByteArray(numSamples * 2)
        val attackSamples = (sampleRate * 0.006).toInt().coerceAtLeast(1)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val attack = (i.toDouble() / attackSamples).coerceAtMost(1.0)
            val decay = exp(-4.5 * progress)
            val envelope = attack * decay * peakVolume

            val sampleVal = (sin(2.0 * PI * frequency * t) * envelope * Short.MAX_VALUE)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()

            pcm[i * 2] = (sampleVal.toInt() and 0xFF).toByte()
            pcm[i * 2 + 1] = ((sampleVal.toInt() shr 8) and 0xFF).toByte()
        }
        return pcm
    }

    /**
     * Synthesizes a subtle ascending two-tone acoustic blip.
     */
    private fun generateDualTonePcm(f1: Double, f2: Double, durationMs: Int, peakVolume: Float): ByteArray {
        val numSamples = (sampleRate * durationMs) / 1000
        val splitSample = numSamples / 2
        val pcm = ByteArray(numSamples * 2)
        val attackSamples = (sampleRate * 0.005).toInt().coerceAtLeast(1)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = if (i < splitSample) f1 else f2
            val localProgress = if (i < splitSample) {
                i.toDouble() / splitSample
            } else {
                (i - splitSample).toDouble() / (numSamples - splitSample)
            }

            val localAttack = if (i < splitSample) {
                (i.toDouble() / attackSamples).coerceAtMost(1.0)
            } else {
                ((i - splitSample).toDouble() / attackSamples).coerceAtMost(1.0)
            }
            val decay = exp(-3.8 * localProgress)
            val envelope = localAttack * decay * peakVolume

            val sampleVal = (sin(2.0 * PI * freq * t) * envelope * Short.MAX_VALUE)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()

            pcm[i * 2] = (sampleVal.toInt() and 0xFF).toByte()
            pcm[i * 2 + 1] = ((sampleVal.toInt() shr 8) and 0xFF).toByte()
        }
        return pcm
    }

    /**
     * Synthesizes a subtle double-pulse acoustic warning chirp.
     */
    private fun generatePulseTonePcm(frequency: Double, durationMs: Int, peakVolume: Float): ByteArray {
        val numSamples = (sampleRate * durationMs) / 1000
        val pcm = ByteArray(numSamples * 2)
        val pulse1End = (numSamples * 0.4).toInt()
        val pulse2Start = (numSamples * 0.55).toInt()
        val attackSamples = (sampleRate * 0.004).toInt().coerceAtLeast(1)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope: Double = when {
                i < pulse1End -> {
                    val attack = (i.toDouble() / attackSamples).coerceAtMost(1.0)
                    val decay = exp(-4.0 * (i.toDouble() / pulse1End))
                    attack * decay * peakVolume
                }
                i in pulse2Start until numSamples -> {
                    val idx = i - pulse2Start
                    val span = numSamples - pulse2Start
                    val attack = (idx.toDouble() / attackSamples).coerceAtMost(1.0)
                    val decay = exp(-4.0 * (idx.toDouble() / span))
                    attack * decay * (peakVolume * 1.15)
                }
                else -> 0.0
            }

            val sampleVal = (sin(2.0 * PI * frequency * t) * envelope * Short.MAX_VALUE)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()

            pcm[i * 2] = (sampleVal.toInt() and 0xFF).toByte()
            pcm[i * 2 + 1] = ((sampleVal.toInt() shr 8) and 0xFF).toByte()
        }
        return pcm
    }
}
