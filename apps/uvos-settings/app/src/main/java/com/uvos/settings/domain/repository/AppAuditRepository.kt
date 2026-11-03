package com.uvos.settings.domain.repository

import com.uvos.settings.domain.model.AuditedAppInfo
import kotlinx.coroutines.flow.Flow

interface AppAuditRepository {
    fun getInstalledApps(): Flow<List<AuditedAppInfo>>
    suspend fun refreshApps()
    suspend fun toggleAppIsolation(packageName: String, isolate: Boolean): Result<Unit>
    suspend fun setAppNetworkAccess(packageName: String, allowNetwork: Boolean): Result<Unit>
    suspend fun terminateApp(packageName: String): Result<Unit>
}
