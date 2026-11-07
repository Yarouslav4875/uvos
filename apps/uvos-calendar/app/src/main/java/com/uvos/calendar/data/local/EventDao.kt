package com.uvos.calendar.data.local

import com.uvos.calendar.data.local.entity.CalendarEventEntity
import com.uvos.calendar.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

interface EventDao {
    fun insertEvent(entity: CalendarEventEntity): Long
    fun updateEvent(entity: CalendarEventEntity)
    fun deleteEvent(id: Long)
    fun getEventById(id: Long): CalendarEventEntity?
    fun getAllEvents(): List<CalendarEventEntity>
    fun getEventsForRange(startMs: Long, endMs: Long): List<CalendarEventEntity>
    fun observeAllEvents(): Flow<List<CalendarEventEntity>>

    fun insertReminder(reminder: ReminderEntity): Long
    fun deleteRemindersForEvent(eventId: Long)
    fun getRemindersForEvent(eventId: Long): List<ReminderEntity>
    fun getAllPendingReminders(): List<Pair<ReminderEntity, CalendarEventEntity>>
    fun markReminderFired(reminderId: Long)
}
