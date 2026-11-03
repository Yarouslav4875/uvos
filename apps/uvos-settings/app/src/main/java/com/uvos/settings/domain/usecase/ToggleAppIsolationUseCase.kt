package com.uvos.settings.domain.usecase

import com.uvos.settings.domain.repository.AppAuditRepository

class ToggleAppIsolationUseCase(
    private val appAuditRepository: AppAuditRepository
) {
    suspend operator fun invoke(packageName: String, isolate: Boolean): Result<Unit> {
        return appAuditRepository.toggleAppIsolation(packageName, isolate)
    }
}
