package com.uvos.clock

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class UvosClockApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM_ID,
                "Будильник UVOS",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Канал звуковых сигналов будильника UVOS"
                enableVibration(true)
                setBypassDnd(true)
            }

            val timerChannel = NotificationChannel(
                CHANNEL_TIMER_ID,
                "Таймер обратного отсчета UVOS",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Фоновый отсчет таймера прибора M4200"
                enableVibration(false)
            }

            val tasksChannel = NotificationChannel(
                CHANNEL_TASKS_ID,
                "Задачи и напоминания UVOS",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Напоминания матрицы задач Эйзенхауэра"
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(
                listOf(alarmChannel, timerChannel, tasksChannel)
            )
        }
    }

    companion object {
        const val CHANNEL_ALARM_ID = "channel_alarm_m4200"
        const val CHANNEL_TIMER_ID = "channel_timer_fgs"
        const val CHANNEL_TASKS_ID = "channel_tasks_eisenhower"
    }
}
