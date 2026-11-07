package com.uvos.downloader.domain.usecase

import com.uvos.downloader.domain.model.NetworkSpeedMetric
import kotlin.math.max

class CalculateSpeedMetricsUseCase {
    private var lastTimestamp: Long = 0L
    private var lastBytesCount: Long = 0L
    private var peakBps: Double = 0.0
    private var smoothedBps: Double = 0.0

    @Synchronized
    fun reset() {
        lastTimestamp = 0L
        lastBytesCount = 0L
        peakBps = 0.0
        smoothedBps = 0.0
    }

    @Synchronized
    operator fun invoke(
        currentTotalBytes: Long,
        activeConnections: Int,
        isWifi: Boolean
    ): NetworkSpeedMetric {
        val now = System.currentTimeMillis()
        if (lastTimestamp == 0L || currentTotalBytes < lastBytesCount) {
            lastTimestamp = now
            lastBytesCount = currentTotalBytes
            return NetworkSpeedMetric(
                bytesPerSecond = 0.0,
                peakBytesPerSecond = peakBps,
                averageBytesPerSecond = smoothedBps,
                activeConnections = activeConnections,
                isWifi = isWifi
            )
        }

        val elapsedMs = now - lastTimestamp
        if (elapsedMs < 150) {
            return NetworkSpeedMetric(
                bytesPerSecond = smoothedBps,
                peakBytesPerSecond = peakBps,
                averageBytesPerSecond = smoothedBps,
                activeConnections = activeConnections,
                isWifi = isWifi
            )
        }

        val deltaBytes = currentTotalBytes - lastBytesCount
        val instantBps = (deltaBytes.toDouble() / (elapsedMs.toDouble() / 1000.0)).coerceAtLeast(0.0)

        // Exponential smoothing (alpha = 0.3)
        smoothedBps = if (smoothedBps == 0.0) instantBps else (0.7 * smoothedBps + 0.3 * instantBps)
        peakBps = max(peakBps, smoothedBps)

        lastTimestamp = now
        lastBytesCount = currentTotalBytes

        return NetworkSpeedMetric(
            bytesPerSecond = smoothedBps,
            peakBytesPerSecond = peakBps,
            averageBytesPerSecond = smoothedBps,
            activeConnections = activeConnections,
            isWifi = isWifi
        )
    }
}
