package com.uvos.music.core.audio.effects

import android.media.audiofx.Equalizer
import android.util.Log
import com.uvos.music.domain.model.EqualizerBand
import com.uvos.music.domain.model.EqualizerState
import kotlin.math.abs

/**
 * High-precision 10-band studio equalizer controller.
 * Binds to Android AudioTrack's audioSessionId.
 */
class EqualizerController {

    private var equalizer: Equalizer? = null
    private var currentState: EqualizerState = EqualizerState()

    fun attachAudioSession(audioSessionId: Int) {
        release()
        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = currentState.isEnabled
            }
            applyCurrentState()
        } catch (e: Exception) {
            Log.w("EqualizerController", "Hardware Equalizer init failed or not supported: ${e.message}")
        }
    }

    fun setEnabled(enabled: Boolean) {
        currentState = currentState.copy(isEnabled = enabled)
        try {
            equalizer?.enabled = enabled
        } catch (e: Exception) {
            Log.w("EqualizerController", "Failed to set equalizer enabled: ${e.message}")
        }
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        val updatedBands = currentState.bands.toMutableList()
        val targetBand = updatedBands.find { it.index == bandIndex } ?: return
        val clampedGain = gainDb.coerceIn(-12.0f, 12.0f)
        val updated = targetBand.copy(gainDb = clampedGain)
        val idx = updatedBands.indexOf(targetBand)
        updatedBands[idx] = updated

        currentState = currentState.copy(bands = updatedBands)
        applyBandToHardware(updated)
    }

    fun setAllGains(gainsDb: List<Float>) {
        val updatedBands = currentState.bands.mapIndexed { index, band ->
            val gain = gainsDb.getOrElse(index) { 0f }.coerceIn(-12.0f, 12.0f)
            band.copy(gainDb = gain)
        }
        currentState = currentState.copy(bands = updatedBands)
        applyCurrentState()
    }

    fun getCurrentState(): EqualizerState = currentState

    private fun applyCurrentState() {
        val eq = equalizer ?: return
        try {
            eq.enabled = currentState.isEnabled
            for (band in currentState.bands) {
                applyBandToHardware(band)
            }
        } catch (e: Exception) {
            Log.w("EqualizerController", "Error applying EQ state: ${e.message}")
        }
    }

    private fun applyBandToHardware(band: EqualizerBand) {
        val eq = equalizer ?: return
        if (!eq.enabled) return

        try {
            val numHardwareBands = eq.numberOfBands.toInt()
            if (numHardwareBands <= 0) return

            // Find closest matching hardware band for the ISO band frequency
            val targetFreqMilliHz = band.centerFreqHz * 1000
            var closestBandIndex: Short = 0
            var minDiff = Int.MAX_VALUE

            for (i in 0 until numHardwareBands) {
                val hwFreq = eq.getCenterFreq(i.toShort())
                val diff = abs(hwFreq - targetFreqMilliHz)
                if (diff < minDiff) {
                    minDiff = diff
                    closestBandIndex = i.toShort()
                }
            }

            // Convert dB to millibels (1 dB = 100 mB)
            val levelMillibels = (band.gainDb * 100.0f).toInt().coerceIn(
                eq.bandLevelRange[0].toInt(),
                eq.bandLevelRange[1].toInt()
            ).toShort()

            eq.setBandLevel(closestBandIndex, levelMillibels)
        } catch (e: Exception) {
            Log.w("EqualizerController", "Failed setting band ${band.index}: ${e.message}")
        }
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (_: Exception) {}
        equalizer = null
    }
}
