package com.uvos.settings.domain.usecase

import com.uvos.settings.domain.repository.SystemSettingsRepository

class SetDisplayParamUseCase(
    private val systemSettingsRepository: SystemSettingsRepository
) {
    fun setBrightness(value: Int) {
        systemSettingsRepository.setBrightness(value)
    }

    fun setAutoBrightness(enabled: Boolean) {
        systemSettingsRepository.setAutoBrightness(enabled)
    }

    fun setRefreshRate(rateHz: Int) {
        systemSettingsRepository.setRefreshRate(rateHz)
    }

    fun setScreenTimeout(seconds: Int) {
        systemSettingsRepository.setScreenTimeout(seconds)
    }
}
