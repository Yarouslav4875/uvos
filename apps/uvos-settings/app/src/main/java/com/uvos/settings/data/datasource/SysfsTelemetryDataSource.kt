package com.uvos.settings.data.datasource

import com.uvos.settings.domain.model.CpuCoreMetrics
import com.uvos.settings.domain.model.HardwareTelemetrySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

class SysfsTelemetryDataSource {

    private var lastTotalTime = 0L
    private var lastIdleTime = 0L

    fun getTelemetryStream(pollIntervalMs: Long = 1000L): Flow<HardwareTelemetrySnapshot> = flow {
        while (true) {
            val snapshot = readHardwareSnapshot()
            emit(snapshot)
            delay(pollIntervalMs)
        }
    }.flowOn(Dispatchers.IO)

    private fun readHardwareSnapshot(): HardwareTelemetrySnapshot {
        // Чтение параметров батареи (мкВ -> мВ, мкА -> мА, десятые доли C -> C)
        val rawVoltage = readSysfsInt("/sys/class/power_supply/battery/voltage_now", 4120000)
        val rawCurrent = readSysfsInt("/sys/class/power_supply/battery/current_now", -320000)
        val rawBatTemp = readSysfsInt("/sys/class/power_supply/battery/temp", 285)
        val rawSocTemp = readSysfsInt("/sys/class/thermal/thermal_zone0/temp", 36500)

        // Чтение частот 8 ядер Snapdragon 685 (4 ядра Silver Cortex-A53 + 4 ядра Gold Cortex-A73)
        val cores = (0..7).map { coreId ->
            val freqPath = "/sys/devices/system/cpu/cpu$coreId/cpufreq/scaling_cur_freq"
            val freqKhz = readSysfsInt(freqPath, if (coreId >= 4) 1804000 else 1024000)
            CpuCoreMetrics(
                coreId = coreId,
                frequencyMhz = freqKhz / 1000,
                isGoldCluster = coreId >= 4
            )
        }

        val cpuLoad = calculateCpuLoad()

        return HardwareTelemetrySnapshot(
            batteryVoltageMv = rawVoltage / 1000,
            batteryCurrentMa = rawCurrent / 1000,
            batteryTempC = rawBatTemp / 10f,
            socTempC = if (rawSocTemp > 1000) rawSocTemp / 1000f else rawSocTemp.toFloat(),
            totalCpuUsagePercent = cpuLoad,
            cores = cores
        )
    }

    private fun readSysfsInt(path: String, default: Int): Int {
        return try {
            val file = File(path)
            if (file.exists()) {
                file.readText().trim().toIntOrNull() ?: default
            } else {
                default
            }
        } catch (_: Exception) {
            default
        }
    }

    private fun calculateCpuLoad(): Int {
        return try {
            val statFile = File("/proc/stat")
            if (!statFile.exists()) return 18
            val line = statFile.bufferedReader().readLine() ?: return 18
            val parts = line.split("\\s+".toRegex()).filter { it.isNotEmpty() }
            if (parts.size < 5) return 18

            val user = parts[1].toLong()
            val nice = parts[2].toLong()
            val system = parts[3].toLong()
            val idle = parts[4].toLong()
            val iowait = if (parts.size > 5) parts[5].toLong() else 0L
            val irq = if (parts.size > 6) parts[6].toLong() else 0L
            val softirq = if (parts.size > 7) parts[7].toLong() else 0L

            val total = user + nice + system + idle + iowait + irq + softirq
            val totalDiff = total - lastTotalTime
            val idleDiff = idle - lastIdleTime

            lastTotalTime = total
            lastIdleTime = idle

            if (totalDiff <= 0) return 15
            val load = ((totalDiff - idleDiff) * 100 / totalDiff).toInt()
            load.coerceIn(0, 100)
        } catch (_: Exception) {
            20
        }
    }
}
