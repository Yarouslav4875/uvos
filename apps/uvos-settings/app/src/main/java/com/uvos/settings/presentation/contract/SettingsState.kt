package com.uvos.settings.presentation.contract

import com.uvos.settings.domain.model.AppFilterCategory
import com.uvos.settings.domain.model.AuditedAppInfo
import com.uvos.settings.domain.model.SystemFontFamily
import com.uvos.settings.domain.model.SystemProfile

/**
 * Единое неизменяемое состояние консоли настроек UVOS
 */
data class SettingsState(
    val currentProfile: SystemProfile = SystemProfile.CUSTOM,
    val isProfileSwitching: Boolean = false,

    // Аппаратная телеметрия Snapdragon 685 (SM6225)
    val batteryVoltageMv: Int = 4120,
    val batteryCurrentMa: Int = -320,
    val batteryTemperatureC: Float = 28.5f,
    val socTemperatureC: Float = 36.5f,
    val cpuTotalLoadPercent: Int = 18,
    val coreFrequenciesMhz: List<Int> = listOf(1024, 1024, 1024, 1024, 1804, 1804, 1804, 1804),

    // Параметры экрана и звука
    val screenBrightness: Int = 128,
    val isAutoBrightness: Boolean = false,
    val refreshRateHz: Int = 120,
    val screenTimeoutSeconds: Int = 30,
    val volumeRing: Int = 5,
    val volumeMedia: Int = 7,
    val volumeAlarm: Int = 6,
    val activeFont: SystemFontFamily = SystemFontFamily.JETBRAINS_MONO,

    // Аудит приложений
    val installedApps: List<AuditedAppInfo> = emptyList(),
    val appFilter: AppFilterCategory = AppFilterCategory.ALL,
    val isAuditScanning: Boolean = false,

    val errorMessage: String? = null
)
