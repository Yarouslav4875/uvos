package com.uvos.settings.domain.repository

import com.uvos.settings.domain.model.SystemProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getCurrentProfile(): Flow<SystemProfile>
    suspend fun applyProfile(profile: SystemProfile): Result<Unit>
}
