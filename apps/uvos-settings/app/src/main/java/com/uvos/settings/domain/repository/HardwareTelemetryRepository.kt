package com.uvos.settings.domain.repository

import com.uvos.settings.domain.model.HardwareTelemetrySnapshot
import kotlinx.coroutines.flow.Flow

interface HardwareTelemetryRepository {
    fun observeTelemetry(pollIntervalMs: Long = 1000L): Flow<HardwareTelemetrySnapshot>
}
