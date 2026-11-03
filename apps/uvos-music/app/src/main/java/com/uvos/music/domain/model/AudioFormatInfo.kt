package com.uvos.music.domain.model

/**
 * Hi-Res audio format metadata representation.
 */
data class AudioFormatInfo(
    val container: String = "PCM", // FLAC, WAV, ALAC, MP3, OGG, OPUS, AAC
    val sampleRateHz: Int = 44100,
    val bitDepth: Int = 16,        // 16-bit, 24-bit, 32-bit float
    val bitrateKbps: Int = 1411,
    val channelCount: Int = 2,
    val isLossless: Boolean = true,
    val isHiRes: Boolean = sampleRateHz >= 48000 || bitDepth > 16
) {
    val displayFormatBadge: String
        get() {
            val resType = if (isHiRes) "Hi-Res" else if (isLossless) "Lossless" else "Lossy"
            return "$container $bitDepth-bit / ${sampleRateHz / 1000} kHz ($resType)"
        }
}
