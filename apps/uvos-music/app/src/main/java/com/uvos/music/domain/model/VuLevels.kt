package com.uvos.music.domain.model

/**
 * Real-time stereo VU meter levels for dual M4200 gauges.
 */
data class StereoVuLevels(
    val leftNormalized: Float = 0.0f,     // 0.0 (off) .. 1.0 (0 VU) .. 1.15 (+3 VU max)
    val rightNormalized: Float = 0.0f,
    val leftRmsDb: Float = -60.0f,        // dBFS level
    val rightRmsDb: Float = -60.0f,
    val leftPeakDb: Float = -60.0f,
    val rightPeakDb: Float = -60.0f,
    val isLeftClipping: Boolean = false,  // Red Overload LED lit when peak >= -0.5 dBFS
    val isRightClipping: Boolean = false
)
