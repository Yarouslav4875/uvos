package com.uvos.settings.presentation.contract

import com.uvos.settings.domain.model.HapticStrength

/**
 * Однократные побочные эффекты (Side Effects)
 */
sealed interface SettingsEffect {
    data class ShowToast(val message: String) : SettingsEffect
    data class PlayToggleHaptic(val strength: HapticStrength) : SettingsEffect
    data class RequestSystemPermission(val permission: String) : SettingsEffect
}
