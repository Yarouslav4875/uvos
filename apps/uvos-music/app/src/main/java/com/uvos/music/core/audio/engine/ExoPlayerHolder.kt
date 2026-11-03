package com.uvos.music.core.audio.engine

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.uvos.music.core.audio.effects.BassBoostController
import com.uvos.music.core.audio.effects.DynamicsLimiter
import com.uvos.music.core.audio.effects.EqualizerController
import com.uvos.music.domain.model.AudioTrack
import com.uvos.music.domain.model.StereoVuLevels
import kotlinx.coroutines.flow.StateFlow

/**
 * Encapsulates AndroidX Media3 ExoPlayer setup, True Hi-Res audio output pipeline,
 * and integration with Stereo VU meter audio processor.
 */
@OptIn(UnstableApi::class)
class ExoPlayerHolder(
    private val context: Context
) {
    val stereoVuAudioProcessor = StereoVuAudioProcessor()
    val equalizerController = EqualizerController()
    val bassBoostController = BassBoostController()
    val dynamicsLimiter = DynamicsLimiter { exoPlayer }

    val vuLevelsFlow: StateFlow<StereoVuLevels> = stereoVuAudioProcessor.vuLevelsFlow

    private val exoPlayer: ExoPlayer

    init {
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(true)
                    .setAudioProcessors(arrayOf(stereoVuAudioProcessor))
                    .build()
            }
        }.apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            setEnableAudioFloatOutput(true)
            setEnableAudioOffload(true)
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        exoPlayer = ExoPlayer.Builder(context, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        // Hook audio effects when audio session ID is assigned
        exoPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    equalizerController.attachAudioSession(audioSessionId)
                    bassBoostController.attachAudioSession(audioSessionId)
                }
            }
        })
    }

    fun getPlayer(): ExoPlayer = exoPlayer

    fun playTrack(track: AudioTrack) {
        val mediaItem = MediaItem.Builder()
            .setUri(track.contentUri)
            .setMediaId(track.id.toString())
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun playQueue(tracks: List<AudioTrack>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setUri(track.contentUri)
                .setMediaId(track.id.toString())
                .build()
        }
        exoPlayer.setMediaItems(mediaItems, startIndex, 0L)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun setVuPadSensitivity(padDb: Float) {
        stereoVuAudioProcessor.padSensitivityDb = padDb
    }

    fun release() {
        equalizerController.release()
        bassBoostController.release()
        stereoVuAudioProcessor.release()
        exoPlayer.release()
    }
}
