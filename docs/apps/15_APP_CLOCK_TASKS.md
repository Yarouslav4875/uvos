# UVOS Clock & Tasks (`com.uvos.clock` / `com.uvos.tasks`) — Техническая спецификация и методы разработки

> **Статус**: Архитектурная спецификация и документация разработки  
> **Пакеты**: `com.uvos.clock` (Часы, Секундомер, Таймер, Будильник) и `com.uvos.tasks` (Напоминания и Задачи)  
> **Целевая платформа**: UnderVoltOS / Android 14 (API 34)  
> **Политика приватности**: Zero Telemetry, 0% рекламы, отсутствие разрешения `INTERNET`.

---

## 1. Назначение и концепция

Комплекс приложений времени и задач UnderVoltOS заменяет стандартные часы и напоминания Google/MIUI. Он реализует:
- Полностью локальное управление временем и задачами без облачных служб Google Tasks.
- Аналоговый приборный хронограф с высокой точностью.
- Плавное аналоговое нарастание сигнала будильника, исключающее стрессовый резкий звонок.
- Матрицу задач Эйзенхауэра (Срочно/Важно) с локальными пуш-уведомлениями.

---

## 2. Архитектура подсистемы времени (`com.uvos.clock`)

### 2.1 Компоненты часов
- **Хронометр/Секундомер**: физическое моделирование стрелочного секундомера с делениями 1/100 секунды и поддержкой кругов (Laps).
- **Таймер**: круговой регулятор времени (Knob Control) с обратным отсчетом на базе `ForegroundService` с постоянным отображением в шторке.
- **Будильник**: системный планировщик `AlarmManager.setAlarmClock()`, гарантирующий пробуждение процессора из режима Deep Sleep (Doze Mode).

```kotlin
@Composable
fun AnalogStopwatchDial(
    elapsedMillis: Long,
    modifier: Modifier = Modifier
) {
    val seconds = (elapsedMillis / 1000) % 60
    val millis = (elapsedMillis % 1000)
    val secondAngle = -90f + (seconds + millis / 1000f) * 6f

    Canvas(modifier = modifier.size(240.dp)) {
        // Фоновый диск прибора M4200
        drawCircle(color = Color(0xFFE8E4D8))
        // 60 минутных делений
        for (i in 0 until 60) {
            val angle = i * 6f
            val isMajor = i % 5 == 0
            val lineLen = if (isMajor) 14.dp.toPx() else 7.dp.toPx()
            val strokeW = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
            rotate(degrees = angle) {
                drawLine(
                    color = Color(0xFF1A1A1A),
                    start = Offset(center.x, center.y - size.height * 0.45f),
                    end = Offset(center.x, center.y - size.height * 0.45f + lineLen),
                    strokeWidth = strokeW
                )
            }
        }
        // Стрелка секундомера (акцентный красный)
        rotate(degrees = secondAngle) {
            drawLine(
                color = Color(0xFFE02020),
                start = center,
                end = Offset(center.x, center.y - size.height * 0.43f),
                strokeWidth = 2.dp.toPx()
            )
        }
        drawCircle(color = Color(0xFF1A1A1A), radius = 5.dp.toPx())
    }
}
```

---

## 3. Архитектура подсистемы задач и напоминаний (`com.uvos.tasks`)

### 3.1 Схема базы данных Room для задач
```kotlin
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String?,
    val priority: Int, // 1: Критично, 2: Важно, 3: Обычное, 4: Низкий
    val isCompleted: Boolean = false,
    val dueTimestamp: Long?,
    val reminderTimestamp: Long?,
    val createdAt: Long = System.currentTimeMillis()
)
```

### 3.2 Матрица Эйзенхауэра в UI
Задачи группируются в 4 приборные плитки:
1. **Срочно и Важно** (Индикатор `LampView` красный);
2. **Важно, но не срочно** (Индикатор `LampView` жёлтый);
3. **Срочно, но не важно** (Индикатор `LampView` оранжевый);
4. **Не срочно и не важно** (Индикатор `LampView` серый).

---

## 4. Манифест и разрешения

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.uvos.clock">

    <!-- Полный оффлайн-инвариант: INTERNET ОТСУТСТВУЕТ -->
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.USE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:label="UVOS Часы и Задачи"
        android:theme="@style/Theme.UnderVoltOS.Dark">

        <receiver
            android:name=".receiver.AlarmBroadcastReceiver"
            android:exported="false">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.TIME_SET" />
                <action android:name="android.intent.action.TIMEZONE_CHANGED" />
            </intent-filter>
        </receiver>

        <activity
            android:name=".ui.main.ClockTasksActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```
