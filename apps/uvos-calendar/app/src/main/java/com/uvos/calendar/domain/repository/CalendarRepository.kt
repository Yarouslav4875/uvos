package com.uvos.calendar.domain.repository

import com.uvos.calendar.domain.model.CalendarEvent
import kotlinx.coroutines.flow.Flow

interface CalendarRepository {
    fun observeAllEvents(): Flow<List<CalendarEvent>>
    suspend fun getEventsForRange(startMs: Long, endMs: Long): List<CalendarEvent>
    suspend fun getEventById(id: Long): CalendarEvent?
    suspend fun saveEvent(event: CalendarEvent): Long
    suspend fun deleteEvent(id: Long)
    suspend fun rescheduleAllAlarms()
    suspend fun exportEventsToIcs(): String
    suspend fun importEventsFromIcs(icsContent: String): Int
}
