package com.uvos.music.core.audio.ballistics

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * High-performance RMS and dBFS calculator for stereo PCM audio stream.
 */
object RmsCalculator {

    const val REFERENCE_0_VU_DBFS = -14.0f
    const val CLIP_THRESHOLD_DBFS = -0.5f

    data class ChannelMetrics(
        val rmsDb: Float,
        val peakDb: Float,
        val normalizedVu: Float,
        val isClipping: Boolean
    )

    /**
     * Computes RMS, peak and VU meter position for a single channel float buffer.
     */
    fun calculateMetricsFromFloat(
        samples: FloatArray,
        offset: Int,
        stride: Int,
        count: Int,
        padDb: Float = 0.0f
    ): ChannelMetrics {
        if (count <= 0) {
            return ChannelMetrics(-90f, -90f, 0f, false)
        }

        var sumSquare = 0.0
        var maxPeak = 0.0f

        var idx = offset
        for (i in 0 until count) {
            val sample = samples[idx]
            val absSample = if (sample < 0) -sample else sample
            if (absSample > maxPeak) {
                maxPeak = absSample
            }
            sumSquare += (sample * sample).toDouble()
            idx += stride
        }

        val meanSquare = sumSquare / count
        val rms = sqrt(meanSquare).toFloat()

        val rmsDb = 20.0f * log10(max(rms, 1e-5f))
        val peakDb = 20.0f * log10(max(maxPeak, 1e-5f))

        val normalizedVu = dbfsToVuNormalized(rmsDb, padDb)
        val isClipping = (peakDb + padDb) >= CLIP_THRESHOLD_DBFS

        return ChannelMetrics(rmsDb, peakDb, normalizedVu, isClipping)
    }

    /**
     * Computes RMS, peak and VU meter position from interleaved 16-bit PCM ByteBuffer.
     */
    fun calculateMetricsFromPcm16(
        byteBuffer: ByteBuffer,
        channelIndex: Int,
        totalChannels: Int,
        padDb: Float = 0.0f
    ): ChannelMetrics {
        val remainingBytes = byteBuffer.remaining()
        val totalSamples = remainingBytes / 2
        val channelSamples = totalSamples / totalChannels

        if (channelSamples <= 0) {
            return ChannelMetrics(-90f, -90f, 0f, false)
        }

        val shortBuffer = byteBuffer.asShortBuffer()
        var sumSquare = 0.0
        var maxPeak = 0.0f

        var idx = channelIndex
        for (i in 0 until channelSamples) {
            if (idx >= shortBuffer.limit()) break
            val raw = shortBuffer.get(idx)
            val sample = raw / 32768.0f
            val absSample = if (sample < 0) -sample else sample
            if (absSample > maxPeak) {
                maxPeak = absSample
            }
            sumSquare += (sample * sample).toDouble()
            idx += totalChannels
        }

        val meanSquare = sumSquare / channelSamples
        val rms = sqrt(meanSquare).toFloat()

        val rmsDb = 20.0f * log10(max(rms, 1e-5f))
        val peakDb = 20.0f * log10(max(maxPeak, 1e-5f))

        val normalizedVu = dbfsToVuNormalized(rmsDb, padDb)
        val isClipping = (peakDb + padDb) >= CLIP_THRESHOLD_DBFS

        return ChannelMetrics(rmsDb, peakDb, normalizedVu, isClipping)
    }

    /**
     * Converts dBFS audio level to normalized VU meter position.
     * Scale range:
     * -20 VU (0.00) to 0 VU (1.00), and +3 VU (1.15).
     * Calibration: 0 VU = REFERENCE_0_VU_DBFS + padDb (e.g. -14 dBFS with 0dB pad).
     */
    fun dbfsToVuNormalized(dBFS: Float, padDb: Float = 0.0f): Float {
        val effectiveDb = dBFS + padDb
        val zeroVuDb = REFERENCE_0_VU_DBFS
        val minVuDb = zeroVuDb - 20.0f // -34 dBFS

        if (effectiveDb <= minVuDb) {
            return 0.0f
        }

        val vuDb = effectiveDb - zeroVuDb // -20 .. 0 .. +3

        return if (vuDb <= 0.0f) {
            // Mapping -20 dB to 0 dB -> normalized 0.0 .. 1.0
            (vuDb + 20.0f) / 20.0f
        } else {
            // Mapping 0 dB to +3 dB -> normalized 1.0 .. 1.15
            1.0f + (vuDb / 3.0f) * 0.15f
        }
    }
}
