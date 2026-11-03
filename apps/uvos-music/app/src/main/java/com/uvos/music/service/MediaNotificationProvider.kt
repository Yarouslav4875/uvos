package com.uvos.music.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import com.uvos.music.R

@OptIn(UnstableApi::class)
class MediaNotificationProvider(
    private val context: Context
) : DefaultMediaNotificationProvider(context) {

    companion object {
        const val CHANNEL_ID = "uvos_music_playback"
        const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
        setSmallIcon(R.drawable.ic_launcher_music)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "UVOS Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Offline Hi-Res studio audio playback controls"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
