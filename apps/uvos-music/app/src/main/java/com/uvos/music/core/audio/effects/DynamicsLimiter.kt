package com.uvos.music.core.audio.effects

import androidx.media3.exoplayer.ExoPlayer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.pow

/**
 * Mastering rack Pre-Gain and Stereo Balance controller.
 * Implements equal-power pan law (-3 dB center).
 */
class DynamicsLimiter(
    private val playerGetter: () -> ExoPlayer?
) {
    private var preGainDb: Float = 0.0f
    private var balance: Float = 0.0f // -1.0 .. 0.0 .. +1.0

    fun setPreGain(gainDb: Float) {
        preGainDb = gainDb.coerceIn(-6.0f, 6.0f)
        updatePlayerVolume()
    }

    fun setBalance(pan: Float) {
        balance = pan.coerceIn(-1.0f, 1.0f)
        updatePlayerVolume()
    }

    /**
     * Updates playback volume applying pre-gain multiplier.
     */
    fun updatePlayerVolume() {
        val player = playerGetter() ?: return

        // Convert dB pre-gain to linear factor: 10^(dB/20)
        val linearFactor = 10.0.pow(preGainDb / 20.0).toFloat()
        // Clamp to prevent digital clipping
        val volume = linearFactor.coerceIn(0.1f, 2.0f)
        player.volume = volume
    }

    /**
     * Equal-power stereo pan law multipliers for Left and Right channels.
     */
    fun calculatePanMultipliers(): Pair<Float, Float> {
        // normalized angle: 0.0 (left) to PI/2 (right)
        val angle = ((balance + 1.0f) / 2.0f) * (PI.toFloat() / 2.0f)
        val leftMultiplier = cos(angle)
        val rightMultiplier = sin(angle)
        return Pair(leftMultiplier, rightMultiplier)
    }
}
