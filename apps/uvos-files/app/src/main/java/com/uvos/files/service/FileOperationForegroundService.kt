package com.uvos.files.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.uvos.files.R

class FileOperationForegroundService : Service {

    companion object {
        const val CHANNEL_ID = "channel_file_ops"
        const val NOTIFICATION_ID = 4200
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_PROGRESS = "EXTRA_PROGRESS"

        fun startService(context: Context, title: String) {
            val intent = Intent(context, FileOperationForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, title)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateProgress(context: Context, title: String, progressPercent: Int) {
            val intent = Intent(context, FileOperationForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_PROGRESS, progressPercent)
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FileOperationForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    constructor() : super()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_START -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Файловая операция"
                val progress = intent.getIntExtra(EXTRA_PROGRESS, -1)
                val notification = buildNotification(title, progress)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                        } else {
                            0
                        }
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(title: String, progress: Int) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("UVOS Files: $title")
            .setContentText(if (progress >= 0) "$progress%" else "Обработка...")
            .setSmallIcon(R.drawable.ic_launcher_m4200)
            .setOngoing(true)
            .apply {
                if (progress >= 0) {
                    setProgress(100, progress, false)
                } else {
                    setProgress(0, 0, true)
                }
            }
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_file_ops),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_file_ops_desc)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
