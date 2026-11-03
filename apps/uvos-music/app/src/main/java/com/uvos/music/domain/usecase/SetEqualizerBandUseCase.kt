package com.uvos.music.domain.usecase

import com.uvos.music.core.audio.effects.EqualizerController
import com.uvos.music.domain.model.EqualizerPreset

class SetEqualizerBandUseCase(
    private val equalizerController: EqualizerController
) {
    fun setBandGain(bandIndex: Int, gainDb: Float) {
        equalizerController.setBandGain(bandIndex, gainDb)
    }

    fun applyPreset(preset: EqualizerPreset) {
        equalizerController.setAllGains(preset.gainsDb)
    }

    fun setEnabled(enabled: Boolean) {
        equalizerController.setEnabled(enabled)
    }
}
