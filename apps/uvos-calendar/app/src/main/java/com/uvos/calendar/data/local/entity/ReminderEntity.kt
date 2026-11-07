package com.uvos.calendar.data.local.entity

data class ReminderEntity(
    val reminderId: Long = 0,
    val eventId: Long,
    val minutesBefore: Int, // 5, 15, 30, 60, 1440
    val isFired: Boolean = false
)
