package com.uvos.dashboard.repository

import com.uvos.dashboard.model.*

/**
 * Репозиторий правил и журнала UVOS (без внешних зависимостей).
 */
interface RuleRepository {
    fun getActiveProfile(): ProfileType
    fun setActiveProfile(profile: ProfileType)

    fun getAllApps(): List<AppRule>
    fun getAppRule(packageName: String): AppRule?

    fun setChannelMode(packageName: String, channel: DataChannel, mode: ChannelMode)
    fun updateNetworkConfig(packageName: String, config: NetworkConfig)

    fun getTotalOpenChannels(): Int
    fun getBlockedAttemptsCountToday(): Int
    fun getAttempts(): List<AccessAttemptEvent>
    fun addAttempt(event: AccessAttemptEvent)
    fun clearAttempts()
    fun markAllAttemptsViewed()
}
