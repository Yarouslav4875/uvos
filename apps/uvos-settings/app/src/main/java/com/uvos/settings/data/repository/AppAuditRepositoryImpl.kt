package com.uvos.settings.data.repository

import com.uvos.settings.data.datasource.AppAuditDataSource
import com.uvos.settings.domain.model.AuditedAppInfo
import com.uvos.settings.domain.repository.AppAuditRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppAuditRepositoryImpl(
    private val appAuditDataSource: AppAuditDataSource
) : AppAuditRepository {

    private val _appsFlow = MutableStateFlow<List<AuditedAppInfo>>(emptyList())

    override fun getInstalledApps(): Flow<List<AuditedAppInfo>> = _appsFlow.asStateFlow()

    override suspend fun refreshApps() {
        val apps = appAuditDataSource.scanInstalledApps()
        _appsFlow.value = apps
    }

    override suspend fun toggleAppIsolation(packageName: String, isolate: Boolean): Result<Unit> {
        val result = appAuditDataSource.setAppIsolation(packageName, isolate)
        if (result.isSuccess) {
            refreshApps()
        }
        return result
    }

    override suspend fun setAppNetworkAccess(packageName: String, allowNetwork: Boolean): Result<Unit> {
        val result = appAuditDataSource.setAppNetworkAccess(packageName, allowNetwork)
        if (result.isSuccess) {
            refreshApps()
        }
        return result
    }

    override suspend fun terminateApp(packageName: String): Result<Unit> {
        return appAuditDataSource.terminateApp(packageName)
    }
}
