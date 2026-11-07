package com.uvos.calendar.domain.model

data class CalendarEvent(
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val isAllDay: Boolean = false,
    val recurrenceRule: String? = null,
    val colorHex: String = "#E02020",
    val syncId: String? = null,
    val reminders: List<ReminderAlert> = emptyList()
)
