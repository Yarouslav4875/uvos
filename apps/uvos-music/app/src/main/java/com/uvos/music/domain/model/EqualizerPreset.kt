package com.uvos.music.domain.model

/**
 * 10-band ISO Studio Equalizer configuration.
 */
data class EqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val label: String,
    val gainDb: Float = 0.0f // -12.0 dB .. +12.0 dB
)

data class EqualizerState(
    val isEnabled: Boolean = true,
    val bands: List<EqualizerBand> = defaultBands(),
    val bassBoost: Float = 0.0f,     // 0.0 .. 1.0 (Rotary knob)
    val warmTube: Boolean = false,   // Warm Tube Saturation toggle
    val preGainDb: Float = 0.0f,     // -6.0 dB .. +6.0 dB
    val balance: Float = 0.0f,       // -1.0 (Full Left) .. 0.0 (Center) .. +1.0 (Full Right)
    val vuSensitivityPadDb: Float = 0.0f // 0 dB or +6 dB
) {
    companion object {
        fun defaultBands(): List<EqualizerBand> = listOf(
            EqualizerBand(0, 31, "31"),
            EqualizerBand(1, 62, "62"),
            EqualizerBand(2, 125, "125"),
            EqualizerBand(3, 250, "250"),
            EqualizerBand(4, 500, "500"),
            EqualizerBand(5, 1000, "1k"),
            EqualizerBand(6, 2000, "2k"),
            EqualizerBand(7, 4000, "4k"),
            EqualizerBand(8, 8000, "8k"),
            EqualizerBand(9, 16000, "16k")
        )
    }
}

data class EqualizerPreset(
    val name: String,
    val gainsDb: List<Float>
) {
    companion object {
        val PRESETS = listOf(
            EqualizerPreset("Flat / Bypass", listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
            EqualizerPreset("Studio Master", listOf(1.5f, 1.0f, 0.5f, 0f, -0.5f, 0.5f, 1.0f, 1.8f, 2.2f, 2.5f)),
            EqualizerPreset("Acoustic Hi-Res", listOf(2.0f, 2.5f, 1.5f, 0f, 0.5f, 1.2f, 2.0f, 3.0f, 3.5f, 4.0f)),
            EqualizerPreset("Deep Analog Bass", listOf(6.0f, 5.0f, 3.5f, 1.5f, 0f, -1.0f, -0.5f, 0f, 1.0f, 1.5f)),
            EqualizerPreset("Vocal Presence", listOf(-2.0f, -1.0f, 0f, 1.5f, 3.0f, 3.5f, 2.5f, 1.0f, 0.5f, 0f)),
            EqualizerPreset("Rock Punch", listOf(4.5f, 3.5f, 2.0f, -0.5f, -1.5f, 0f, 2.0f, 3.5f, 4.0f, 4.5f))
        )
    }
}
