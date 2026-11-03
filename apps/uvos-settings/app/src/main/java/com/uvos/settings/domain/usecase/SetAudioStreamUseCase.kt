package com.uvos.settings.domain.usecase

import com.uvos.settings.domain.model.AudioStreamType
import com.uvos.settings.domain.repository.SystemSettingsRepository

class SetAudioStreamUseCase(
    private val systemSettingsRepository: SystemSettingsRepository
) {
    operator fun invoke(streamType: AudioStreamType, level: Int) {
        systemSettingsRepository.setStreamVolume(streamType, level)
    }
}
