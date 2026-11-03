package com.uvos.settings.presentation.contract

import com.uvos.settings.domain.model.AppFilterCategory
import com.uvos.settings.domain.model.AudioStreamType
import com.uvos.settings.domain.model.SystemFontFamily
import com.uvos.settings.domain.model.SystemProfile

/**
 * Пользовательские намерения (Intents)
 */
sealed interface SettingsIntent {
    data class SelectProfile(val profile: SystemProfile) : SettingsIntent
    data class ChangeBrightness(val value: Int) : SettingsIntent
    data class ToggleAutoBrightness(val enabled: Boolean) : SettingsIntent
    data class ChangeRefreshRate(val rateHz: Int) : SettingsIntent
    data class ChangeScreenTimeout(val seconds: Int) : SettingsIntent
    data class ChangeVolume(val streamType: AudioStreamType, val level: Int) : SettingsIntent
    data class SelectFont(val font: SystemFontFamily) : SettingsIntent

    // Действия аудита
    data class FilterApps(val category: AppFilterCategory) : SettingsIntent
    data class ToggleAppIsolation(val packageName: String, val isolate: Boolean) : SettingsIntent
    data class SetAppNetworkAccess(val packageName: String, val allowNetwork: Boolean) : SettingsIntent
    data class TerminateApp(val packageName: String) : SettingsIntent
    data object RefreshAppsList : SettingsIntent
}
