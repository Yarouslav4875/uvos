package com.uvos.settings.data.repository

import com.uvos.settings.data.datasource.SettingsProviderDataSource
import com.uvos.settings.domain.model.AudioStreamType
import com.uvos.settings.domain.model.SystemFontFamily
import com.uvos.settings.domain.repository.SystemSettingsRepository

class SystemSettingsRepositoryImpl(
    private val settingsProviderDataSource: SettingsProviderDataSource
) : SystemSettingsRepository {

    private var activeFont: SystemFontFamily = SystemFontFamily.JETBRAINS_MONO

    override fun getBrightness(): Int = settingsProviderDataSource.getBrightness()

    override fun setBrightness(value: Int) = settingsProviderDataSource.setBrightness(value)

    override fun isAutoBrightness(): Boolean = settingsProviderDataSource.isAutoBrightness()

    override fun setAutoBrightness(enabled: Boolean) = settingsProviderDataSource.setAutoBrightness(enabled)

    override fun getRefreshRate(): Int = settingsProviderDataSource.getRefreshRate()

    override fun setRefreshRate(rateHz: Int) = settingsProviderDataSource.setRefreshRate(rateHz)

    override fun getScreenTimeout(): Int = settingsProviderDataSource.getScreenTimeout()

    override fun setScreenTimeout(seconds: Int) = settingsProviderDataSource.setScreenTimeout(seconds)

    override fun getStreamVolume(streamType: AudioStreamType): Int =
        settingsProviderDataSource.getStreamVolume(streamType)

    override fun setStreamVolume(streamType: AudioStreamType, level: Int) =
        settingsProviderDataSource.setStreamVolume(streamType, level)

    override fun getActiveFont(): SystemFontFamily = activeFont

    override fun setActiveFont(font: SystemFontFamily) {
        activeFont = font
        // Adjust font scaling or system font parameter
        when (font) {
            SystemFontFamily.JETBRAINS_MONO -> settingsProviderDataSource.setFontScale(1.0f)
            SystemFontFamily.ROBOTO_MONO -> settingsProviderDataSource.setFontScale(1.0f)
            SystemFontFamily.TERMINAL_RETRO -> settingsProviderDataSource.setFontScale(1.1f)
        }
    }
}
