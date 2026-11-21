# UVOS Multi-Agent Coordination Board
Статус: АКТИВЕН  
Дата координации: 2026-11-19  
Координатор: Main Agent  
Рабочая директория: `C:\Users\barab\Desktop\Проекты AI\uvos`

---

## 1. Реестр агентов и их специализация

| Chat ID | Роль агента | Специализация / Фокус | Текущий статус |
| :--- | :--- | :--- | :--- |
| **`ecd50c90-7bc9-4aed-afc1-f66b718f9c81`** | **Lead Developer (Core & Apps)** | Реализация модулей, интеграция кода, коммиты, Gradle | Выполнил DEV-01..03 (WallpaperManager, IconPackManager, Customization Dialog, APK собран) |
| **`0c547d7b-340f-47db-ae40-a3d3649e75c1`** | **UI/UX Designer (M4200 UI)** | Макеты, дизайн-система, рендеринг приборов, презентации | Сдал макеты Dark Glassmorphism и пресеты обоев (UI-01..03, REPORTS/UI_GLASSMORPHISM_REPORT.md) |
| **`75271c78-7409-4035-85d4-c1b832e933ac`** | **Security & Hardening Engineer** | Аудит iptables, AppOps, Root su, поиск уязвимостей | Выполнил SEC-02..04 (Аудит манифестов 15 приложений, AppLauncher Intent Hardening, DAC 0600 изоляция, сдал REPORTS/SECURITY_ECOSYSTEM_REPORT.md) |
| **`ff2b50cf-15ae-4192-959b-38a87446c3cb`** | **Performance & 120Hz Optimizer** | Профайлинг Compose, 120 FPS рендеринг, удаление мусора | Отчёт сдан (PERF_120HZ_REPORT.md) |

---

## 2. Распределение целевых задач (Sprint Tasks)

### 👨‍💻 Агент 1: Lead Developer (`ecd50c90-7bc9-4aed-afc1-f66b718f9c81`)
**Блок:** Валидация сборки и покрытие Unit-тестами бизнес-логики.
- [x] Настроить сборочный тулчейн: сертифицирован стабильный JDK 21 LTS (`C:\Users\barab\UVOS\Java\jdk-21.0.13+9`) из-за несовместимости AGP 8.7.3 с Java 25.
- [x] Добавить базовые JUnit-тесты для критических сервисов:
  - `uvos-security`: правила файрвола, валидация пакетов (7 тестов, PASSED).
  - `uvos-files`: операции с путями, root-команды, защита от шелл-инъекций (5 тестов, PASSED).
  - `uvos-vpn`: конфигурация WireGuard интерфейса, MTU и IP валидация, kill-switch (5 тестов, PASSED).
- [x] Зафиксировать результаты сборки в отчёте `coordinator/REPORTS/DEV_BUILD_REPORT.md`.

### 🎨 Агент 2: UI/UX Designer (`0c547d7b-340f-47db-ae40-a3d3649e75c1`)
**Блок:** Аудит консистентности дизайн-системы M4200 в Jetpack Compose.
- [x] Построчно сверить цветовую палитру `#0E0E12`, `#1C1E26`, `#E02020`, `#E8E4D8` во всех 15 модулях `apps/*/app/src/main/res/` и `Theme.kt`.
- [x] Проверить кастомные Canvas-компоненты стрелочных приборов (`GaugeView`, `VU-meter`, `Speedometer`) во всех модулях:
  - `uvos-music` (стерео VU-метры L/R).
  - `uvos-downloader` (спидометр загрузки).
  - `uvos-terminal` (амперметр CPU/RAM).
  - `uvos-security` (вольтметр фоновой тревоги).
- [x] Создать отчёт по консистентности интерфейса: `coordinator/REPORTS/UI_AUDIT_REPORT.md`.

### 🛡️ Агент 3: Security Engineer (`75271c78-7409-4035-85d4-c1b832e933ac`)
**Блок:** Тестирование векторов атак и валидация «Zero Telemetry Invariant».
- [x] Просканировать все `AndroidManifest.xml` во всех 15 модулях на предмет случайного включения `android.permission.INTERNET` в оффлайн-приложениях (Заметки, Календарь, Часы, Галерея, Калькулятор, Файлы).
- [x] Проверить скрипт `scripts/debloat_purge_telemetry.ps1` и `.sh` на полноту списка пакетов Xiaomi/HyperOS.
- [x] Проверить реализацию Root execution (`libsu` / `su shell`) в `uvos-terminal` и `uvos-security` на предмет command injection (экранирование аргументов).
- [x] Сформировать финальный отчёт безопасности: `coordinator/REPORTS/SECURITY_AUDIT_REPORT.md`.

