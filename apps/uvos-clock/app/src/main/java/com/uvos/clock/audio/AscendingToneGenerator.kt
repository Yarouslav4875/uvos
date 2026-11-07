package com.uvos.clock.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * AscendingToneGenerator — синтезатор плавного нарастающего акустического сигнала
 * прибора UVOS M4200.
 *
 * Полностью 100% автономный синтез звука через AudioTrack PCM.
 * Исключает резкий пробуждающий шок: громкость стартует с 0.05 и
 * плавно повышается до 1.0 в течение rampDurationSec.
 */
class AscendingToneGenerator(private val context: Context) {

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    private var isPlaying = false

    fun start(rampDurationSec: Int = 30) {
        if (isPlaying) return
        isPlaying = true

        playbackJob = scope.launch {
            val sampleRate = 44100
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(sampleRate / 4)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            track.play()

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            val buffer = ShortArray(sampleRate / 10) // 100ms chunk
            var phase1 = 0.0
            var phase2 = 0.0
            val freq1 = 659.25 // E5 (гармоничный приборный колокольчик)
            val freq2 = 880.00 // A5 (кварцевый обертон)
            val startTime = System.currentTimeMillis()

            var vibeTick = 0

            while (isActive && isPlaying) {
                val elapsedSec = (System.currentTimeMillis() - startTime) / 1000f
                // Нарастание громкости от 0.05f до 1.0f
                val volume = (0.05f + (elapsedSec / rampDurationSec).coerceAtMost(1f) * 0.95f)

                // Периодический паттерн звука (800мс звук, 400мс тишина)
                val cycleTime = (System.currentTimeMillis() % 1200)
                val isBeepActive = cycleTime < 800

                if (isBeepActive) {
                    for (i in buffer.indices) {
                        val sample1 = sin(phase1)
                        val sample2 = sin(phase2) * 0.4
                        val combined = (sample1 + sample2) * volume * 0.7
                        buffer[i] = (combined * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

                        phase1 += 2.0 * PI * freq1 / sampleRate
                        if (phase1 > 2.0 * PI) phase1 -= 2.0 * PI

                        phase2 += 2.0 * PI * freq2 / sampleRate
                        if (phase2 > 2.0 * PI) phase2 -= 2.0 * PI
                    }
                } else {
                    buffer.fill(0)
                }

                track.write(buffer, 0, buffer.size)

                // Вибрация в такт сигналу с нарастанием
                vibeTick++
                if (vibeTick % 10 == 0 && isBeepActive) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val amplitude = (50 + (volume * 200).toInt()).coerceIn(1, 255)
                        vibrator?.vibrate(VibrationEffect.createOneShot(120, amplitude))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(120)
                    }
                }
            }

            try {
                track.stop()
                track.release()
            } catch (_: Exception) {}
        }
    }

    fun stop() {
        isPlaying = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
