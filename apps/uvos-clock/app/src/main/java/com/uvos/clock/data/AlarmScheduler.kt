package com.uvos.clock.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.uvos.clock.receiver.AlarmBroadcastReceiver
import com.uvos.clock.ui.main.ClockTasksActivity

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(alarm: AlarmEntity) {
        if (!alarm.isEnabled) {
            cancelAlarm(alarm.id)
            return
        }

        val triggerMillis = alarm.getNextTriggerMillis()

        val intent = Intent(context, AlarmBroadcastReceiver::class.java).apply {
            action = AlarmBroadcastReceiver.ACTION_ALARM_TRIGGER
            putExtra(AlarmBroadcastReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmBroadcastReceiver.EXTRA_ALARM_LABEL, alarm.label)
            putExtra(AlarmBroadcastReceiver.EXTRA_IS_CRESCENDO, alarm.isCrescendo)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, ClockTasksActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            1000 + alarm.id.toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val clockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
                alarmManager.setAlarmClock(clockInfo, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
            Log.d("AlarmScheduler", "Alarm #${alarm.id} set for $triggerMillis (${alarm.getFormattedTime()})")
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "Exact alarm permission missing", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    fun cancelAlarm(alarmId: Long) {
        val intent = Intent(context, AlarmBroadcastReceiver::class.java).apply {
            action = AlarmBroadcastReceiver.ACTION_ALARM_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("AlarmScheduler", "Alarm #$alarmId cancelled")
    }

    fun scheduleTaskReminder(task: TaskEntity) {
        val reminderTime = task.reminderTimestamp ?: return
        if (reminderTime <= System.currentTimeMillis() || task.isCompleted) return

        val intent = Intent(context, AlarmBroadcastReceiver::class.java).apply {
            action = AlarmBroadcastReceiver.ACTION_TASK_REMINDER
            putExtra(AlarmBroadcastReceiver.EXTRA_TASK_ID, task.id)
            putExtra(AlarmBroadcastReceiver.EXTRA_TASK_TITLE, task.title)
            putExtra(AlarmBroadcastReceiver.EXTRA_TASK_PRIORITY, task.priority)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (20000 + task.id).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Failed to schedule task reminder", e)
        }
    }

    fun rescheduleAllAlarms(dbHelper: ClockDatabaseHelper) {
        val alarms = dbHelper.getAllAlarms()
        for (alarm in alarms) {
            if (alarm.isEnabled) {
                scheduleAlarm(alarm)
            }
        }
    }
}
