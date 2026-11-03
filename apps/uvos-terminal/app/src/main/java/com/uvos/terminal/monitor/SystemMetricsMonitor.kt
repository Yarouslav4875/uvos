package com.uvos.terminal.monitor

import com.uvos.terminal.root.RootShellManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TerminalSystemMetrics(
    val cpuFrequencyGhz: Float = 1.90f,
    val cpuLoadPercentage: Float = 10f,
    val ramUsedGb: Float = 3.5f,
    val ramTotalGb: Float = 8.0f,
    val ramPercentage: Float = 43.75f,
    val isRoot: Boolean = false,
    val isPtyActive: Boolean = false,
    val isRecording: Boolean = false
)

class SystemMetricsMonitor(
    private val scope: CoroutineScope
) {
    private val _metrics = MutableStateFlow(TerminalSystemMetrics())
    val metrics: StateFlow<TerminalSystemMetrics> = _metrics.asStateFlow()

    private var pollingJob: Job? = null

    fun start(isPtyFlow: StateFlow<Boolean>, isRecFlow: StateFlow<Boolean>) {
        pollingJob?.cancel()
        pollingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val cpuGhz = CpuFreqCollector.readCpuFrequencyGhz()
                val cpuLoad = CpuFreqCollector.readCpuLoadPercentage()
                val ram = MemInfoCollector.readRamMetrics()
                val isRoot = RootShellManager.isRoot()

                _metrics.value = TerminalSystemMetrics(
                    cpuFrequencyGhz = cpuGhz,
                    cpuLoadPercentage = cpuLoad,
                    ramUsedGb = ram.usedGb,
                    ramTotalGb = ram.totalGb,
                    ramPercentage = ram.usedPercentage,
                    isRoot = isRoot,
                    isPtyActive = isPtyFlow.value,
                    isRecording = isRecFlow.value
                )

                delay(500) // 2 Hz sampling rate
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }
}
