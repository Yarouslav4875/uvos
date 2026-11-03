package com.uvos.terminal.monitor

import java.io.File

data class RamMetrics(
    val totalGb: Float,
    val usedGb: Float,
    val availableGb: Float,
    val usedPercentage: Float
)

object MemInfoCollector {
    private const val MEMINFO_PATH = "/proc/meminfo"

    fun readRamMetrics(): RamMetrics {
        var memTotalKb = 0L
        var memAvailableKb = 0L

        try {
            val file = File(MEMINFO_PATH)
            if (file.exists() && file.canRead()) {
                file.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        if (line.startsWith("MemTotal:")) {
                            memTotalKb = parseKb(line)
                        } else if (line.startsWith("MemAvailable:")) {
                            memAvailableKb = parseKb(line)
                        }
                    }
                }
            }
        } catch (_: Throwable) {}

        // Fallback if unavailable
        if (memTotalKb <= 0) {
            memTotalKb = 8 * 1024 * 1024 // 8GB default for Redmi Note 13 4G
        }
        if (memAvailableKb <= 0) {
            memAvailableKb = 4 * 1024 * 1024
        }

        val memUsedKb = (memTotalKb - memAvailableKb).coerceAtLeast(0L)
        val totalGb = memTotalKb / (1024f * 1024f)
        val usedGb = memUsedKb / (1024f * 1024f)
        val availableGb = memAvailableKb / (1024f * 1024f)
        val usedPercentage = ((usedGb / totalGb) * 100f).coerceIn(0f, 100f)

        return RamMetrics(
            totalGb = totalGb,
            usedGb = usedGb,
            availableGb = availableGb,
            usedPercentage = usedPercentage
        )
    }

    private fun parseKb(line: String): Long {
        val parts = line.split("\\s+".toRegex())
        return if (parts.size >= 2) {
            parts[1].toLongOrNull() ?: 0L
        } else {
            0L
        }
    }
}