### ⚡ Агент 4: 120Hz Optimizer (`ff2b50cf-15ae-4192-959b-38a87446c3cb`)
**Блок:** Оптимизация производительности рендеринга и рекомпозиций Jetpack Compose.
- [x] Проверить рекомпозиции в `Custom Canvas` стрелочных приборов (использование `drawBehind`, `graphicsLayer`, кэширование `Path` и `Paint`, отсутствие аллокаций объектов внутри цикла отрисовки `onDraw`).
- [x] Проверить списки `LazyColumn` / `LazyVerticalGrid` во всех приложениях на наличие стабильных ключей `key = { ... }`.
- [x] Проверить фоновые корутины (отсутствие блокировок главного UI-потока `Dispatchers.Main` при чтении датчиков и дисков).
- [x] Сформировать отчёт по оптимизации: `coordinator/REPORTS/PERF_120HZ_REPORT.md`.

---

## 3. Деплой и решение проблем на реальном устройстве (Redmi Note 13 4G)

### 📱 Устройство: Xiaomi Redmi Note 13 4G NFC (`sapphiren` / `c0691046`), Android 15 HyperOS

- [x] **Деплой UVOS Dashboard:**
  - Собран `app-debug.apk` (1.06 МБ) с чистой оптимизированной сборкой.
  - Удалена старая несовместимая версия с другим debug-ключом подписи.
  - Успешно установлен пакет `com.uvos.dashboard` через ADB (`Streamed Install Success`).
  - Приложение запущено и активно на экране (`MainActivity`, visibility=0).

- [x] **Устранение зависания профиля безопасности («профиль безопасности подвисает»):**
  1. **Ликвидирован рекурсивный цикл событий переключения профилей:**
     - В `SegmentedControl.kt` добавлен флаг `notify: Boolean = false` в метод `setSelectedIndex`.
     - При программной синхронизации профиля со шторкой `SystemShadeManager` слушатель больше не вызывается повторно, исключая цикл перезаписи и двойной рендеринг.
  2. **Асинхронное сохранение правил (`saveToDiskAsync`):**
     - Сериализация и запись JSON в `RuleRepositoryImpl.kt` переведены на выделенный однопоточный исполнитель `ioExecutor`.
     - Главный поток освобожден от дисковых блокировок при выборе профилей «СТЕРИЛЬНО» / «СВЯЗЬ» / «СВОЙ».
  3. **Мгновенное обновление бейджей (`updateDrawerBadges`):**
     - Ликвидировано полное пересоздание и инфлейт всех 50+ представлений меню приложений на главном потоке.
     - Добавлено кэширование иконок `ConcurrentHashMap<String, Drawable>` и метод `updateDrawerBadges()`, обновляющий только бейджи за 0.2 мс вместо 2.5 сек.
  4. **Вынос сбора системной телеметрии из UI-потока:**
     - Методы чтения `/proc/stat`, `/proc/net/dev`, `sysfs thermal` и `StatFs` вынесены в `collectAndDispatchTelemetry()` на фоновом исполнителе.
     - На UI-поток через `runOnUiThread` передаются только готовые скалярные значения для стрелочных индикаторов.
  5. **Аппаратные замеры (`dumpsys gfxinfo`):**
     - 50-й перцентиль UI: **13 мс**, GPU: **6 мс**.
     - Зависания и лаги при переключении профилей безопасности полностью устранены.

---

## 4. Спринт 2: Таблица распределения задач (UI Acceleration, Dark Glassmorphism, Theme & Fluid Motion)

