package com.uvos.calendar.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.uvos.calendar.data.local.CalendarDatabase
import com.uvos.calendar.data.local.EventDao
import com.uvos.calendar.data.local.entity.CalendarEventEntity
import com.uvos.calendar.data.local.entity.ReminderEntity
import com.uvos.calendar.data.sync.IcsParser
import com.uvos.calendar.domain.model.CalendarEvent
import com.uvos.calendar.domain.model.ReminderAlert
import com.uvos.calendar.domain.repository.CalendarRepository
import com.uvos.calendar.receiver.EventAlarmReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CalendarRepositoryImpl(
    private val context: Context,
    private val eventDao: EventDao = CalendarDatabase.getInstance(context).eventDao()
) : CalendarRepository {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    override fun observeAllEvents(): Flow<List<CalendarEvent>> {
        return eventDao.observeAllEvents().map { entities ->
            entities.map { entity ->
                val reminders = eventDao.getRemindersForEvent(entity.id).map { rem ->
                    ReminderAlert(
                        reminderId = rem.reminderId,
                        eventId = rem.eventId,
                        minutesBefore = rem.minutesBefore,
                        isFired = rem.isFired
                    )
                }
                mapEntityToDomain(entity, reminders)
            }
        }
    }

    override suspend fun getEventsForRange(startMs: Long, endMs: Long): List<CalendarEvent> =
        withContext(Dispatchers.IO) {
            val entities = eventDao.getEventsForRange(startMs, endMs)
            entities.map { entity ->
                val reminders = eventDao.getRemindersForEvent(entity.id).map { rem ->
                    ReminderAlert(
                        reminderId = rem.reminderId,
                        eventId = rem.eventId,
                        minutesBefore = rem.minutesBefore,
                        isFired = rem.isFired
                    )
                }
                mapEntityToDomain(entity, reminders)
            }
        }

    override suspend fun getEventById(id: Long): CalendarEvent? =
        withContext(Dispatchers.IO) {
            val entity = eventDao.getEventById(id) ?: return@withContext null
            val reminders = eventDao.getRemindersForEvent(id).map { rem ->
                ReminderAlert(
                    reminderId = rem.reminderId,
                    eventId = rem.eventId,
                    minutesBefore = rem.minutesBefore,
                    isFired = rem.isFired
                )
            }
            mapEntityToDomain(entity, reminders)
        }

    override suspend fun saveEvent(event: CalendarEvent): Long =
        withContext(Dispatchers.IO) {
            val entity = CalendarEventEntity(
                id = event.id,
                title = event.title,
                description = event.description,
                location = event.location,
                startTimestamp = event.startTimestamp,
                endTimestamp = event.endTimestamp,
                isAllDay = event.isAllDay,
                recurrenceRule = event.recurrenceRule,
                colorHex = event.colorHex,
                syncId = event.syncId,
                etag = System.currentTimeMillis().toString()
            )

            val eventId: Long
            if (event.id == 0L) {
                eventId = eventDao.insertEvent(entity)
            } else {
                eventId = event.id
                // Cancel existing alarms before replacing reminders
                val existingReminders = eventDao.getRemindersForEvent(eventId)
                for (rem in existingReminders) {
                    cancelAlarm(rem.reminderId)
                }
                eventDao.updateEvent(entity)
                eventDao.deleteRemindersForEvent(eventId)
            }

            // Insert new reminders and schedule alarms
            for (reminder in event.reminders) {
                val remEntity = ReminderEntity(
                    eventId = eventId,
                    minutesBefore = reminder.minutesBefore,
                    isFired = false
                )
                val remId = eventDao.insertReminder(remEntity)
                scheduleAlarm(eventId, remId, event.title, event.description, event.startTimestamp, event.location, reminder.minutesBefore)
            }

            eventId
        }

    override suspend fun deleteEvent(id: Long) =
        withContext(Dispatchers.IO) {
            val existingReminders = eventDao.getRemindersForEvent(id)
            for (rem in existingReminders) {
                cancelAlarm(rem.reminderId)
            }
            eventDao.deleteEvent(id)
        }

    override suspend fun rescheduleAllAlarms() =
        withContext(Dispatchers.IO) {
            val pending = eventDao.getAllPendingReminders()
            for ((reminder, event) in pending) {
                scheduleAlarm(
                    eventId = event.id,
                    reminderId = reminder.reminderId,
                    title = event.title,
                    description = event.description,
                    startTimestamp = event.startTimestamp,
                    location = event.location,
                    minutesBefore = reminder.minutesBefore
                )
            }
        }

    override suspend fun exportEventsToIcs(): String =
        withContext(Dispatchers.IO) {
            val all = eventDao.getAllEvents().map { entity ->
                val reminders = eventDao.getRemindersForEvent(entity.id).map { rem ->
                    ReminderAlert(
                        reminderId = rem.reminderId,
                        eventId = rem.eventId,
                        minutesBefore = rem.minutesBefore,
                        isFired = rem.isFired
                    )
                }
                mapEntityToDomain(entity, reminders)
            }
            IcsParser.exportToIcs(all)
        }

    override suspend fun importEventsFromIcs(icsContent: String): Int =
        withContext(Dispatchers.IO) {
            val parsedEvents = IcsParser.parseIcs(icsContent)
            var count = 0
            for (event in parsedEvents) {
                saveEvent(event)
                count++
            }
            count
        }

    private fun scheduleAlarm(
        eventId: Long,
        reminderId: Long,
        title: String,
        description: String?,
        startTimestamp: Long,
        location: String?,
        minutesBefore: Int
    ) {
        if (alarmManager == null) return
        val triggerTime = startTimestamp - (minutesBefore * 60_000L)
        val now = System.currentTimeMillis()
        if (triggerTime <= now) return

        val intent = Intent(context, EventAlarmReceiver::class.java).apply {
            action = EventAlarmReceiver.ACTION_EVENT_REMINDER
            putExtra(EventAlarmReceiver.EXTRA_EVENT_ID, eventId)
            putExtra(EventAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(EventAlarmReceiver.EXTRA_TITLE, title)
            putExtra(EventAlarmReceiver.EXTRA_DESCRIPTION, description)
            putExtra(EventAlarmReceiver.EXTRA_START_TIME, startTimestamp)
            putExtra(EventAlarmReceiver.EXTRA_LOCATION, location)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    private fun cancelAlarm(reminderId: Long) {
        if (alarmManager == null) return
        val intent = Intent(context, EventAlarmReceiver::class.java).apply {
            action = EventAlarmReceiver.ACTION_EVENT_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun mapEntityToDomain(
        entity: CalendarEventEntity,
        reminders: List<ReminderAlert>
    ): CalendarEvent {
        return CalendarEvent(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            location = entity.location,
            startTimestamp = entity.startTimestamp,
            endTimestamp = entity.endTimestamp,
            isAllDay = entity.isAllDay,
            recurrenceRule = entity.recurrenceRule,
            colorHex = entity.colorHex,
            syncId = entity.syncId,
            reminders = reminders
        )
    }
}
