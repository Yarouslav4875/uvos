package com.uvos.settings.domain.usecase

import com.uvos.settings.domain.repository.AppAuditRepository

class SetAppNetworkAccessUseCase(
    private val appAuditRepository: AppAuditRepository
) {
    suspend operator fun invoke(packageName: String, allowNetwork: Boolean): Result<Unit> {
        return appAuditRepository.setAppNetworkAccess(packageName, allowNetwork)
    }
}