| ID задачи | Ответственный агент | Роль | Модуль / Файлы | Задача и декомпозиция | Критерий приёмки (Done) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **PERF-01** | `ff2b50cf-15ae-4192-959b-38a87446c3cb` | **120Hz Fluid Motion Optimizer** | `MainActivity.kt`, `DesktopPagerView.kt` | **Устранение `requestLayout()` при листании:** Заменить мутацию `layoutParams` в индикаторах точек страниц на `translationX`/`scaleX` и проверку `if (currentActiveDotPage == page) return`. | ✅ **Выполнено:** 0 вызовов `requestLayout` во время свайпа столов (см. `REPORTS/PERF_SPRINT2_REPORT.md`). |
| **PERF-02** | `ff2b50cf-15ae-4192-959b-38a87446c3cb` | **120Hz Fluid Motion Optimizer** | `DesktopPagerView.kt` | **Оптимизация пагинатора и пружинная физика:** Убрать `child.alpha` для сложных контейнеров столов (предотвратить создание `RenderNodeLayer` на Adreno 610). Внедрить адаптивную пружинную физику с затуханием (Damped Spring). | ✅ **Выполнено:** Damped Spring внедрен, `child.alpha` исключен, RenderNodeLayer в VRAM устранены. |
| **PERF-03** | `ff2b50cf-15ae-4192-959b-38a87446c3cb` | **120Hz Fluid Motion Optimizer** | `activity_main.xml`, `MainActivity.kt` | **Ленивая загрузка столов (Lazy Desktops):** Отложенный инфлейт Стола 03 (Drawer) и Стола 04 (Process Manager) до первого перехода на них. | ✅ **Выполнено:** Снижение активного дерева представлений с 1057 до 343 View (подтверждено dumpsys gfxinfo). |
| **UI-01** | `0c547d7b-340f-47db-ae40-a3d3649e75c1` | **UI/UX Designer** | `res/drawable/`, `GlassTileDrawable.kt` | **Генератор стеклянных иконок Dark Glassmorphism:** Реализовать подложку тёмного стекла (Frosted Glass), зеркальную фаску (specular rim) и алый неон (`#FF3B30`/`#E02020`) по референсу `glass_icons_reference.jpg`. | ✅ **Выполнено:** Реализован GlassTileDrawable и XML layer-lists (см. `REPORTS/UI_GLASSMORPHISM_REPORT.md`). |
| **UI-02** | `0c547d7b-340f-47db-ae40-a3d3649e75c1` | **UI/UX Designer** | `res/drawable/ic_glass_*` | **Пакет векторных иконок:** Создать 12 базовых иконок: Телефон, Чат, Браузер/Радар, Камера, Файлы, Плеер, Настройки, Сейф, Терминал, Ракета, Сеть, Щит. | ✅ **Выполнено:** 12 векторных иконок ic_glass_* внедрены в res/drawable и отрендерен шоукейс. |
| **UI-03** | `0c547d7b-340f-47db-ae40-a3d3649e75c1` | **UI/UX Designer** | `res/drawable/bg_wallpaper_*` | **Пресеты обоев и димминг:** Подготовить пресеты «Deep Void» (`#090A0E`), «M4200 Tactical Grid» (координатная сетка), «Crimson Horizon» (тёмный графит с алой полосой). | ✅ **Выполнено:** 3 XML пресета обоев и 3 PNG превью в 1080x2400 подготовлены. |
| **DEV-01** | `ecd50c90-7bc9-4aed-afc1-f66b718f9c81` | **Lead Developer** | `WallpaperManager.kt`, `MainActivity.kt` | **Движок обоев (Wallpaper Engine):** Реализовать выбор встроенных пресетов + загрузку изображения из галереи (SAF) с сохранением в `SharedPreferences`. | ✅ **Выполнено:** Реализован `WallpaperManager.kt`, процедурные пресеты («Тактическая сетка», «Карбон», «Радар-сталь», AMOLED), безопасный SAF импорт с даунсэмплингом и димминг 0-80%. |
| **DEV-02** | `ecd50c90-7bc9-4aed-afc1-f66b718f9c81` | **Lead Developer** | `IconPackManager.kt`, `HomeManager.kt` | **Движок переключения наборов иконок:** Поддержка стилей: 1) Dark Glassmorphism, 2) Retro M4200, 3) Системные. Мгновенное применение к сетке стола. | ✅ **Выполнено:** Реализован `IconPackManager.kt`, интеграция `GlassTileDrawable`, векторные `ic_glass_*`, горячая смена на лету без перезапуска Activity. |
| **DEV-03** | `ecd50c90-7bc9-4aed-afc1-f66b718f9c81` | **Lead Developer** | `dialog_desktop_customization.xml` | **Диалог кастомизации:** Окно по долгому тапу на экран с вкладками «ОБОИ» (пресеты, галерея, ползунок затемнения) и «ИКОНКИ». | ✅ **Выполнено:** Создан диалог `DesktopCustomizationDialog.kt` и разметка `dialog_desktop_customization.xml`, синхронизация со слоями `MainActivity`. |
| **SEC-01** | `75271c78-7409-4035-85d4-c1b832e933ac` | **Security Engineer** | `WallpaperManager.kt` | **Песочница и безопасность медиа:** Чтение пользовательских файлов через `content://` URI без избыточных системных прав и даунсэмплинг под 1080x2400 (защита от OOM). | ✅ **Сдан отчёт безопасности:** `REPORTS/SECURITY_MEDIA_REPORT.md` |

---

## 5. Готовые директивы для передачи агентам (Prompts for External Chats)

### 📌 Директива для Агента 4 (`ff2b50cf-15ae-4192-959b-38a87446c3cb`):
```text
Задача PERF-01..03: Оптимизация 120 FPS Fluid Motion в uvos-dashboard.
1. В MainActivity.kt перепиши updatePageIndicators: устрани вызовы dot.layoutParams во время свайпа (используй scaleX/translationX и флаг if (currentActiveDotPage == page) return).
2. В DesktopPagerView.kt удали child.alpha на уровне View-групп столов во время скролла для исключения off-screen RenderNodeLayer аллокаций на GPU Adreno 610.
3. Добавь отложенную инициализацию (Lazy loading) содержимого Стола 03 (Drawer) и Стола 04 (Process Manager), чтобы сократить активное дерево представлений с 1057 до ~180 View.
Сдай отчёт с замерами gfxinfo.
```

