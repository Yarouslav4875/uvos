package com.uvos.downloader.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.uvos.downloader.DownloaderApplication
import com.uvos.downloader.R
import com.uvos.downloader.ui.main.DownloaderActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DownloadForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var monitorJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as DownloaderApplication
        val engine = app.downloadEngine
        val taskId = intent?.getStringExtra(EXTRA_TASK_ID)

        when (intent?.action) {
            ACTION_START -> {
                if (taskId != null) {
                    engine.startTask(taskId)
                    startForeground(NOTIFICATION_ID, buildNotification("Запуск загрузки...", 0, 0, false))
                    startMonitoring(taskId)
                }
            }
            ACTION_PAUSE -> {
                if (taskId != null) {
                    engine.pauseTask(taskId)
                }
            }
            ACTION_CANCEL -> {
                if (taskId != null) {
                    engine.cancelTask(taskId)
                }
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startMonitoring(taskId: String) {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            val app = application as DownloaderApplication
            val repo = app.downloadRepository
            val engine = app.downloadEngine

            while (isActive) {
                delay(1000)
                val task = repo.getTaskById(taskId)
                if (task == null || task.isFinished) {
                    val finishText = if (task?.status?.name == "COMPLETED") "Загрузка завершена" else "Загрузка остановлена"
                    val finishedNotif = buildNotification(finishText, 100, 100, false)
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, finishedNotif)
                    stopForeground(STOP_FOREGROUND_DETACH)
                    stopSelf()
                    break
                }

                val speed = engine.speedMetric.value.formattedSpeed()
                val title = "${task.fileName} ($speed)"
                val progress = if (task.totalBytes > 0) ((task.downloadedBytes * 100) / task.totalBytes).toInt() else 0

                val notification = buildNotification(title, progress, 100, task.isIndeterminate)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun buildNotification(
        content: String,
        progress: Int,
        maxProgress: Int,
        indeterminate: Boolean
    ): Notification {
        val launchIntent = Intent(this, DownloaderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("UVOS Загрузчик")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification_download)
            .setProgress(maxProgress, progress, indeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val CHANNEL_ID = "uvos_download_service_channel"
        const val NOTIFICATION_ID = 4200

        const val ACTION_START = "com.uvos.downloader.action.START"
        const val ACTION_PAUSE = "com.uvos.downloader.action.PAUSE"
        const val ACTION_CANCEL = "com.uvos.downloader.action.CANCEL"
        const val EXTRA_TASK_ID = "extra_task_id"

        fun start(context: Context, taskId: String) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TASK_ID, taskId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pause(context: Context, taskId: String) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_PAUSE
                putExtra(EXTRA_TASK_ID, taskId)
            }
            context.startService(intent)
        }

        fun cancel(context: Context, taskId: String) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_TASK_ID, taskId)
            }
            context.startService(intent)
        }
    }
}
