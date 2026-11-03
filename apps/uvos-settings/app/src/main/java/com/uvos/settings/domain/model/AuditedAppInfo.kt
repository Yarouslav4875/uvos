package com.uvos.settings.domain.model

data class AuditedAppInfo(
    val packageName: String,
    val appName: String,
    val uid: Int,
    val isSystemApp: Boolean,
    val hasInternetPermission: Boolean,
    val isIptablesBlocked: Boolean,
    val isolationBadge: AppIsolationBadge,
    val activeChannelsCount: Int = 0
)