### 📌 Директива для Агента 2 (`0c547d7b-340f-47db-ae40-a3d3649e75c1`):
```text
Задача UI-01..03: Разработка стиля иконок Dark Glassmorphism и пресетов обоев для uvos-dashboard.
1. Ознакомься с референсом пользователя: тёмное матовое полупрозрачное стекло с зеркальным бликом по контуру и сочным алым неоном (#FF3B30 / #E02020).
2. Реализуй GlassTileDrawable и векторные иконки ic_glass_* для 12 ключевых приложений UVOS (Phone, Chat, Radar, Camera, Files, Music, Settings, Vault, Terminal, Downloader, Cloud, Security).
3. Создай 3 пресета фонов: «Deep Void» (#090A0E), «M4200 Tactical Grid» (приборная координатная сетка) и «Crimson Horizon».
Сдай отчёт и макеты.
```

### 📌 Директива для Агента 1 (`ecd50c90-7bc9-4aed-afc1-f66b718f9c81`):
```text
Задача DEV-01..03: Реализация WallpaperManager, IconPackManager и диалога кастомизации.
1. Создай WallpaperManager: сохранение выбранного фона (3 системных пресета + пользовательское фото через Storage Access Framework) и уровня димминга (0-80%).
2. Создай IconPackManager: поддержка наборов DARK_GLASS, RETRO_M4200, SYSTEM_DEFAULT с обновлением HomeManager и сетки ярлыков без перезапуска MainActivity.
3. Реализуй всплывающий диалог кастомизации стола с вкладками «ОБОИ» и «ИКОНКИ».
Собери и протестируй APK.
```

---

## 6. Спринт 3: Интеграция экосистемы приложений, полный комплект Dark Glassmorphism и IPC безопасность

### 📋 Таблица задач Спринта 3

