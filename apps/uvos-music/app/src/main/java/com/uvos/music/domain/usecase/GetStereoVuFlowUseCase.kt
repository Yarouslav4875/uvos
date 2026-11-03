package com.uvos.music.domain.usecase

import com.uvos.music.core.audio.engine.ExoPlayerHolder
import com.uvos.music.domain.model.StereoVuLevels
import kotlinx.coroutines.flow.StateFlow

class GetStereoVuFlowUseCase(
    private val playerHolder: ExoPlayerHolder
) {
    fun execute(): StateFlow<StereoVuLevels> {
        return playerHolder.vuLevelsFlow
    }

    fun setPadSensitivity(padDb: Float) {
        playerHolder.setVuPadSensitivity(padDb)
    }
}
