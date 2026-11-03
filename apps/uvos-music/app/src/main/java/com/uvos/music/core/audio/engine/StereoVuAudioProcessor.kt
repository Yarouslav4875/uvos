package com.uvos.music.core.audio.engine

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import com.uvos.music.core.audio.ballistics.RmsCalculator
import com.uvos.music.core.audio.ballistics.VuMeterBallistics
import com.uvos.music.domain.model.StereoVuLevels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Media3 AudioProcessor that passively intercepts PCM stereo audio streams (Tee processor)
 * without transcoding or degrading the bit-perfect stream, and computes real-time stereo
 * levels with ANSI C16.5-1954 ballistics for dual M4200 VU-meters.
 */
class StereoVuAudioProcessor(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : BaseAudioProcessor() {

    private val leftBallistics = VuMeterBallistics()
    private val rightBallistics = VuMeterBallistics()

    private val _vuLevelsFlow = MutableStateFlow(StereoVuLevels())
    val vuLevelsFlow: StateFlow<StereoVuLevels> = _vuLevelsFlow.asStateFlow()

    @Volatile
    private var targetLeftNormalized = 0f

    @Volatile
    private var targetRightNormalized = 0f

    @Volatile
    private var lastLeftRmsDb = -60f

    @Volatile
    private var lastRightRmsDb = -60f

    @Volatile
    private var lastLeftPeakDb = -60f

    @Volatile
    private var lastRightPeakDb = -60f

    @Volatile
    private var leftClipTimestamp = 0L

    @Volatile
    private var rightClipTimestamp = 0L

    @Volatile
    var padSensitivityDb: Float = 0.0f // 0dB or +6dB

    private var animationJob: Job? = null

    init {
        startBallisticsTicker()
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // We pass through the audio unchanged
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val inputFormat = inputAudioFormat
        val channelCount = inputFormat.channelCount
        val encoding = inputFormat.encoding

        // Process audio metrics from PCM data
        if (channelCount >= 2 && (encoding == C.ENCODING_PCM_16BIT || encoding == C.ENCODING_PCM_FLOAT)) {
            val duplicate = inputBuffer.duplicate()
            duplicate.order(ByteOrder.LITTLE_ENDIAN)

            if (encoding == C.ENCODING_PCM_16BIT) {
                val left = RmsCalculator.calculateMetricsFromPcm16(duplicate.duplicate(), 0, channelCount, padSensitivityDb)
                val right = RmsCalculator.calculateMetricsFromPcm16(duplicate.duplicate(), 1, channelCount, padSensitivityDb)

                targetLeftNormalized = left.normalizedVu
                targetRightNormalized = right.normalizedVu
                lastLeftRmsDb = left.rmsDb
                lastRightRmsDb = right.rmsDb
                lastLeftPeakDb = left.peakDb
                lastRightPeakDb = right.peakDb

                val now = System.currentTimeMillis()
                if (left.isClipping) leftClipTimestamp = now
                if (right.isClipping) rightClipTimestamp = now
            } else if (encoding == C.ENCODING_PCM_FLOAT) {
                val floatBuffer = duplicate.asFloatBuffer()
                val totalFloats = floatBuffer.remaining()
                val frames = totalFloats / channelCount
                if (frames > 0) {
                    val temp = FloatArray(totalFloats)
                    floatBuffer.get(temp)
                    val left = RmsCalculator.calculateMetricsFromFloat(temp, 0, channelCount, frames, padSensitivityDb)
                    val right = RmsCalculator.calculateMetricsFromFloat(temp, 1, channelCount, frames, padSensitivityDb)

                    targetLeftNormalized = left.normalizedVu
                    targetRightNormalized = right.normalizedVu
                    lastLeftRmsDb = left.rmsDb
                    lastRightRmsDb = right.rmsDb
                    lastLeftPeakDb = left.peakDb
                    lastRightPeakDb = right.peakDb

                    val now = System.currentTimeMillis()
                    if (left.isClipping) leftClipTimestamp = now
                    if (right.isClipping) rightClipTimestamp = now
                }
            }
        }

        // Forward untouched PCM data to ExoPlayer's next processor / AudioTrack
        val buffer = replaceOutputBuffer(remaining)
        buffer.put(inputBuffer)
        buffer.flip()
    }

    override fun onFlush() {
        targetLeftNormalized = 0f
        targetRightNormalized = 0f
        lastLeftRmsDb = -60f
        lastRightRmsDb = -60f
    }

    override fun onReset() {
        onFlush()
        leftBallistics.reset()
        rightBallistics.reset()
    }

    /**
     * Ballistics ticker running at 60 FPS (~16ms delta) calculating the exact
     * spring-mass-damping differential equations.
     */
    private fun startBallisticsTicker() {
        animationJob?.cancel()
        animationJob = scope.launch {
            var lastTimeNanos = System.nanoTime()
            val clipDurationMs = 150L

            while (isActive) {
                delay(16)
                val nowNanos = System.nanoTime()
                val dt = ((nowNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0.005f, 0.050f)
                lastTimeNanos = nowNanos

                val nowMs = System.currentTimeMillis()
                val leftNeedle = leftBallistics.update(targetLeftNormalized, dt)
                val rightNeedle = rightBallistics.update(targetRightNormalized, dt)

                // Overload clip led persistence
                val leftClip = (nowMs - leftClipTimestamp) < clipDurationMs
                val rightClip = (nowMs - rightClipTimestamp) < clipDurationMs

                // Decay the target slightly in case stream has paused
                targetLeftNormalized = (targetLeftNormalized * 0.90f).coerceAtLeast(0f)
                targetRightNormalized = (targetRightNormalized * 0.90f).coerceAtLeast(0f)

                _vuLevelsFlow.value = StereoVuLevels(
                    leftNormalized = leftNeedle,
                    rightNormalized = rightNeedle,
                    leftRmsDb = lastLeftRmsDb,
                    rightRmsDb = lastRightRmsDb,
                    leftPeakDb = lastLeftPeakDb,
                    rightPeakDb = lastRightPeakDb,
                    isLeftClipping = leftClip,
                    isRightClipping = rightClip
                )
            }
        }
    }

    fun release() {
        animationJob?.cancel()
    }
}
