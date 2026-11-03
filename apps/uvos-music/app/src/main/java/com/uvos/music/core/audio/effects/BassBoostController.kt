package com.uvos.music.core.audio.effects

import android.media.audiofx.BassBoost
import android.util.Log

/**
 * Controller for Analog Bass Boost and Vintage Tube Warmth saturation.
 */
class BassBoostController {

    private var bassBoost: BassBoost? = null
    private var strengthNormalized: Float = 0.0f // 0.0 .. 1.0
    private var isWarmTubeEnabled: Boolean = false

    fun attachAudioSession(audioSessionId: Int) {
        release()
        try {
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = strengthNormalized > 0.01f || isWarmTubeEnabled
            }
            applyStrength()
        } catch (e: Exception) {
            Log.w("BassBoostController", "BassBoost init failed: ${e.message}")
        }
    }

    fun setStrength(strength: Float) {
        strengthNormalized = strength.coerceIn(0.0f, 1.0f)
        applyStrength()
    }

    fun setWarmTubeEnabled(enabled: Boolean) {
        isWarmTubeEnabled = enabled
        applyStrength()
    }

    private fun applyStrength() {
        val bb = bassBoost ?: return
        try {
            val shouldEnable = strengthNormalized > 0.01f || isWarmTubeEnabled
            bb.enabled = shouldEnable
            if (shouldEnable) {
                // Base strength mapped to 0..1000 permille
                var targetStrength = (strengthNormalized * 1000f).toInt()
                if (isWarmTubeEnabled) {
                    // Tube saturation adds mild low-end density (+200 permille minimum)
                    targetStrength = (targetStrength + 200).coerceAtMost(1000)
                }
                bb.setStrength(targetStrength.toShort())
            }
        } catch (e: Exception) {
            Log.w("BassBoostController", "Failed setting bass boost: ${e.message}")
        }
    }

    fun release() {
        try {
            bassBoost?.release()
        } catch (_: Exception) {}
        bassBoost = null
    }
}
