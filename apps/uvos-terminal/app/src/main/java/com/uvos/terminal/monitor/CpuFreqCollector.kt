package com.uvos.terminal.monitor

import android.util.Log
import java.io.File
import java.io.RandomAccessFile

object CpuFreqCollector {
    private const val TAG = "CpuFreqCollector"

    // Primary paths for Snapdragon 685 (4x Kryo 265 Silver LITTLE, 4x Kryo 265 Gold BIG)
    private const val CPU0_CUR_FREQ = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq"
    private const val CPU4_CUR_FREQ = "/sys/devices/system/cpu/cpu4/cpufreq/scaling_cur_freq"

    private var lastCpuUsage = 0f
    private var lastTotalTime = 0L
    private var lastIdleTime = 0L

    /**
     * Returns the maximum active cluster frequency in GHz (between 0.5 and 2.8 GHz).
     */
    fun readCpuFrequencyGhz(): Float {
        var freqKHz = 0L

        // Check Big cluster first (cpu4)
        val bigKHz = readFreqFromFile(CPU4_CUR_FREQ)
        val littleKHz = readFreqFromFile(CPU0_CUR_FREQ)

        freqKHz = maxOf(bigKHz, littleKHz)

        // If sysfs cpufreq is restricted/unavailable without root, probe all cores 0-7
        if (freqKHz <= 0) {
            for (i in 0..7) {
                val f = readFreqFromFile("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
                if (f > freqKHz) freqKHz = f
            }
        }

        // Fallback default for Snapdragon 685 if unreadable in unprivileged mode
        if (freqKHz <= 0) {
            return 1.90f
        }

        val ghz = freqKHz / 1_000_000f
        return ghz.coerceIn(0.5f, 2.8f)
    }

    /**
     * Reads instantaneous CPU load percentage from /proc/stat.
     */
    fun readCpuLoadPercentage(): Float {
        try {
            val statFile = File("/proc/stat")
            if (!statFile.canRead()) return 15f

            statFile.bufferedReader().use { reader ->
                val line = reader.readLine() ?: return lastCpuUsage
                if (line.startsWith("cpu ")) {
                    val parts = line.split("\\s+".toRegex()).drop(1).mapNotNull { it.toLongOrNull() }
                    if (parts.size >= 4) {
                        val idle = parts[3] + (if (parts.size > 4) parts[4] else 0)
                        val total = parts.sum()

                        val deltaTotal = total - lastTotalTime
                        val deltaIdle = idle - lastIdleTime

                        if (deltaTotal > 0) {
                            val usage = (1.0f - (deltaIdle.toFloat() / deltaTotal.toFloat())) * 100f
                            lastCpuUsage = usage.coerceIn(0f, 100f)
                        }

                        lastTotalTime = total
                        lastIdleTime = idle
                    }
                }
            }
        } catch (_: Throwable) {}
        return lastCpuUsage
    }

    private fun readFreqFromFile(path: String): Long {
        return try {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                file.readText().trim().toLongOrNull() ?: 0L
            } else {
                0L
            }
        } catch (_: Throwable) {
            0L
        }
    }
}