| ID задачи | Ответственный агент | Роль | Модуль / Компоненты | Задача и декомпозиция | Критерий приёмки (Done) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **DEV-04** | `ecd50c90-7bc9-4aed-afc1-f66b718f9c81` | **Lead Developer** | `IconPackManager.kt`, `HomeManager.kt` | **Маппинг всех 15 нативных приложений UVOS:** Расширить `IconPackManager` привязкой векторных иконок к 15 приложениям (`uvos-calendar`, `camera`, `clock`, `compass`, `downloader`, `files`, `gallery`, `mail`, `messages`, `music`, `notes`, `security`, `settings`, `terminal`, `vpn`). | ✅ **Выполнено:** Все 15 ярлыков стола получают нативные Glass-иконки. |
| **DEV-05** | `ecd50c90-7bc9-4aed-afc1-f66b718f9c81` | **Lead Developer** | `AppLauncher.kt`, `MainActivity.kt` | **Подсистема запуска и Intent fallback:** Реализовать `AppLauncher` с безопасной отправкой Intent в нативные модули UVOS и fallback-переходом на системные стандартные приложения AOSP/HyperOS при отсутствии установленного модуля. | ✅ **Выполнено:** Создан `AppLauncher.kt`, безопасная маршрутизация с fallback без `ActivityNotFoundException`. |
| **DEV-06** | `ecd50c90-7bc9-4aed-afc1-f66b718f9c81` | **Lead Developer** | `WallpaperManager.kt` | **Управление жизненным циклом Bitmap:** Внедрить `bitmap.recycle()` и сброс ссылок при повторной установке кастомных обоев для предотвращения утечек Native Bitmap памяти. | ✅ **Выполнено:** Внедрен `bitmap.recycle()` и освобождение памяти, утечки Native Heap ликвидированы. |
| **UI-04** | `0c547d7b-340f-47db-ae40-a3d3649e75c1` | **UI/UX Designer** | `res/drawable/ic_glass_*` | **Завершение комплекта иконок Dark Glassmorphism (15/15):** Отрисовать недостающие векторные иконки: `ic_glass_compass.xml`, `ic_glass_mail.xml`, `ic_glass_notes.xml`, `ic_glass_calendar.xml` в строгом соответствии с базовым стилем (матовое стекло + зеркальный блик + алый неон `#FF3B30`). | ✅ **Выполнено:** 4 недостающие векторные иконки созданы и протестированы (100% покрытие 15 приложений). |
| **UI-05** | `0c547d7b-340f-47db-ae40-a3d3649e75c1` | **UI/UX Designer** | `res/drawable/`, `activity_main.xml` | **Стеклянный стиль системных баров (Dock & Status Bar):** Реализовать подложку `bg_glass_dock.xml` для нижнего дока и разделителей с мягким размытием и фаской. | ✅ **Выполнено:** Создана подложка `bg_glass_dock.xml` с матовым стеклом, фаской и неоновым рефлексом. |
| **UI-06** | `0c547d7b-340f-47db-ae40-a3d3649e75c1` | **UI/UX Designer** | `coordinator/REPORTS/` | **Гайдлайн дизайн-токенов экосистемы:** Оформить `UI_DESIGN_SYSTEM_TOKENS.md` с точными спецификациями цветов, радиусов (16dp/24dp) и неоновых акцентов для авторов 15 приложений. | ✅ **Выполнено:** Гайдлайн и документация токенов оформлены в `UI_DESIGN_SYSTEM_TOKENS.md`. |
| **PERF-04** | `ff2b50cf-15ae-4192-959b-38a87446c3cb` | **120Hz Fluid Motion Optimizer** | `BatteryQuadWidgetView.kt`, `SensorTelemetryManager.kt` | **Профайлинг Canvas виджетов телеметрии:** Устранить перерисовки неактивных слоёв в `BatteryQuadWidgetView`, кэшировать градиенты и `Paint`, вынести вычисления из `onDraw()`. | ✅ **Выполнено:** Ликвидированы все аллокации в onDraw(), рендер виджетов 0.32 мс (норматив < 2.0 мс). |
| **PERF-05** | `ff2b50cf-15ae-4192-959b-38a87446c3cb` | **120Hz Fluid Motion Optimizer** | `MainActivity.kt`, `res/anim/` | **Анимации перехода запуска приложений (120 FPS Transitions):** Настроить аппаратные масштабные и альфа-переходы (`ActivityOptions.makeCustomAnimation`) с кривой интерполяции FastOutSlowIn для бесшовного запуска на экранах 120 Гц. | ✅ **Выполнено:** Внедрен бесшовный переход makeScaleUpAnimation и тактильный пружинный отклик. |
| **PERF-06** | `ff2b50cf-15ae-4192-959b-38a87446c3cb` | **120Hz Fluid Motion Optimizer** | `coordinator/REPORTS/` | **Аппаратный стресс-тест лаунчера:** Провести замеры частоты кадров при непрерывном 60-секундном скроллинге столов и переключении тем, зафиксировать перцентили в `PERF_SPRINT3_REPORT.md`. | ✅ **Выполнено:** Зафиксирован результат в PERF_SPRINT3_REPORT.md: 99-й перцентиль < 8.3 мс. |
| **SEC-02** | `75271c78-7409-4035-85d4-c1b832e933ac` | **Security Engineer** | `apps/*/AndroidManifest.xml` | **Аудит IPC и экспортированных компонентов:** Проверить манифесты всех 15 приложений в `apps/` на атрибут `android:exported="false"` для непубличных Activity, Service и BroadcastReceiver. | ✅ **Выполнено:** 100% манифестов проверены, непубличные компоненты закрыты (`exported="false"`), системные точки входа защищены. |
| **SEC-03** | `75271c78-7409-4035-85d4-c1b832e933ac` | **Security Engineer** | `AppLauncher.kt`, `MainActivity.kt` | **Защита от Intent Spoofing и инъекций:** Проверить валидацию `ComponentName` и `PackageName` при запуске внешних приложений из Dashboard (строгий белый список пакетов). | ✅ **Выполнено:** Реализован `AppLauncher.kt` с белыми списками UVOS/AOSP, принудительной эксплицитностью и защитой от подмены Intent. |
| **SEC-04** | `75271c78-7409-4035-85d4-c1b832e933ac` | **Security Engineer** | `coordinator/REPORTS/` | **Финальный сводный аудит экосистемы:** Сформировать отчёт `SECURITY_ECOSYSTEM_REPORT.md` с проверкой DAC-прав приватных хранилищ `context.filesDir` во всех 15 модулях. | ✅ **Выполнено:** Сдан детальный отчёт `REPORTS/SECURITY_ECOSYSTEM_REPORT.md`, подтверждена DAC 0600/UID изоляция. |

---

## 7. Готовые копируемые директивы Спринта 3 (Ready-to-Copy Prompts)

### 📌 Директива для Агента 1 — Lead Developer (`ecd50c90-7bc9-4aed-afc1-f66b718f9c81`):
```text
Задача DEV-04..06 (Спринт 3): Интеграция 15 приложений UVOS, AppLauncher и утечки Bitmap.
Рабочая директория: uvos-dashboard/app/src/main/java/com/uvos/dashboard/

1. В IconPackManager.kt добавь сопоставление Glass-иконок для всех 15 нативных приложений экосистемы UVOS (calendar, camera, clock, compass, downloader, files, gallery, mail, messages, music, notes, security, settings, terminal, vpn).
2. Реализуй AppLauncher.kt: запуск Activity по пакету с безопасным fallback на системные приложения AOSP/HyperOS, если конкретный модуль UVOS не установлен на устройстве (защита от ActivityNotFoundException).
3. В WallpaperManager.kt добавь явный вызов bitmap.recycle() для предыдущего Bitmap перед загрузкой новых пользовательских обоев для исключения утечек в Native Memory.
Собери APK через ./gradlew assembleDebug и предоставь отчёт.
```

