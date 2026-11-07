package com.uvos.security.domain.model

data class AppIsolationInfo(
    val packageName: String,
    val appName: String,
    val uid: Int,
    val isNetworkBlocked: Boolean,
    val isSensorsSpoofed: Boolean,
    val isContactsStubbed: Boolean
)

data class SecurityStatus(
    val threatLevelScore: Float, // 0.0f..1.0f
    val isNetfilterActive: Boolean,
    val isRootActive: Boolean,
    val blockedConnectionsCount: Int,
    val isolatedAppsCount: Int
)
