package com.uvos.music.feature.equalizer

import androidx.lifecycle.ViewModel
import com.uvos.music.UvosMusicApp
import com.uvos.music.domain.model.EqualizerBand
import com.uvos.music.domain.model.EqualizerPreset
import com.uvos.music.domain.model.EqualizerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EqualizerViewModel : ViewModel() {

    private val playerHolder = UvosMusicApp.instance.playerHolder
    private val eqController = playerHolder.equalizerController
    private val bassController = playerHolder.bassBoostController
    private val dynamicsLimiter = playerHolder.dynamicsLimiter

    private val _state = MutableStateFlow(eqController.getCurrentState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        eqController.setEnabled(enabled)
        _state.value = _state.value.copy(isEnabled = enabled)
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        eqController.setBandGain(bandIndex, gainDb)
        _state.value = eqController.getCurrentState()
    }

    fun applyPreset(preset: EqualizerPreset) {
        eqController.setAllGains(preset.gainsDb)
        _state.value = eqController.getCurrentState()
    }

    fun setBassBoost(strength: Float) {
        bassController.setStrength(strength)
        _state.value = _state.value.copy(bassBoost = strength)
    }

    fun setWarmTube(enabled: Boolean) {
        bassController.setWarmTubeEnabled(enabled)
        _state.value = _state.value.copy(warmTube = enabled)
    }

    fun setPreGain(gainDb: Float) {
        dynamicsLimiter.setPreGain(gainDb)
        _state.value = _state.value.copy(preGainDb = gainDb)
    }

    fun setBalance(balance: Float) {
        dynamicsLimiter.setBalance(balance)
        _state.value = _state.value.copy(balance = balance)
    }

    fun resetToFlat() {
        applyPreset(EqualizerPreset.PRESETS.first())
        setBassBoost(0f)
        setWarmTube(false)
        setPreGain(0f)
        setBalance(0f)
    }
}
