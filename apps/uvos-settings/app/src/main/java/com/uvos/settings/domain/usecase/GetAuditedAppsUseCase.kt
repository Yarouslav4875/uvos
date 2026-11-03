package com.uvos.settings.domain.usecase

import com.uvos.settings.domain.model.AppFilterCategory
import com.uvos.settings.domain.model.AppIsolationBadge
import com.uvos.settings.domain.model.AuditedAppInfo
import com.uvos.settings.domain.repository.AppAuditRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetAuditedAppsUseCase(
    private val appAuditRepository: AppAuditRepository
) {
    operator fun invoke(filter: AppFilterCategory = AppFilterCategory.ALL): Flow<List<AuditedAppInfo>> {
        return appAuditRepository.getInstalledApps().map { apps ->
            when (filter) {
                AppFilterCategory.ALL -> apps
                AppFilterCategory.SYSTEM -> apps.filter { it.isSystemApp }
                AppFilterCategory.ISOLATED -> apps.filter { it.isolationBadge == AppIsolationBadge.ISOLATED }
                AppFilterCategory.USER -> apps.filter { !it.isSystemApp }
                AppFilterCategory.THREATS -> apps.filter { it.isolationBadge == AppIsolationBadge.EXPOSED }
            }
        }
    }

    suspend fun refresh() {
        appAuditRepository.refreshApps()
    }
}