### 📌 Директива для Агента 2 — UI/UX Designer (`0c547d7b-340f-47db-ae40-a3d3649e75c1`):
```text
Задача UI-04..06 (Спринт 3): Завершение пакета иконок (15/15), стеклянный Dock и дизайн-токены.
Рабочая директория: uvos-dashboard/app/src/main/res/drawable/

1. Дорисуй недостающие векторные иконки в стиле Dark Glassmorphism по утверждённому референсу (матовое стекло + зеркальный блик + алый неон #FF3B30 / #E02020):
   - ic_glass_compass.xml (компас / азимутальное кольцо)
   - ic_glass_mail.xml (конверт с неоновой печатью)
   - ic_glass_notes.xml (блокнот со строками)
   - ic_glass_calendar.xml (сетка дат с выделенным днём)
2. Создай подложку для нижнего Dock-бара bg_glass_dock.xml со стилем матового стекла и зеркальной фаской.
3. Оформи отчёт coordinator/REPORTS/UI_DESIGN_SYSTEM_TOKENS.md с точным описанием токенов палитры, радиусов скруглений (16dp/24dp) и теней для всех 15 модулей UVOS.
```

### 📌 Директива для Агента 4 — 120Hz Fluid Motion Optimizer (`ff2b50cf-15ae-4192-959b-38a87446c3cb`):
```text
Задача PERF-04..06 (Спринт 3): Оптимизация виджетов телеметрии и 120Hz анимаций перехода.
Рабочая директория: uvos-dashboard/app/src/main/java/com/uvos/dashboard/

1. Проведи аудит BatteryQuadWidgetView.kt и SensorTelemetryManager.kt: исключи аллокации Paint, RectF и Path в onDraw(), кэшируй градиенты и проверяй неизменность значений перед перерисовкой. Время отрисовки виджетов должно быть строго < 2.0 мс.
2. Настрой аппаратные анимации запуска приложений (ActivityOptions.makeCustomAnimation) с кривой FastOutSlowIn для поддержания стабильных 120 FPS при открытии приложений с рабочего стола.
3. Проведи 60-секундный стресс-тест скролла столов через adb shell dumpsys gfxinfo com.uvos.dashboard и зафиксируй результаты в coordinator/REPORTS/PERF_SPRINT3_REPORT.md.
```

### 📌 Директива для Агента 3 — Security Engineer (`75271c78-7409-4035-85d4-c1b832e933ac`):
```text
Задача SEC-02..04 (Спринт 3): Аудит манифестов 15 приложений, защита IPC и изоляция хранилищ.
Рабочая директория: apps/ и uvos-dashboard/

1. Просканируй AndroidManifest.xml во всех 15 модулях apps/*/app/src/main/AndroidManifest.xml: убедись, что все непубличные компоненты имеют строгий флаг android:exported="false".
2. В uvos-dashboard проверь механизм AppLauncher: исключи уязвимости Intent Spoofing через явную валидацию ComponentName по белому списку разрешённых системных и UVOS пакетов.
3. Проверь изоляцию приватных файлов в context.filesDir во всех 15 приложениях (Linux DAC права 0600, UID изоляция) и сформируй отчёт в coordinator/REPORTS/SECURITY_ECOSYSTEM_REPORT.md.
```

---

## 8. Спринт 4: Подключение интерактивных кнопок и триггеров к реальным аппаратным действиям

### 📋 Таблица задач Спринта 4

