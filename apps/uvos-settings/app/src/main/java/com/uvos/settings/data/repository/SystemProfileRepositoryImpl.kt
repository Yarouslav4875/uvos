package com.uvos.settings.data.repository

import com.uvos.settings.data.datasource.RadioControllerDataSource
import com.uvos.settings.domain.model.SystemProfile
import com.uvos.settings.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SystemProfileRepositoryImpl(
    private val radioControllerDataSource: RadioControllerDataSource
) : ProfileRepository {

    private val _currentProfile = MutableStateFlow(SystemProfile.CUSTOM)

    override fun getCurrentProfile(): Flow<SystemProfile> = _currentProfile.asStateFlow()

    override suspend fun applyProfile(profile: SystemProfile): Result<Unit> {
        val result = radioControllerDataSource.applyProfile(profile)
        if (result.isSuccess) {
            _currentProfile.value = profile
        }
        return result
    }
}
