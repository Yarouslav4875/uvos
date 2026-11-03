package com.uvos.settings.data.repository

import com.uvos.settings.data.datasource.SysfsTelemetryDataSource
import com.uvos.settings.domain.model.HardwareTelemetrySnapshot
import com.uvos.settings.domain.repository.HardwareTelemetryRepository
import kotlinx.coroutines.flow.Flow

class HardwareTelemetryRepositoryImpl(
    private val sysfsTelemetryDataSource: SysfsTelemetryDataSource
) : HardwareTelemetryRepository {

    override fun observeTelemetry(pollIntervalMs: Long): Flow<HardwareTelemetrySnapshot> {
        return sysfsTelemetryDataSource.getTelemetryStream(pollIntervalMs)
    }
}