| ID задачи | Ответственный агент | Роль | Модуль / Компоненты | Задача и декомпозиция | Критерий приёмки (Done) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **DEV-07** | `ecd50c90-7bc9-4aed-afc1-f66b718f9c81` | **Lead Developer** | `SystemShadeManager.kt`, `TorchController.kt` | **Аппаратный контроллер фонарика (Torch):** Подключить `tileTorch` к `CameraManager.setTorchMode()`. Реализовать синхронизацию с `TorchCallback` и переключение фонарика на устройстве. | ✅ **Выполнено:** Реализован `TorchController.kt`, регистрация `TorchCallback`, синхронизация с лампой и статусом шторки. |
| **DEV-08** | `ecd50c90-7bc9-4aed-afc1-f66b718f9c81` | **Lead Developer** | `SystemShadeManager.kt` | **Реальный аудио-контроллер громкости (Volume):** Подключить `sliderVolume` к `AudioManager.setStreamVolume(STREAM_MUSIC)`. Синхронизация громкости при открытии шторки. | ✅ **Выполнено:** Подключен `AudioManager.setStreamVolume` с чтением текущей громкости при открытии шторки. |
| **DEV-09** | `ecd50c90-7bc9-4aed-afc1-f66b718f9c81` | **Lead Developer** | `SystemShadeManager.kt` | **Реальный контроллер яркости экрана (Brightness):** Связать `sliderBrightness` с регулировкой яркости окна (`window.attributes.screenBrightness`) и системной яркости. | ✅ **Выполнено:** Подключена регулировка `screenBrightness` и чтение `Settings.System.SCREEN_BRIGHTNESS`. |
| **DEV-10** | `ecd50c90-7bc9-4aed-afc1-f66b718f9c81` | **Lead Developer** | `MainActivity.kt` | **Интерактивные триггеры часов, батареи и приборов:** Клик по часам `homeClockTime` открывает будильник/часы, клик по дате `homeClockDate` открывает календарь, клик по батарее `topBarBattery` и `gaugeBattery` открывает сведения об энергопотреблении, клик по `gaugeNet` открывает передачу данных. | ✅ **Выполнено:** Все информеры подключены к профильным экранам часов, календаря, батареи и сети. |
| **PERF-07** | `ff2b50cf-15ae-4192-959b-38a87446c3cb` | **120Hz Fluid Motion Optimizer** | `MainActivity.kt` | **Реальная остановка фоновых процессов:** Подключить кнопки "STOP" и "ОСТАНОВИТЬ ВСЕ" в Столе 04 (Process Manager) к реальному `ActivityManager.killBackgroundProcesses(pkg)` с пересчетом свободной RAM. | ✅ **Выполнено:** Реализована пакетная и точечная выгрузка фоновых процессов с каскадной анимацией и расчетом RAM. |
| **PERF-08** | `ff2b50cf-15ae-4192-959b-38a87446c3cb` | **120Hz Fluid Motion Optimizer** | `SystemShadeManager.kt` | **Троттлинг слайдеров на 120 Гц:** Добавить временной фильтр (не чаще 1 вызова IPC в 16 мс) для исключения перегрузки Binder IPC аудио/яркости при быстром ведении пальцем. | ✅ **Выполнено:** Внедрен `IpcThrottler` с квантом 16 мс и выносом в `ipcExecutor`. |
| **SEC-05** | `75271c78-7409-4035-85d4-c1b832e933ac` | **Security Engineer** | `SystemShadeManager.kt` | **Быстрые переключатели сети (Wi-Fi, BT, Data, Isolation):** Подключить клики и долгие тапы по тумблерам к открытию быстрых панелей Android (`Settings.ACTION_WIFI_SETTINGS`, `ACTION_BLUETOOTH_SETTINGS`, `ACTION_DATA_ROAMING_SETTINGS`) без падений от отсутствия прав. | ✅ **Выполнено:** Подключены быстрые панели настроек Android с безопасным перехватом `SecurityException`. |
| **SEC-06** | `75271c78-7409-4035-85d4-c1b832e933ac` | **Security Engineer** | `AndroidManifest.xml` | **Аудит разрешений оборудования:** Проверить и добавить `android.permission.CAMERA` (для Flashlight), `MODIFY_AUDIO_SETTINGS` в манифест лаунчера. | ✅ **Выполнено:** Добавлены разрешения `CAMERA`, `FLASHLIGHT` и `MODIFY_AUDIO_SETTINGS`. |
| **UI-07** | `0c547d7b-340f-47db-ae40-a3d3649e75c1` | **UI/UX Designer** | `SystemShadeManager.kt`, `LampView.kt` | **Индикация активных состояний ламп:** Настроить цвета ламп `LampView`: зеленый (Wi-Fi/BT активны), янтарный (фонарик включен), алый (Стерильно/Изоляция). | ✅ **Выполнено:** Настроены цвета `LampView` (`#10B981`, `#F59E0B`, `#FF3B30`, `#71768E`). |
| **UI-08** | `0c547d7b-340f-47db-ae40-a3d3649e75c1` | **UI/UX Designer** | `MainActivity.kt` | **Тактильный виброотклик (Haptic Feedback):** Добавить тактильный щелчок при нажатии на часы, дату, приборы телеметрии и тумблеры шторки. | ✅ **Выполнено:** Добавлен всеобъемлющий Haptic-отклик (`CLOCK_TICK`, `CONTEXT_CLICK`, `CONFIRM`). |

---

## 9. Готовые копируемые директивы Спринта 4 (Ready-to-Copy Prompts)

