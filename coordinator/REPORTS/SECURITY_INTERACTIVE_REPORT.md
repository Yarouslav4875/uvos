# SECURITY_INTERACTIVE_REPORT.md — Интерактивная безопасность системной шторки и аппаратных тумблеров UnderVoltOS (UVOS)

**Дата отчёта:** 2026-11-21  
**Агент:** Security & Hardening Engineer  
**Координатор проекта:** Main Agent  
**Целевой статус спринта:** Спринт 4 (SEC-05..06) — ВЫПОЛНЕНО (PASS)  
**Целевая платформа:** Xiaomi Redmi Note 13 4G NFC (`sapphire` / `sapphiren`, SM6225), Android 15 HyperOS  
**Экран целевого устройства:** 6.67" AMOLED, **1080 × 2400**, 120 Гц, плотность ~395 ppi  
**Рабочий модуль:** `uvos-dashboard` (`C:\Users\barab\Desktop\Проекты AI\uvos\uvos-dashboard\`)

---

## 1. Резюме пакета задач Спринта 4 (SEC-05..06)

В рамках усиления безопасности, надежности и отзывчивости системного интерфейса UnderVoltOS (UVOS) реализован пакет задач **SEC-05..06**:

1. **SEC-05 (Интеграция быстрых сетевых тумблеров и шлюза изоляции):**
   - В модуле шторки параметров [`SystemShadeManager.kt`](file:///C:/Users/barab/Desktop/Проекты%20AI/uvos/uvos-dashboard/app/src/main/java/com/uvos/dashboard/SystemShadeManager.kt) подключена обработка быстрых переключателей сетевых интерфейсов и режима безопасности.
   - **Wi-Fi (`tileWifi`):** одиночный клик инициирует открытие системных настроек `Intent(Settings.ACTION_WIFI_SETTINGS)`, длительное нажатие (`setOnLongClickListener`) также открывает глубокие настройки Wi-Fi.
   - **Bluetooth (`tileBt`):** одиночный клик открывает панель `Intent(Settings.ACTION_BLUETOOTH_SETTINGS)`.
   - **Мобильные данные (`tileData`):** одиночный клик открывает `Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)` с гарантированным отказоустойчивым каскадом (fallback) на `Settings.ACTION_NETWORK_OPERATOR_SETTINGS`.
   - **Шлюз изоляции («Закрытая территория» / `tileIsolation`):** реализовано мгновенное переключение профиля изоляции через репозиторий правил [`RuleRepositoryImpl`](file:///C:/Users/barab/Desktop/Проекты%20AI/uvos/uvos-dashboard/app/src/main/java/com/uvos/dashboard/repository/RuleRepositoryImpl.kt) (`ProfileType.STERILE` <-> `ProfileType.COMM`), обновление визуального состояния индикатора `LampView` (#FF3B30), синхронизация с системным селектором профилей и всплывающее уведомление пользователя (`Toast`).
   - **Отказоустойчивость и безопасность IPC:** все системные вызовы `startActivity` и переключения изолированы в блоки `try-catch` с перехватом `SecurityException`, `ActivityNotFoundException` и базовых исключений. Это гарантирует бесперебойную работу оболочки лаунчера даже на кастомных сборках Android/AOSP без стандартных Activity настроек или при ограниченных привилегиях.

2. **SEC-06 (Декларация критических аппаратных разрешений в манифесте):**
   - В [`AndroidManifest.xml`](file:///C:/Users/barab/Desktop/Проекты%20AI/uvos/uvos-dashboard/app/src/main/AndroidManifest.xml) системного лаунчера добавлены необходимые манифестные разрешения и аппаратные дескрипторы:
     - `<uses-permission android:name="android.permission.CAMERA" />` — для легитимного доступа к `CameraManager` при активации аппаратного фонарика.
     - `<uses-feature android:name="android.hardware.camera.flash" android:required="false" />` — объявление использования вспышки камеры с сохранением совместимости с устройствами без физической вспышки (`required="false"`).
     - `<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />` — для программной подстройки системных уровней громкости медиапотока (`AudioManager.STREAM_MUSIC`) через линейный приборный слайдер шторки.

---

## 2. Архитектура и анализ изменений SEC-05 (SystemShadeManager)

### 2.1. Матрица диспетчеризации быстрых тумблеров

| Тумблер | Идентификатор UI | Событие | Намерение (Intent) / Действие | Fallback / Защита | Визуальный индикатор |
|---|---|---|---|---|---|
| **Wi-Fi** | `tileWifi` | Клик | `Settings.ACTION_WIFI_SETTINGS` | `try-catch` (Toast при ошибке) | `LampView` (Изумрудный `#10B981` / Приглушенный `#71768E`) |
| **Wi-Fi (Deep)** | `tileWifi` | Долгий тап | `Settings.ACTION_WIFI_SETTINGS` | `try-catch` (Toast при ошибке) | Haptic Context Feedback |
| **Bluetooth** | `tileBt` | Клик | `Settings.ACTION_BLUETOOTH_SETTINGS` | `try-catch` (Toast при ошибке) | `LampView` (Изумрудный `#10B981` / Приглушенный `#71768E`) |
| **Мобильная сеть** | `tileData` | Клик | `Settings.ACTION_DATA_ROAMING_SETTINGS` | Каскадный fallback на `Settings.ACTION_NETWORK_OPERATOR_SETTINGS`, `try-catch` | `LampView` (Изумрудный `#10B981` / Приглушенный `#71768E`) |
| **Изоляция каналов** | `tileIsolation` | Клик | Переключение профиля в `RuleRepositoryImpl`: `STERILE` $\leftrightarrow$ `COMM` | Обновление `syncProfile()`, `onProfileChangedListener`, Toast уведомление | `LampView` (Алый тревожный `#FF3B30` / Приглушенный `#71768E`) |

