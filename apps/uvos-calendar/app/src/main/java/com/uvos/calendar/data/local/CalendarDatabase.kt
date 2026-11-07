package com.uvos.calendar.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.uvos.calendar.data.local.entity.CalendarEventEntity
import com.uvos.calendar.data.local.entity.ReminderEntity
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onStart

class CalendarDatabase private constructor(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        const val DATABASE_NAME = "uvos_calendar.db"
        const val DATABASE_VERSION = 1

        private const val TABLE_EVENTS = "calendar_events"
        private const val TABLE_REMINDERS = "event_reminders"

        @Volatile
        private var instance: CalendarDatabase? = null

        fun getInstance(context: Context): CalendarDatabase {
            return instance ?: synchronized(this) {
                instance ?: CalendarDatabase(context).also { instance = it }
            }
        }
    }

    private val changeNotifier = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val eventDaoInstance = object : EventDao {
        override fun insertEvent(entity: CalendarEventEntity): Long {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("title", entity.title)
                put("description", entity.description)
                put("location", entity.location)
                put("startTimestamp", entity.startTimestamp)
                put("endTimestamp", entity.endTimestamp)
                put("isAllDay", if (entity.isAllDay) 1 else 0)
                put("recurrenceRule", entity.recurrenceRule)
                put("colorHex", entity.colorHex)
                put("syncId", entity.syncId)
                put("etag", entity.etag)
            }
            val id = db.insert(TABLE_EVENTS, null, values)
            notifyChanged()
            return id
        }

        override fun updateEvent(entity: CalendarEventEntity) {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("title", entity.title)
                put("description", entity.description)
                put("location", entity.location)
                put("startTimestamp", entity.startTimestamp)
                put("endTimestamp", entity.endTimestamp)
                put("isAllDay", if (entity.isAllDay) 1 else 0)
                put("recurrenceRule", entity.recurrenceRule)
                put("colorHex", entity.colorHex)
                put("syncId", entity.syncId)
                put("etag", entity.etag)
            }
            db.update(TABLE_EVENTS, values, "id = ?", arrayOf(entity.id.toString()))
            notifyChanged()
        }

        override fun deleteEvent(id: Long) {
            val db = writableDatabase
            db.delete(TABLE_REMINDERS, "eventId = ?", arrayOf(id.toString()))
            db.delete(TABLE_EVENTS, "id = ?", arrayOf(id.toString()))
            notifyChanged()
        }

        override fun getEventById(id: Long): CalendarEventEntity? {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_EVENTS,
                null,
                "id = ?",
                arrayOf(id.toString()),
                null,
                null,
                null
            )
            return cursor.use {
                if (it.moveToFirst()) mapCursorToEvent(it) else null
            }
        }

        override fun getAllEvents(): List<CalendarEventEntity> {
            val db = readableDatabase
            val list = mutableListOf<CalendarEventEntity>()
            val cursor = db.query(
                TABLE_EVENTS,
                null,
                null,
                null,
                null,
                null,
                "startTimestamp ASC"
            )
            cursor.use {
                while (it.moveToNext()) {
                    list.add(mapCursorToEvent(it))
                }
            }
            return list
        }

        override fun getEventsForRange(startMs: Long, endMs: Long): List<CalendarEventEntity> {
            val db = readableDatabase
            val list = mutableListOf<CalendarEventEntity>()
            // Events that overlap the window [startMs, endMs]
            val cursor = db.query(
                TABLE_EVENTS,
                null,
                "endTimestamp >= ? AND startTimestamp <= ?",
                arrayOf(startMs.toString(), endMs.toString()),
                null,
                null,
                "startTimestamp ASC"
            )
            cursor.use {
                while (it.moveToNext()) {
                    list.add(mapCursorToEvent(it))
                }
            }
            return list
        }

        override fun observeAllEvents(): Flow<List<CalendarEventEntity>> {
            return kotlinx.coroutines.flow.flow {
                changeNotifier.onStart { emit(Unit) }.collect {
                    emit(getAllEvents())
                }
            }
        }

        override fun insertReminder(reminder: ReminderEntity): Long {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("eventId", reminder.eventId)
                put("minutesBefore", reminder.minutesBefore)
                put("isFired", if (reminder.isFired) 1 else 0)
            }
            val id = db.insert(TABLE_REMINDERS, null, values)
            notifyChanged()
            return id
        }

        override fun deleteRemindersForEvent(eventId: Long) {
            val db = writableDatabase
            db.delete(TABLE_REMINDERS, "eventId = ?", arrayOf(eventId.toString()))
            notifyChanged()
        }

        override fun getRemindersForEvent(eventId: Long): List<ReminderEntity> {
            val db = readableDatabase
            val list = mutableListOf<ReminderEntity>()
            val cursor = db.query(
                TABLE_REMINDERS,
                null,
                "eventId = ?",
                arrayOf(eventId.toString()),
                null,
                null,
                "minutesBefore ASC"
            )
            cursor.use {
                while (it.moveToNext()) {
                    list.add(mapCursorToReminder(it))
                }
            }
            return list
        }

        override fun getAllPendingReminders(): List<Pair<ReminderEntity, CalendarEventEntity>> {
            val db = readableDatabase
            val list = mutableListOf<Pair<ReminderEntity, CalendarEventEntity>>()
            val query = """
                SELECT r.reminderId, r.eventId, r.minutesBefore, r.isFired,
                       e.id, e.title, e.description, e.location, e.startTimestamp,
                       e.endTimestamp, e.isAllDay, e.recurrenceRule, e.colorHex, e.syncId, e.etag
                FROM $TABLE_REMINDERS r
                INNER JOIN $TABLE_EVENTS e ON r.eventId = e.id
                WHERE r.isFired = 0
            """.trimIndent()
            val cursor = db.rawQuery(query, null)
            cursor.use {
                while (it.moveToNext()) {
                    val reminder = ReminderEntity(
                        reminderId = it.getLong(0),
                        eventId = it.getLong(1),
                        minutesBefore = it.getInt(2),
                        isFired = it.getInt(3) != 0
                    )
                    val event = CalendarEventEntity(
                        id = it.getLong(4),
                        title = it.getString(5),
                        description = it.getString(6),
                        location = it.getString(7),
                        startTimestamp = it.getLong(8),
                        endTimestamp = it.getLong(9),
                        isAllDay = it.getInt(10) != 0,
                        recurrenceRule = it.getString(11),
                        colorHex = it.getString(12) ?: "#E02020",
                        syncId = it.getString(13),
                        etag = it.getString(14)
                    )
                    list.add(reminder to event)
                }
            }
            return list
        }

        override fun markReminderFired(reminderId: Long) {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("isFired", 1)
            }
            db.update(TABLE_REMINDERS, values, "reminderId = ?", arrayOf(reminderId.toString()))
            notifyChanged()
        }
    }

    fun eventDao(): EventDao = eventDaoInstance

    private fun notifyChanged() {
        changeNotifier.tryEmit(Unit)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_EVENTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT,
                location TEXT,
                startTimestamp INTEGER NOT NULL,
                endTimestamp INTEGER NOT NULL,
                isAllDay INTEGER NOT NULL DEFAULT 0,
                recurrenceRule TEXT,
                colorHex TEXT NOT NULL DEFAULT '#E02020',
                syncId TEXT,
                etag TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_REMINDERS (
                reminderId INTEGER PRIMARY KEY AUTOINCREMENT,
                eventId INTEGER NOT NULL,
                minutesBefore INTEGER NOT NULL,
                isFired INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(eventId) REFERENCES $TABLE_EVENTS(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_REMINDERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EVENTS")
        onCreate(db)
    }

    private fun mapCursorToEvent(cursor: Cursor): CalendarEventEntity {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
        val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
        val descIdx = cursor.getColumnIndexOrThrow("description")
        val description = if (cursor.isNull(descIdx)) null else cursor.getString(descIdx)
        val locIdx = cursor.getColumnIndexOrThrow("location")
        val location = if (cursor.isNull(locIdx)) null else cursor.getString(locIdx)
        val startTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow("startTimestamp"))
        val endTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow("endTimestamp"))
        val isAllDay = cursor.getInt(cursor.getColumnIndexOrThrow("isAllDay")) != 0
        val rruleIdx = cursor.getColumnIndexOrThrow("recurrenceRule")
        val recurrenceRule = if (cursor.isNull(rruleIdx)) null else cursor.getString(rruleIdx)
        val colorHex = cursor.getString(cursor.getColumnIndexOrThrow("colorHex")) ?: "#E02020"
        val syncIdIdx = cursor.getColumnIndexOrThrow("syncId")
        val syncId = if (cursor.isNull(syncIdIdx)) null else cursor.getString(syncIdIdx)
        val etagIdx = cursor.getColumnIndexOrThrow("etag")
        val etag = if (cursor.isNull(etagIdx)) null else cursor.getString(etagIdx)

        return CalendarEventEntity(
            id = id,
            title = title,
            description = description,
            location = location,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            isAllDay = isAllDay,
            recurrenceRule = recurrenceRule,
            colorHex = colorHex,
            syncId = syncId,
            etag = etag
        )
    }

    private fun mapCursorToReminder(cursor: Cursor): ReminderEntity {
        val reminderId = cursor.getLong(cursor.getColumnIndexOrThrow("reminderId"))
        val eventId = cursor.getLong(cursor.getColumnIndexOrThrow("eventId"))
        val minutesBefore = cursor.getInt(cursor.getColumnIndexOrThrow("minutesBefore"))
        val isFired = cursor.getInt(cursor.getColumnIndexOrThrow("isFired")) != 0
        return ReminderEntity(
            reminderId = reminderId,
            eventId = eventId,
            minutesBefore = minutesBefore,
            isFired = isFired
        )
    }
}
