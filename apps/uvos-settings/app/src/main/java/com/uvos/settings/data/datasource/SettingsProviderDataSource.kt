package com.uvos.settings.data.datasource

import android.content.ContentResolver
import android.media.AudioManager
import android.provider.Settings
import com.uvos.settings.domain.model.AudioStreamType
import com.uvos.settings.domain.model.SystemFontFamily

class SettingsProviderDataSource(
    private val contentResolver: ContentResolver,
    private val audioManager: AudioManager
) {
    fun getBrightness(): Int {
        return try {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (_: Exception) {
            128
        }
    }

    fun setBrightness(value: Int) {
        val clamped = value.coerceIn(1, 255)
        try {
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                clamped
            )
        } catch (_: Exception) {
            // Write Settings permission may be needed
        }
    }

    fun isAutoBrightness(): Boolean {
        return try {
            Settings.System.getInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE
            ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        } catch (_: Exception) {
            false
        }
    }

    fun setAutoBrightness(enabled: Boolean) {
        val mode = if (enabled) {
            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        } else {
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        }
        try {
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                mode
            )
        } catch (_: Exception) {
            // Ignore if lack of permission in preview mode
        }
    }

    fun getRefreshRate(): Int {
        return try {
            val peak = Settings.Global.getFloat(contentResolver, "peak_refresh_rate", 120f)
            peak.toInt()
        } catch (_: Exception) {
            120
        }
    }

    fun setRefreshRate(rateHz: Int) {
        val rateFloat = if (rateHz == 120) 120f else 60f
        try {
            Settings.Global.putFloat(contentResolver, "peak_refresh_rate", rateFloat)
            Settings.Global.putFloat(contentResolver, "min_refresh_rate", rateFloat)
        } catch (_: Exception) {
            // Write secure settings permission
        }
    }

    fun getScreenTimeout(): Int {
        return try {
            val ms = Settings.System.getInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 30000)
            ms / 1000
        } catch (_: Exception) {
            30
        }
    }

    fun setScreenTimeout(seconds: Int) {
        try {
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                seconds * 1000
            )
        } catch (_: Exception) {
            //
        }
    }

    fun getStreamVolume(streamType: AudioStreamType): Int {
        val androidStream = toAndroidStream(streamType)
        return try {
            audioManager.getStreamVolume(androidStream)
        } catch (_: Exception) {
            7
        }
    }

    fun setStreamVolume(streamType: AudioStreamType, level: Int) {
        val androidStream = toAndroidStream(streamType)
        val maxVol = audioManager.getStreamMaxVolume(androidStream)
        val clamped = level.coerceIn(0, maxVol)
        try {
            audioManager.setStreamVolume(androidStream, clamped, 0)
        } catch (_: Exception) {
            //
        }
    }

    fun getFontScale(): Float {
        return try {
            Settings.System.getFloat(contentResolver, Settings.System.FONT_SCALE, 1.0f)
        } catch (_: Exception) {
            1.0f
        }
    }

    fun setFontScale(scale: Float) {
        try {
            Settings.System.putFloat(contentResolver, Settings.System.FONT_SCALE, scale)
        } catch (_: Exception) {
            //
        }
    }

    private fun toAndroidStream(streamType: AudioStreamType): Int {
        return when (streamType) {
            AudioStreamType.RING -> AudioManager.STREAM_RING
            AudioStreamType.MEDIA -> AudioManager.STREAM_MUSIC
            AudioStreamType.ALARM -> AudioManager.STREAM_ALARM
        }
    }
}
