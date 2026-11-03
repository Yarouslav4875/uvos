# UVOS Calendar (`com.uvos.calendar`) — Техническая спецификация и методы разработки

> **Статус**: Архитектурная спецификация и документация разработки  
> **Пакет**: `com.uvos.calendar`  
> **Целевая платформа**: UnderVoltOS / Android 14 (API 34)  
> **Политика приватности**: Zero Telemetry, 0% рекламы, полная локальность базы данных без Google Calendar API.

---

## 1. Назначение и концепция

`UVOS Calendar` — локальный защищённый календарь-планировщик для экосистемы UnderVoltOS. Он спроектирован как полная замена `Google Calendar` (`com.google.android.calendar`), исключающая передачу метаданных о встречах, контактах участников, геолокациях и расписании пользователя на внешние серверы корпораций.

### Ключевые требования:
- **Локальное хранение (Local-First)**: база данных SQLite (Room) с открытой схемой и возможностью локального экспорта в стандартные файлы `.ics` (iCalendar / RFC 5545).
- **Синхронизация по CalDAV**: опциональная синхронизация строго с персональными доверенными серверами (Nextcloud, Radicale, Baïkal) по защищённому протоколу TLS без сторонних посредников.
- **Индустриальный дизайн M4200**:
  - Круговой ретро-циферблат отображения текущего дня, недели, месяца и фазы луны.
  - Тёмная палитра (`#0E0E12`, карточки `#1C1E26`, слоновая кость `#E8E4D8`, акцентный красный `#E02020`).
- **Точные локальные оповещения (Exact Alarms)**: использование `AlarmManager.setExactAndAllowWhileIdle()` для надежного срабатывания будильников событий без фоновой работы сервисов Google.

---

## 2. Архитектура приложения

```
com.uvos.calendar/
├── data/
│   ├── local/
│   │   ├── CalendarDatabase.kt
│   │   ├── EventDao.kt
│   │   └── entity/
│   │       ├── CalendarEventEntity.kt
│   │       └── ReminderEntity.kt
│   ├── sync/
│   │   ├── CalDavSyncAdapter.kt
│   │   └── IcsParser.kt
│   └── repository/
│       └── CalendarRepositoryImpl.kt
├── domain/
│   ├── model/
│   │   ├── CalendarEvent.kt
│   │   ├── RecurrenceRule.kt
│   │   └── ReminderAlert.kt
│   └── repository/
│       └── CalendarRepository.kt
├── receiver/
│   └── EventAlarmReceiver.kt
└── ui/
    ├── dial/
    │   └── RetroCalendarDialView.kt
    ├── month/
    │   ├── MonthGridScreen.kt
    │   └── CalendarViewModel.kt
    └── event/
        └── EditEventBottomSheet.kt
```

---

## 3. Схема базы данных Room

```kotlin
@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String?,
    val location: String?,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val isAllDay: Boolean = false,
    val recurrenceRule: String?, // RRULE:FREQ=WEEKLY;BYDAY=MO,WE,FR
    val colorHex: String = "#E02020",
    val syncId: String? = null,   // UID для CalDAV/ICS
    val etag: String? = null      // ETag для разрешения коллизий CalDAV
)

@Entity(
    tableName = "event_reminders",
    foreignKeys = [
        ForeignKey(
            entity = CalendarEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val reminderId: Long = 0,
    val eventId: Long,
    val minutesBefore: Int, // 5, 15, 30, 60, 1440
    val isFired: Boolean = false
)
```

---

## 4. UI: Ретро-циферблат даты (M4200 Dial View)

Круговой циферблат отображает день месяца и индикатор загруженности дня в виде секторов:

```kotlin
@Composable
fun RetroDateDial(
    currentDay: Int,
    daysInMonth: Int,
    eventsCount: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(160.dp)) {
        // Диск циферблата
        drawCircle(color = Color(0xFFE8E4D8))
        // Круговая шкала дней месяца (360 градусов / daysInMonth)
        val anglePerDay = 360f / daysInMonth
        val currentAngle = -90f + (currentDay - 1) * anglePerDay

        // Стрелка указателя дня
        rotate(degrees = currentAngle) {
            drawLine(
                color = Color(0xFFE02020),
                start = center,
                end = Offset(center.x, center.y - size.height * 0.40f),
                strokeWidth = 3.dp.toPx()
            )
        }
        drawCircle(color = Color(0xFF1A1A1A), radius = 5.dp.toPx())
    }
}
```

---

## 5. Манифест и разрешения

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.uvos.calendar">

    <!-- Доступ к сети опционален: нужен только при включении CalDAV синхронизации -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <application
        android:label="UVOS Календарь"
        android:theme="@style/Theme.UnderVoltOS.Dark">

        <receiver
            android:name=".receiver.EventAlarmReceiver"
            android:exported="false">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>

        <activity
            android:name=".ui.month.CalendarActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```
