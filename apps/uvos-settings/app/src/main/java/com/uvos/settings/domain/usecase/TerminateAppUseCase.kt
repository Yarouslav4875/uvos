package com.uvos.settings.domain.usecase

import com.uvos.settings.domain.repository.AppAuditRepository

class TerminateAppUseCase(
    private val appAuditRepository: AppAuditRepository
) {
    suspend operator fun invoke(packageName: String): Result<Unit> {
        return appAuditRepository.terminateApp(packageName)
    }
}
