package com.uvos.downloader.domain.model

import java.util.Locale

data class NetworkSpeedMetric(
    val bytesPerSecond: Double = 0.0,
    val peakBytesPerSecond: Double = 0.0,
    val averageBytesPerSecond: Double = 0.0,
    val activeConnections: Int = 0,
    val isWifi: Boolean = true
) {
    val speedKb: Double
        get() = bytesPerSecond / 1024.0

    val speedMb: Double
        get() = bytesPerSecond / (1024.0 * 1024.0)

    fun formattedSpeed(): String {
        return when {
            speedMb >= 1.0 -> String.format(Locale.US, "%.2f МБ/с", speedMb)
            speedKb >= 1.0 -> String.format(Locale.US, "%.1f КБ/с", speedKb)
            else -> String.format(Locale.US, "%.0f Б/с", bytesPerSecond)
        }
    }

    fun formattedPeak(): String {
        val peakMb = peakBytesPerSecond / (1024.0 * 1024.0)
        val peakKb = peakBytesPerSecond / 1024.0
        return when {
            peakMb >= 1.0 -> String.format(Locale.US, "%.1f МБ/с", peakMb)
            peakKb >= 1.0 -> String.format(Locale.US, "%.0f КБ/с", peakKb)
            else -> String.format(Locale.US, "%.0f Б/с", peakBytesPerSecond)
        }
    }
}
