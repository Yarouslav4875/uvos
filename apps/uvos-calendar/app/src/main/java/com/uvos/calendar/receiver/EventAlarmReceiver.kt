package com.uvos.calendar.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.uvos.calendar.R
import com.uvos.calendar.data.local.CalendarDatabase
import com.uvos.calendar.ui.month.CalendarActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_EVENT_REMINDER = "com.uvos.calendar.ACTION_EVENT_REMINDER"
        const val CHANNEL_ID = "uvos_calendar_events"

        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_START_TIME = "extra_start_time"
        const val EXTRA_LOCATION = "extra_location"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val database = CalendarDatabase.getInstance(context)

        if (Intent.ACTION_BOOT_COMPLETED == action) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = database.eventDao()
                    val pending = dao.getAllPendingReminders()
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
                    if (alarmManager != null) {
                        val now = System.currentTimeMillis()
                        for ((reminder, event) in pending) {
                            val triggerTime = event.startTimestamp - (reminder.minutesBefore * 60_000L)
                            if (triggerTime > now) {
                                val alarmIntent = Intent(context, EventAlarmReceiver::class.java).apply {
                                    this.action = ACTION_EVENT_REMINDER
                                    putExtra(EXTRA_EVENT_ID, event.id)
                                    putExtra(EXTRA_REMINDER_ID, reminder.reminderId)
                                    putExtra(EXTRA_TITLE, event.title)
                                    putExtra(EXTRA_DESCRIPTION, event.description)
                                    putExtra(EXTRA_START_TIME, event.startTimestamp)
                                    putExtra(EXTRA_LOCATION, event.location)
                                }
                                val pIntent = PendingIntent.getBroadcast(
                                    context,
                                    reminder.reminderId.toInt(),
                                    alarmIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    alarmManager.setExactAndAllowWhileIdle(
                                        android.app.AlarmManager.RTC_WAKEUP,
                                        triggerTime,
                                        pIntent
                                    )
                                } else {
                                    alarmManager.setExact(
                                        android.app.AlarmManager.RTC_WAKEUP,
                                        triggerTime,
                                        pIntent
                                    )
                                }
                            }
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        if (ACTION_EVENT_REMINDER == action) {
            val eventId = intent.getLongExtra(EXTRA_EVENT_ID, 0L)
            val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, 0L)
            val title = intent.getStringExtra(EXTRA_TITLE) ?: "Событие календаря"
            val description = intent.getStringExtra(EXTRA_DESCRIPTION)
            val startTime = intent.getLongExtra(EXTRA_START_TIME, 0L)
            val location = intent.getStringExtra(EXTRA_LOCATION)

            // Mark reminder as fired in database
            if (reminderId > 0) {
                CoroutineScope(Dispatchers.IO).launch {
                    database.eventDao().markReminderFired(reminderId)
                }
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Оповещения календаря M4200",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    this.description = "Высокоточные уведомления о запланированных событиях"
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val openIntent = Intent(context, CalendarActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("OPEN_EVENT_ID", eventId)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                eventId.toInt(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val timeStr = if (startTime > 0) {
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                "Начало в ${timeFormat.format(Date(startTime))}"
            } else ""

            val contentText = buildString {
                if (timeStr.isNotEmpty()) append(timeStr)
                if (!location.isNullOrBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(location)
                }
                if (!description.isNullOrBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(description)
                }
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_calendar)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setColor(0xFFE02020.toInt())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(eventId.toInt(), notification)
        }
    }
}
