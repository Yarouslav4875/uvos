package com.uvos.settings.domain.usecase

import com.uvos.settings.domain.model.HardwareTelemetrySnapshot
import com.uvos.settings.domain.repository.HardwareTelemetryRepository
import kotlinx.coroutines.flow.Flow

class ObserveTelemetryUseCase(
    private val telemetryRepository: HardwareTelemetryRepository
) {
    operator fun invoke(pollIntervalMs: Long = 1000L): Flow<HardwareTelemetrySnapshot> {
        return telemetryRepository.observeTelemetry(pollIntervalMs)
    }
}
