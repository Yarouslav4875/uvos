package com.uvos.music.domain.usecase

import com.uvos.music.core.audio.engine.ExoPlayerHolder
import com.uvos.music.domain.model.AudioTrack

class PlayTrackUseCase(
    private val playerHolder: ExoPlayerHolder
) {
    fun execute(track: AudioTrack) {
        playerHolder.playTrack(track)
    }

    fun playQueue(tracks: List<AudioTrack>, startIndex: Int) {
        playerHolder.playQueue(tracks, startIndex)
    }
}
