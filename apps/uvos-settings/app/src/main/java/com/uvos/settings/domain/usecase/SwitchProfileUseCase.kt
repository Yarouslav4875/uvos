package com.uvos.settings.domain.usecase

import com.uvos.settings.domain.model.SystemProfile
import com.uvos.settings.domain.repository.ProfileRepository

class SwitchProfileUseCase(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(profile: SystemProfile): Result<Unit> {
        return profileRepository.applyProfile(profile)
    }
}