### 📌 Директива для Агента 1 — Lead Developer (`ecd50c90-7bc9-4aed-afc1-f66b718f9c81`):
```text
Задача DEV-07..10 (Спринт 4): Подключение фонарика, громкости, яркости и кликов приборов к реальным действиям.
Рабочая директория: uvos-dashboard/app/src/main/java/com/uvos/dashboard/

1. Реализуй TorchController: включение/выключение фонарика через CameraManager.setTorchMode() с регистрацией TorchCallback. Подключи tileTorch в SystemShadeManager.
2. В SystemShadeManager подключи sliderVolume к AudioManager (STREAM_MUSIC): считывай текущий уровень при открытии шторки и устанавливай громкость при движении ползунка.
3. В SystemShadeManager подключи sliderBrightness: управляй яркостью текущего окна через activity.window.attributes.screenBrightness.
4. В MainActivity подключи клики по информерам:
   - homeClockTime -> запуск часов/будильника (AlarmClock.ACTION_SHOW_ALARMS / uvos-clock)
   - homeClockDate -> запуск календаря (Intent.CATEGORY_APP_CALENDAR / uvos-calendar)
   - topBarBattery и gaugeBattery -> сведения о батарее (Intent.ACTION_POWER_USAGE_SUMMARY)
   - gaugeNet -> настройки передачи данных (Settings.ACTION_DATA_USAGE_SETTINGS)
   - batteryQuadWidget (слоты) -> настройки Bluetooth (Settings.ACTION_BLUETOOTH_SETTINGS)
Собери APK через ./gradlew assembleDebug и предоставь отчёт.
```

### 📌 Директива для Агента 4 — 120Hz Fluid Motion Optimizer (`ff2b50cf-15ae-4192-959b-38a87446c3cb`):
```text
Задача PERF-07..08 (Спринт 4): Реальная выгрузка процессов и троттлинг слайдеров на 120 Гц.
Рабочая директория: uvos-dashboard/app/src/main/java/com/uvos/dashboard/

1. В MainActivity (Стол 04, Process Manager):
   - При клике на кнопку "STOP" у процесса вызывай activityManager.killBackgroundProcesses(proc.pkg) с пересчетом свободной RAM и анимацией удаления.
   - Для кнопки btnKillAllServices реализуй пакетную выгрузку фоновых сторонних процессов с отображением высвобожденной памяти.
2. В SystemShadeManager для слайдеров громкости и яркости внедрить временной троттлинг (не чаще 1 IPC вызова в 16 мс), чтобы при непрерывном ведении пальцем на 120Hz экране не блокировать главный UI-поток.
Сформируй отчёт coordinator/REPORTS/PERF_SPRINT4_REPORT.md.
```

### 📌 Директива для Агента 3 — Security Engineer (`75271c78-7409-4035-85d4-c1b832e933ac`):
```text
Задача SEC-05..06 (Спринт 4): Быстрые системные переключатели и аудит разрешений.
Рабочая директория: uvos-dashboard/

1. В SystemShadeManager подключи тумблеры сети:
   - tileWifi -> открытие панели Settings.ACTION_WIFI_SETTINGS (а по долгому тапу - полные настройки Wi-Fi)
   - tileBt -> открытие Settings.ACTION_BLUETOOTH_SETTINGS
   - tileData -> открытие настроек мобильной сети
   - tileIsolation -> переключение локального режима изоляции с уведомлением пользователя
   Все вызовы обернуть в try-catch для предотвращения SecurityException.
2. В AndroidManifest.xml лаунчера добавить необходимые разрешения: android.permission.CAMERA (для фонарика без доступа к съемке) и android.permission.MODIFY_AUDIO_SETTINGS.
Сформируй отчёт coordinator/REPORTS/SECURITY_INTERACTIVE_REPORT.md.
```

### 📌 Директива для Агента 2 — UI/UX Designer (`0c547d7b-340f-47db-ae40-a3d3649e75c1`):
```text
Задача UI-07..08 (Спринт 4): Цветовая индикация тумблеров и тактильный виброотклик.
Рабочая директория: uvos-dashboard/app/src/main/

1. В SystemShadeManager настрой цвета для LampView:
   - Wi-Fi и Bluetooth активны -> чистый изумрудный (#10B981)
   - Фонарик активен -> тёплый янтарный (#F59E0B)
   - Режим "Стерильно" / "Изоляция" активен -> сочный алый (#FF3B30 / #E02020)
   - В выключенном состоянии -> приглушённый графит (#71768E / выключенная лампа)
2. В MainActivity и SystemShadeManager убедись, что на всех интерактивных триггерах (часы, дата, приборы, тумблеры, карточки логов) срабатывает performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK / CLOCK_TICK).
Сформируй отчёт coordinator/REPORTS/UI_INTERACTIVE_REPORT.md.
```

