package com.uvos.settings.domain.model

data class CpuCoreMetrics(
    val coreId: Int,
    val frequencyMhz: Int,
    val isGoldCluster: Boolean
)

data class HardwareTelemetrySnapshot(
    val batteryVoltageMv: Int = 0,
    val batteryCurrentMa: Int = 0,
    val batteryTempC: Float = 0f,
    val socTempC: Float = 0f,
    val totalCpuUsagePercent: Int = 0,
    val cores: List<CpuCoreMetrics> = emptyList()
)
