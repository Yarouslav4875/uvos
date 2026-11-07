package com.uvos.clock.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.uvos.clock.R
import com.uvos.clock.UvosClockApplication
import com.uvos.clock.data.AlarmScheduler
import com.uvos.clock.data.ClockDatabaseHelper
import com.uvos.clock.ui.alarm.AlarmRingingActivity
import com.uvos.clock.ui.main.ClockTasksActivity

class AlarmBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "UVOS:AlarmWakeLock"
        )
        wakeLock.acquire(30000) // 30 секунд максимальный таймаут

        try {
            when (action) {
                ACTION_ALARM_TRIGGER -> {
                    val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
                    val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Будильник UVOS"
                    val isCrescendo = intent.getBooleanExtra(EXTRA_IS_CRESCENDO, true)

                    // Запуск экрана звонящего будильника
                    val alarmIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                        putExtra(EXTRA_ALARM_ID, alarmId)
                        putExtra(EXTRA_ALARM_LABEL, label)
                        putExtra(EXTRA_IS_CRESCENDO, isCrescendo)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_USER_ACTION
                    }

                    // Полноэкранный интент уведомления для обхода ограничений Android 10+
                    val fullScreenPendingIntent = PendingIntent.getActivity(
                        context,
                        (3000 + alarmId).toInt(),
                        alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val notification = NotificationCompat.Builder(context, UvosClockApplication.CHANNEL_ALARM_ID)
                        .setSmallIcon(R.drawable.ic_launcher_clock)
                        .setContentTitle("ЗВОНИТ БУДИЛЬНИК")
                        .setContentText(label)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setFullScreenIntent(fullScreenPendingIntent, true)
                        .setAutoCancel(true)
                        .build()

                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify((1000 + alarmId).toInt(), notification)

                    try {
                        context.startActivity(alarmIntent)
                    } catch (_: Exception) {}

                    // Планирование следующего срабатывания, если будильник повторяющийся
                    if (alarmId > 0) {
                        val db = ClockDatabaseHelper(context)
                        val alarm = db.getAlarmById(alarmId)
                        if (alarm != null && alarm.daysOfWeek != 0) {
                            AlarmScheduler(context).scheduleAlarm(alarm)
                        }
                    }
                }

                ACTION_TASK_REMINDER -> {
                    val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                    val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Задача UVOS"
                    val priority = intent.getIntExtra(EXTRA_TASK_PRIORITY, 1)

                    val priorityLabel = when (priority) {
                        1 -> "[!КРИТИЧНО! Срочно/Важно]"
                        2 -> "[СТРАТЕГИЧЕСКИ: Важно]"
                        3 -> "[ОПЕРАТИВНО: Срочно]"
                        else -> "[РУТИНА]"
                    }

                    val openIntent = Intent(context, ClockTasksActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val openPendingIntent = PendingIntent.getActivity(
                        context,
                        (5000 + taskId).toInt(),
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val notification = NotificationCompat.Builder(context, UvosClockApplication.CHANNEL_TASKS_ID)
                        .setSmallIcon(R.drawable.ic_launcher_clock)
                        .setContentTitle("Напоминание задачи: $priorityLabel")
                        .setContentText(taskTitle)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(openPendingIntent)
                        .build()

                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify((20000 + taskId).toInt(), notification)
                }

                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIME_SET,
                Intent.ACTION_TIMEZONE_CHANGED -> {
                    val db = ClockDatabaseHelper(context)
                    val scheduler = AlarmScheduler(context)
                    scheduler.rescheduleAllAlarms(db)
                }
            }
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    companion object {
        const val ACTION_ALARM_TRIGGER = "com.uvos.clock.ACTION_ALARM_TRIGGER"
        const val ACTION_TASK_REMINDER = "com.uvos.clock.ACTION_TASK_REMINDER"
        const val ACTION_TIMER_EXPIRED = "com.uvos.clock.ACTION_TIMER_EXPIRED"

        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"
        const val EXTRA_IS_CRESCENDO = "extra_is_crescendo"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_PRIORITY = "extra_task_priority"
    }
}
