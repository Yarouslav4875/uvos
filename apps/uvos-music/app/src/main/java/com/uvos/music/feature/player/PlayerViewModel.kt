package com.uvos.music.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.uvos.music.UvosMusicApp
import com.uvos.music.domain.model.AudioFormatInfo
import com.uvos.music.domain.model.AudioTrack
import com.uvos.music.domain.model.StereoVuLevels
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val currentTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val vuPadDb: Float = 0.0f,
    val queue: List<AudioTrack> = emptyList(),
    val currentIndex: Int = 0
)

class PlayerViewModel : ViewModel() {

    private val playerHolder = UvosMusicApp.instance.playerHolder
    private val exoPlayer = playerHolder.getPlayer()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val vuLevelsFlow: StateFlow<StereoVuLevels> = playerHolder.vuLevelsFlow

    private var progressTickerJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            if (isPlaying) {
                startProgressTicker()
            } else {
                stopProgressTicker()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _uiState.value = _uiState.value.copy(
                isPlaying = exoPlayer.isPlaying,
                durationMs = exoPlayer.duration.coerceAtLeast(0L),
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            )
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val mediaId = mediaItem?.mediaId?.toLongOrNull()
            val currentQueue = _uiState.value.queue
            val foundTrack = currentQueue.find { it.id == mediaId }
            val index = if (foundTrack != null) currentQueue.indexOf(foundTrack) else exoPlayer.currentMediaItemIndex

            _uiState.value = _uiState.value.copy(
                currentTrack = foundTrack ?: _uiState.value.currentTrack,
                currentIndex = index,
                durationMs = exoPlayer.duration.coerceAtLeast(0L),
                currentPositionMs = 0L
            )
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _uiState.value = _uiState.value.copy(isShuffleEnabled = shuffleModeEnabled)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _uiState.value = _uiState.value.copy(repeatMode = repeatMode)
        }
    }

    init {
        exoPlayer.addListener(playerListener)
        _uiState.value = _uiState.value.copy(
            isPlaying = exoPlayer.isPlaying,
            isShuffleEnabled = exoPlayer.shuffleModeEnabled,
            repeatMode = exoPlayer.repeatMode,
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
            durationMs = exoPlayer.duration.coerceAtLeast(0L),
            vuPadDb = playerHolder.stereoVuAudioProcessor.padSensitivityDb
        )
        if (exoPlayer.isPlaying) {
            startProgressTicker()
        }
    }

    fun playQueue(tracks: List<AudioTrack>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        _uiState.value = _uiState.value.copy(
            queue = tracks,
            currentIndex = startIndex,
            currentTrack = tracks.getOrNull(startIndex)
        )
        playerHolder.playQueue(tracks, startIndex)
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_IDLE && _uiState.value.queue.isNotEmpty()) {
                playQueue(_uiState.value.queue, _uiState.value.currentIndex)
            } else {
                exoPlayer.play()
            }
        }
    }

    fun skipToNext() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        }
    }

    fun skipToPrevious() {
        if (exoPlayer.currentPosition > 3000L) {
            exoPlayer.seekTo(0L)
        } else if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        } else {
            exoPlayer.seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _uiState.value = _uiState.value.copy(currentPositionMs = positionMs)
    }

    fun toggleShuffle() {
        val newShuffle = !exoPlayer.shuffleModeEnabled
        exoPlayer.shuffleModeEnabled = newShuffle
        _uiState.value = _uiState.value.copy(isShuffleEnabled = newShuffle)
    }

    fun toggleRepeat() {
        val nextMode = when (exoPlayer.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        exoPlayer.repeatMode = nextMode
        _uiState.value = _uiState.value.copy(repeatMode = nextMode)
    }

    fun toggleVuPad() {
        val newPad = if (_uiState.value.vuPadDb > 0f) 0.0f else 6.0f
        playerHolder.setVuPadSensitivity(newPad)
        _uiState.value = _uiState.value.copy(vuPadDb = newPad)
    }

    private fun startProgressTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = viewModelScope.launch {
            while (isActive) {
                delay(300)
                if (exoPlayer.isPlaying) {
                    _uiState.value = _uiState.value.copy(
                        currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                        durationMs = exoPlayer.duration.coerceAtLeast(0L)
                    )
                }
            }
        }
    }

    private fun stopProgressTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressTicker()
        exoPlayer.removeListener(playerListener)
    }
}