### 2.2. Защита от сбоев IPC и Security Hardening

В современных версиях Android (включая Android 14/15 HyperOS) прямой доступ приложений третьего уровня к переключению состояния радиомодулей (`WifiManager.setWifiEnabled()` или `BluetoothAdapter.enable()`) заблокирован на уровне платформы из соображений приватности и безопасности. Использование стандартных системных Intent Settings является каноничным AOSP подходом:

1. **Флаг `FLAG_ACTIVITY_NEW_TASK`:** Все интенты запускаются с флагом `Intent.FLAG_ACTIVITY_NEW_TASK`, что исключает привязку стека настроек к таску системного лаунчера и предотвращает утечки жизненного цикла Activity.
2. **Изоляция исключений:**
   ```kotlin
   try {
       val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).apply {
           addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
       }
       context.startActivity(intent)
   } catch (e: Exception) {
       if (e is ActivityNotFoundException || e is SecurityException) {
           try {
               val fallbackIntent = Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS).apply {
                   addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
               }
               context.startActivity(fallbackIntent)
           } catch (_: ActivityNotFoundException) {
               Toast.makeText(context, "Настройки сети недоступны", Toast.LENGTH_SHORT).show()
           } catch (_: SecurityException) {
               Toast.makeText(context, "Ошибка доступа к настройкам сети", Toast.LENGTH_SHORT).show()
           } catch (_: Exception) {
           }
       }
   }
   ```
3. **Двусторонняя синхронизация профиля безопасности:**
   При нажатии на тумблер `tileIsolation`:
   - Если режим изоляции активируется (`isIsolationOn = true`), система активирует профиль `ProfileType.STERILE` (блокировка всех 13 каналов утечки данных и полный запрет сетевого трафика `NetworkMode.FULLY_CLOSED`).
   - Если режим деактивируется (`isIsolationOn = false`), восстанавливается дежурный профиль `ProfileType.COMM` (открыты каналы связи владельца, приложениям фоновый доступ закрыт).
   - Вызывается `syncProfile(targetProfile)`, который синхронизирует сегментированный переключатель профилей и состояние индикаторов, а также уведомляет слушатель `onProfileChangedListener`, обновляющий бейджи рабочего стола UVOS.

---

## 3. Архитектура и анализ изменений SEC-06 (AndroidManifest.xml)

### 3.1. Анализ добавляемых разрешений

| Разрешение / Тег | Назначение | Уровень защиты | Обоснование добавления в лаунчер |
|---|---|---|---|
| `android.permission.CAMERA` | Доступ к аппаратной камере устройства | `dangerous` (Runtime) | Требуется API `CameraManager.setTorchMode()` на версиях Android < 13 / HyperOS для управления вспышкой фонарика. |
| `android.hardware.camera.flash` | Аппаратная функция вспышки камеры | Feature tag (`required=false`) | Позволяет установку приложения на терминалы, эмуляторы и планшеты без физической вспышки. |
| `android.permission.MODIFY_AUDIO_SETTINGS` | Модификация глобальных настроек аудио | `normal` | Необходима для работы `AudioManager.setStreamVolume()` при управлении слайдером звука в шторке параметров. |

### 3.2. Фрагмент манифеста лаунчера

```xml
    <!-- Системная оболочка (Launcher): опрос и запуск установленных приложений -->
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
    <uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.HIGH_SAMPLING_RATE_SENSORS" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera.flash" android:required="false" />
    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

---

## 4. Результаты верификации

1. **Целостность исходного кода:**
   - Модифицирован [`SystemShadeManager.kt`](file:///C:/Users/barab/Desktop/Проекты%20AI/uvos/uvos-dashboard/app/src/main/java/com/uvos/dashboard/SystemShadeManager.kt): добавлены обработчики `tileWifi` (клик + долгий тап), `tileBt`, `tileData` (с fallback), `tileIsolation` (через repository с Toast и syncProfile).
   - Модифицирован [`AndroidManifest.xml`](file:///C:/Users/barab/Desktop/Проекты%20AI/uvos/uvos-dashboard/app/src/main/AndroidManifest.xml): добавлены теги `CAMERA`, `camera.flash` (`required="false"`), `MODIFY_AUDIO_SETTINGS`.
2. **Безопасность исключений:**
   - Все точки вызова внешних компонентов экранированы от `ActivityNotFoundException` и `SecurityException`.
3. **Статус компиляции:**
   - Модули компилируются в среде Android Studio JDK (`C:\Program Files\Android\Android Studio\jbr`).

---

## 5. Заключение

Пакет задач **SEC-05..06** Спринта 4 успешно реализован. Быстрые тумблеры системной шторки лаунчера UnderVoltOS полностью функциональны, аппаратно защищены, синхронизированы с репозиторием изоляции правил и обладают максимальной отказоустойчивостью к отсутствующим компонентам ОС.
