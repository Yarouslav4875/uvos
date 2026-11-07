package com.uvos.calendar.data.local.entity

data class CalendarEventEntity(
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val isAllDay: Boolean = false,
    val recurrenceRule: String? = null, // RRULE:FREQ=WEEKLY;BYDAY=MO,WE,FR
    val colorHex: String = "#E02020",
    val syncId: String? = null,   // UID для CalDAV/ICS
    val etag: String? = null      // ETag для разрешения коллизий CalDAV
)
