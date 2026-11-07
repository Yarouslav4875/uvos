package com.uvos.clock.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ClockDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT,
                priority INTEGER NOT NULL,
                is_completed INTEGER NOT NULL DEFAULT 0,
                due_timestamp INTEGER,
                reminder_timestamp INTEGER,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE alarms (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                hour INTEGER NOT NULL,
                minute INTEGER NOT NULL,
                is_enabled INTEGER NOT NULL DEFAULT 1,
                days_of_week INTEGER NOT NULL DEFAULT 127,
                label TEXT NOT NULL,
                is_crescendo INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )

        // Добавим демо-задачи и дефолтный будильник для быстрого старта системы
        insertDefaultData(db)
    }

    private fun insertDefaultData(db: SQLiteDatabase) {
        // Будильник на 07:00
        val alarmValues = ContentValues().apply {
            put("hour", 7)
            put("minute", 0)
            put("is_enabled", 1)
            put("days_of_week", 31) // Будни
            put("label", "Дежурный подъем M4200")
            put("is_crescendo", 1)
        }
        db.insert("alarms", null, alarmValues)

        // Задачи матрицы Эйзенхауэра
        val tasks = listOf(
            Triple("Калибровка прибора M4200", "Проверить демпфер и нулевую балансировку стрелок", 1),
            Triple("Плановое ТО энергоблока", "Провести проверку напряжения и температурного режима", 2),
            Triple("Передать журнал дежурному", "Сверить записи кругов секундомера за смену", 3),
            Triple("Полировка латунных клемм", "Очистить окислы на контактах шасси", 4)
        )

        for (task in tasks) {
            val cv = ContentValues().apply {
                put("title", task.first)
                put("description", task.second)
                put("priority", task.third)
                put("is_completed", 0)
                put("created_at", System.currentTimeMillis())
            }
            db.insert("tasks", null, cv)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS tasks")
        db.execSQL("DROP TABLE IF EXISTS alarms")
        onCreate(db)
    }

    // --- Методы работы с задачами ---

    fun getAllTasks(): List<TaskEntity> {
        val list = mutableListOf<TaskEntity>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM tasks ORDER BY is_completed ASC, priority ASC, id DESC", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToTask(it))
            }
        }
        return list
    }

    fun getTasksByPriority(priority: Int): List<TaskEntity> {
        val list = mutableListOf<TaskEntity>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM tasks WHERE priority = ? ORDER BY is_completed ASC, id DESC", arrayOf(priority.toString()))
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToTask(it))
            }
        }
        return list
    }

    fun insertTask(task: TaskEntity): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("title", task.title)
            put("description", task.description)
            put("priority", task.priority)
            put("is_completed", if (task.isCompleted) 1 else 0)
            put("due_timestamp", task.dueTimestamp)
            put("reminder_timestamp", task.reminderTimestamp)
            put("created_at", task.createdAt)
        }
        return db.insert("tasks", null, cv)
    }

    fun updateTask(task: TaskEntity): Int {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("title", task.title)
            put("description", task.description)
            put("priority", task.priority)
            put("is_completed", if (task.isCompleted) 1 else 0)
            put("due_timestamp", task.dueTimestamp)
            put("reminder_timestamp", task.reminderTimestamp)
        }
        return db.update("tasks", cv, "id = ?", arrayOf(task.id.toString()))
    }

    fun deleteTask(id: Long): Int {
        val db = writableDatabase
        return db.delete("tasks", "id = ?", arrayOf(id.toString()))
    }

    private fun cursorToTask(c: Cursor): TaskEntity {
        return TaskEntity(
            id = c.getLong(c.getColumnIndexOrThrow("id")),
            title = c.getString(c.getColumnIndexOrThrow("title")),
            description = c.getString(c.getColumnIndexOrThrow("description")),
            priority = c.getInt(c.getColumnIndexOrThrow("priority")),
            isCompleted = c.getInt(c.getColumnIndexOrThrow("is_completed")) == 1,
            dueTimestamp = if (c.isNull(c.getColumnIndexOrThrow("due_timestamp"))) null else c.getLong(c.getColumnIndexOrThrow("due_timestamp")),
            reminderTimestamp = if (c.isNull(c.getColumnIndexOrThrow("reminder_timestamp"))) null else c.getLong(c.getColumnIndexOrThrow("reminder_timestamp")),
            createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"))
        )
    }

    // --- Методы работы с будильниками ---

    fun getAllAlarms(): List<AlarmEntity> {
        val list = mutableListOf<AlarmEntity>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM alarms ORDER BY hour ASC, minute ASC", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToAlarm(it))
            }
        }
        return list
    }

    fun insertAlarm(alarm: AlarmEntity): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("hour", alarm.hour)
            put("minute", alarm.minute)
            put("is_enabled", if (alarm.isEnabled) 1 else 0)
            put("days_of_week", alarm.daysOfWeek)
            put("label", alarm.label)
            put("is_crescendo", if (alarm.isCrescendo) 1 else 0)
        }
        return db.insert("alarms", null, cv)
    }

    fun updateAlarm(alarm: AlarmEntity): Int {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("hour", alarm.hour)
            put("minute", alarm.minute)
            put("is_enabled", if (alarm.isEnabled) 1 else 0)
            put("days_of_week", alarm.daysOfWeek)
            put("label", alarm.label)
            put("is_crescendo", if (alarm.isCrescendo) 1 else 0)
        }
        return db.update("alarms", cv, "id = ?", arrayOf(alarm.id.toString()))
    }

    fun deleteAlarm(id: Long): Int {
        val db = writableDatabase
        return db.delete("alarms", "id = ?", arrayOf(id.toString()))
    }

    fun getAlarmById(id: Long): AlarmEntity? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM alarms WHERE id = ?", arrayOf(id.toString()))
        cursor.use {
            if (it.moveToNext()) {
                return cursorToAlarm(it)
            }
        }
        return null
    }

    private fun cursorToAlarm(c: Cursor): AlarmEntity {
        return AlarmEntity(
            id = c.getLong(c.getColumnIndexOrThrow("id")),
            hour = c.getInt(c.getColumnIndexOrThrow("hour")),
            minute = c.getInt(c.getColumnIndexOrThrow("minute")),
            isEnabled = c.getInt(c.getColumnIndexOrThrow("is_enabled")) == 1,
            daysOfWeek = c.getInt(c.getColumnIndexOrThrow("days_of_week")),
            label = c.getString(c.getColumnIndexOrThrow("label")),
            isCrescendo = c.getInt(c.getColumnIndexOrThrow("is_crescendo")) == 1
        )
    }

    companion object {
        private const val DATABASE_NAME = "uvos_clock_tasks.db"
        private const val DATABASE_VERSION = 1
    }
}
