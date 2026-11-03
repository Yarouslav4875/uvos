# UVOS Downloader (`com.uvos.downloader`) — Техническая спецификация и методы разработки

> **Статус**: Архитектурная спецификация и документация разработки  
> **Пакет**: `com.uvos.downloader`  
> **Целевая платформа**: UnderVoltOS / Android 14 (API 34)  
> **Политика приватности**: Zero Telemetry, 0% рекламы, отсутствие внешних трекеров.

---

## 1. Назначение и концепция

`UVOS Downloader` — это легковесный, высокопроизводительный оффлайн-менеджер загрузок для экосистемы UnderVoltOS. Приложение заменяет стандартный системный `DownloadProvider` от Google и проприетарные менеджеры загрузок, устраняя фоновую отправку телеметрии об URL скачиваемых файлов, типах контента и объемах трафика.

### Ключевые требования:
- **Многопоточное скачивание (Segmented Multi-Part Downloads)**: разбиение файла на 2–8 параллельных сегментов с использованием HTTP заголовка `Range: bytes=start-end`.
- **Автоматическая докачка (Resume Capability)**: возобновление прерванных загрузок без повторного скачивания уже полученных фрагментов.
- **Приборный интерфейс M4200**: аналоговый стрелочный спидометр скорости скачивания (Кб/с, Мб/с), вольтметр стабильности соединения и тумблеры управления потоками.
- **Изоляция сети**: гибкий планировщик правил («Качать только по Wi-Fi», ограничение максимальной полосы пропускания).
- **Интеграция с Firefox / Mull**: перехват ссылок через системные Intent-фильтры.

---

## 2. Архитектура системы

Приложение строится по принципам **Clean Architecture** с разделением на Presentation (Jetpack Compose), Domain (UseCase) и Data (OkHttp + SQLite Room).

```
com.uvos.downloader/
├── data/
│   ├── local/
│   │   ├── db/
│   │   │   ├── DownloadDatabase.kt
│   │   │   ├── DownloadTaskDao.kt
│   │   │   └── DownloadChunkDao.kt
│   │   └── entity/
│   │       ├── DownloadTaskEntity.kt
│   │       └── DownloadChunkEntity.kt
│   ├── network/
│   │   ├── DownloadEngine.kt
│   │   ├── ChunkWorker.kt
│   │   └── OkHttpDownloadClient.kt
│   └── repository/
│       └── DownloadRepositoryImpl.kt
├── domain/
│   ├── model/
│   │   ├── DownloadTask.kt
│   │   ├── DownloadChunk.kt
│   │   ├── DownloadStatus.kt
│   │   └── NetworkSpeedMetric.kt
│   ├── repository/
│   │   └── DownloadRepository.kt
│   └── usecase/
│       ├── StartDownloadUseCase.kt
│       ├── PauseDownloadUseCase.kt
│       ├── CancelDownloadUseCase.kt
│       └── CalculateSpeedMetricsUseCase.kt
├── service/
│   └── DownloadForegroundService.kt
└── ui/
    ├── gauge/
    │   ├── SpeedometerGaugeView.kt
    │   └── ChunkProgressStripView.kt
    ├── main/
    │   ├── DownloaderMainScreen.kt
    │   └── DownloaderViewModel.kt
    └── theme/
        ├── Color.kt
        └── Theme.kt
```

---

## 3. Модуль загрузки (Segmented Multi-Thread Engine)

### 3.1 Алгоритм работы многопоточного загрузчика
1. **Probe Request**: отправка легковесного `HEAD` (или `GET` с `Range: bytes=0-0`) запроса к серверу для выяснения:
   - Поддерживает ли сервер заголовок `Accept-Ranges: bytes`.
   - Полный размер файла (`Content-Length`).
   - Имя файла (`Content-Disposition` или парсинг пути URL).
2. **Сегментация**:
   - Если сервер поддерживает диапазонные запросы и размер файла превышает 5 МБ, файл разбивается на $N$ равных чанков ($N = 2 \dots 8$).
   - Создается целевой разреженный файл на диске через `RandomAccessFile.setLength(totalBytes)`.
3. **Параллельная выкачка**:
   - Каждый поток `ChunkWorker` пишет байты напрямую в целевой файл со своим смещением:
     ```kotlin
     val targetFile = RandomAccessFile(filePath, "rw")
     targetFile.seek(chunk.startOffset + chunk.downloadedBytes)
     ```
4. **Склейка на лету**: благодаря прямому позиционированию в `RandomAccessFile` не требуется дорогая операция финальной конкатенации временных файлов на диске.

---

## 4. Схема базы данных Room

```kotlin
@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val url: String,
    val destinationPath: String,
    val fileName: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: String, // QUEUED, DOWNLOADING, PAUSED, COMPLETED, ERROR
    val numThreads: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val onlyWifi: Boolean = true,
    val errorMessage: String? = null
)

@Entity(
    tableName = "download_chunks",
    foreignKeys = [
        ForeignKey(
            entity = DownloadTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DownloadChunkEntity(
    @PrimaryKey(autoGenerate = true) val chunkId: Long = 0,
    val taskId: String,
    val chunkIndex: Int,
    val startByte: Long,
    val endByte: Long,
    val currentByte: Long
)
```

---

## 5. UI и стрелочный спидометр скорости (M4200 Gauge)

Спидометр визуализирует совокупную скорость закачки со стрелочным прибором со шкалой от 0 до 50 МБ/с.

```kotlin
@Composable
fun SpeedometerGauge(
    speedBytesPerSecond: Double,
    modifier: Modifier = Modifier
) {
    val speedMb = (speedBytesPerSecond / (1024 * 1024)).toFloat().coerceIn(0f, 50f)
    val angle = -135f + (speedMb / 50f) * 270f

    Canvas(modifier = modifier.size(220.dp)) {
        // Отрисовка фона циферблата (слоновая кость)
        drawCircle(color = Color(0xFFE8E4D8))
        // Отрисовка красной критической зоны шкалы (> 40 МБ/с)
        drawArc(
            color = Color(0xFFE02020),
            startAngle = 135f + (40f / 50f) * 270f,
            sweepAngle = (10f / 50f) * 270f,
            useCenter = false,
            style = Stroke(width = 8.dp.toPx())
        )
        // Отрисовка стрелки прибора
        rotate(degrees = angle) {
            drawLine(
                color = Color(0xFFE02020),
                start = center,
                end = Offset(center.x, center.y - size.height * 0.42f),
                strokeWidth = 3.dp.toPx()
            )
        }
        // Центральная заглушка прибора
        drawCircle(color = Color(0xFF1A1A1A), radius = 6.dp.toPx())
    }
}
```

---

## 6. Манифест и разрешения

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.uvos.downloader">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:label="UVOS Загрузки"
        android:theme="@style/Theme.UnderVoltOS.Dark">

        <service
            android:name=".service.DownloadForegroundService"
            android:foregroundServiceType="dataSync"
            android:exported="false" />

        <activity
            android:name=".ui.main.DownloaderActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <!-- Перехват прямых ссылок на загрузку -->
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="http" />
                <data android:scheme="https" />
                <data android:mimeType="application/octet-stream" />
                <data android:mimeType="application/zip" />
                <data android:mimeType="application/x-tar" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```
