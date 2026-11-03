package com.uvos.settings.domain.repository

import com.uvos.settings.domain.model.AudioStreamType
import com.uvos.settings.domain.model.SystemFontFamily

interface SystemSettingsRepository {
    fun getBrightness(): Int
    fun setBrightness(value: Int)
    fun isAutoBrightness(): Boolean
    fun setAutoBrightness(enabled: Boolean)
    fun getRefreshRate(): Int
    fun setRefreshRate(rateHz: Int)
    fun getScreenTimeout(): Int
    fun setScreenTimeout(seconds: Int)
    fun getStreamVolume(streamType: AudioStreamType): Int
    fun setStreamVolume(streamType: AudioStreamType, level: Int)
    fun getActiveFont(): SystemFontFamily
    fun setActiveFont(font: SystemFontFamily)
}
