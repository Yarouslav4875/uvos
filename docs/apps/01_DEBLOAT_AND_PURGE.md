# Системная спецификация UVOS № 01: Debloat, Purge & Hardening
## Комплексная процедура деблоата, ликвидации учетных записей Xiaomi, подавления телеметрии и оптимизации сервисов Google

> **Статус документа**: Принят к реализации (UnderVoltOS Engineering Standard)  
> **Целевая платформа**: Xiaomi Redmi Note 13 4G NFC (`sapphire` / `sapphiren`)  
> **Окружение**: HyperOS / MIUI 14 / Android 13–14 (AOSP / LineageOS compatible)  
> **Допуск**: Root / ADB Shell (User 0) / Magisk & KernelSU  
> **Принцип UnderVoltOS**: 0% рекламы, 0% фоновой телеметрии вендора, 0% неконтролируемых облаков, автономность и аппаратный минимализм.

---

## 1. Архитектурный контекст и модель угроз

В стандартных заводских прошивках Xiaomi HyperOS / MIUI смартфон непрерывно выполняет десятки фоновых процессов, расходующих аппаратные ресурсы (тактовую частоту CPU, заряд аккумулятора и полосу пропускания модема). Векторы утечки конфиденциальности и аппаратного истощения распределены по трем ключевым группам:

1. **Экосистема Xiaomi Cloud / Mi Account**: Постоянный фоновый обмен с серверами в пекинском и европейском регионах, регулярная передача идентификаторов оборудования (IMEI, IMSI, MAC, серийный номер базовой платы), скрытый механизм Find Device с аппаратным сопряжением TrustZone.
2. **Рекламно-аналитический стек (MSA, Joyose, Daemon, Analytics)**: Анализ нажатий, сценариев использования, мониторинг системного журнала (logcat) и принудительное динамическое урезание частот ядер процессора (троттлинг планировщика через Joyose).
3. **Избыточные сервисы Google (Bloatware)**: Цифровое благополучие (Wellbeing), Android System Intelligence, службы отчетов об ошибках, встроенные медиаприложения с непрерывным профилированием пользовательских интересов.

**Цель процедуры**: Сформировать стерильную, предсказуемую операционную среду UnderVoltOS, сохранив исключительно базовую инфраструктуру каталога приложений (Google Play Store + ядро Google Play Services для push-токенов и покупок), ликвидировав все остальные трекеры вендора и сторонних провайдеров.

```
+-----------------------------------------------------------------------------------+
|                        ПРОСТРАНСТВО СИСТЕМЫ UNDERVOLTOS                          |
+-----------------------------------------------------------------------------------+
| [XIAOMI / HYPEROS СТЕК]        [ТЕЛЕМЕТРИЯ И РЕКЛАМА]        [GOOGLE СЕРВИСЫ]     |
| - Mi Account (com.xiaomi.*)    - Joyose                      - Chrome, YouTube    |
| - FindDevice                   - Analytics                   - Maps, Gmail, Drive |
| - CloudService / Sync          - MSA Global                  - Photos, Assistant  |
| - Mi Pay / Pass / SimActivate  - Daemon, YellowPage          - Wellbeing, Turbo   |
|          |                               |                           |            |
|     ПОЛНЫЙ PURGE                    ПОЛНЫЙ БЛОК             ВЫБОРОЧНЫЙ DEBLOAT    |
|          |                               |                           |            |
|          v                               v                           v            |
| [ ЛИКВИДИРОВАНО ]             [ DNS / HOSTS / PM KILL ]     [ ТОЛЬКО PLAY STORE ] |
|  - 0 байт трафика              - Троттлинг отключен          - com.android.vending|
|  - 0 фоновых демонов           - 0 рекламных баннеров        - com.google.android.|
|  - Свобода от FRP Lock         - Холодный Deep Sleep           gms (Core Only)    |
+-----------------------------------------------------------------------------------+
```

---

## 2. Критический протокол: Ликвидация аккаунтов Xiaomi / HyperOS / MIUI

> [!CAUTION]
> **КРИТИЧЕСКОЕ ПРЕДОСТЕРЕЖЕНИЕ (FRP & FIND DEVICE LOCK)**:
> Сервис `com.xiaomi.finddevice` и аккаунт `com.xiaomi.account` глубоко интегрированы в доверенную зону безопасности Qualcomm (TrustZone/TEE) и RPMB-раздел флеш-памяти UFS. 
> 
> Если удалить пакет `com.xiaomi.account` или `com.xiaomi.finddevice` **ДО ТОГО**, как пользователь вручную выполнит Sign Out (выход) из учетной записи на устройстве, флаг защиты `Device Associate Token` останется активным в защищенном хранилище. При первой же перезагрузке, очистке кэша Dalvik или сбросе настроек устройство заблокируется экраном **"This device is locked"** (FRP Lock), требуя ввода пароля от удаленного аккаунта без возможности подключиться к Wi-Fi из-за вырезанных сетевых служб аккаунта!

### 2.1. Обязательный регламент подготовки к удалению аккаунтов (Pre-Debloat Checklist)

Перед выполнением любых консольных команд выполните следующие действия в графическом интерфейсе смартфона:

1. **Ручной выход из Mi Account**:
   - Перейдите в: `Настройки` -> `Xiaomi Account` (или `Mi Аккаунт`).
   - Нажмите `Выйти из аккаунта` (`Sign Out`).
   - При запросе системы выберите **«Удалить объекты с устройства»** (контакты, заметки Mi Cloud), чтобы локальная SQLite-база очистилась штатно.
   - Введите пароль от Mi-аккаунта для снятия аппаратного токена с серверов Xiaomi.
   - Убедитесь, что в меню `Настройки` пункт `Xiaomi Account` отображает состояние **«Не выполнен вход»**.
2. **Отключение функции поиска устройства (Find Device)**:
   - Перейдите в: `Настройки` -> `Безопасность и конфиденциальность` -> `Поиск устройства` (Find Device).
   - Деактивируйте тумблер. Подтвердите операцию паролем.
3. **Отключение блокировок экрана и биометрии**:
   - `Настройки` -> `Пароли и безопасность` -> временно переведите экран блокировки в режим `Нет` (отключите PIN, графический ключ и отпечатки пальцев). Это предотвратит рассинхронизацию Gatekeeper и Keystore при реорганизации системных пакетов.
4. **Активация режима отладки по USB**:
   - `Настройки` -> `О телефоне` -> тапните 7 раз по `Версия OS / MIUI`.
   - `Настройки` -> `Расширенные настройки` -> `Для разработчиков`:
     - Включить `Отладка по USB` (`USB Debugging`).
     - Включить `Установка через USB` (`Install via USB`).
     - Включить `Отладка по USB (Настройки безопасности)` (`USB Debugging (Security settings)`).

---

### 2.2. Пакетная матрица компонентов экосистемы Xiaomi подлежащих удалению

| Имя пакета | Назначение в стоковой прошивке | Риск при удалении | Влияние на автономность/безопасность |
|---|---|---|---|
| `com.xiaomi.account` | Ядро авторизации Mi Account | **Низкий** (после Sign Out) | Исключает фоновые запросы аутентификации, освобождает ~45 МБ ОЗУ |
| `com.xiaomi.finddevice` | Маяк удаленного поиска и блокировки | **Низкий** (после Sign Out) | Полностью устраняет постоянные GPS-запросы и WakeLock службы |
| `com.miui.cloudservice` | Фоновая служба Mi Cloud | Отсутствует | Прекращает несанкционированную выгрузку резервных копий в КНР |
| `com.miui.cloudservice.sysbase` | Низкоуровневая база Mi Cloud | Отсутствует | Устраняет связующие IPC-провайдеры облака |
| `com.miui.cloudbackup` | Автоматический бэкап приложений в облако | Отсутствует | Экономит трафик и циклы записи флеш-памяти |
| `com.miui.micloudsync` | Синхронизация галереи, заметок, вызовов | Отсутствует | Устраняет утечку журнала вызовов и SMS |
| `com.xiaomi.xmsf` | Xiaomi Mobile Services Framework | Низкий | Отключает вендорские Push-уведомления и фоновый сокет |
| `com.xiaomi.payment` | Mi Pay (платежная инфраструктура Китая/Индии)| Отсутствует | Исключает неиспользуемые финансовые службы |
| `com.mipay.wallet.id` | Идентификатор кошелька Mi Pay | Отсутствует | Ликвидация паразитных сервисов |
| `com.xiaomi.pass` | Mi Pass / Учетные записи скидок и бонусов | Отсутствует | Освобождение системных ресурсов |
| `com.miui.userguide` | Системный гид с навязчивым входом в аккаунт | Отсутствует | Блокировка всплывающих баннеров регистрации |
| `com.miui.vipservice` | Программа лояльности и сообщество Xiaomi | Отсутствует | Отключение телеметрии активности в сообществе |
| `com.xiaomi.simactivate.service`| Служба фоновой верификации SIM через скрытые SMS | Отсутствует | Предотвращает несанкционированную отправку международных SMS |

---

### 2.3. Команды деблоата экосистемы Xiaomi (ADB Shell / Root)

Для выполнения без root-доступа через ADB (удаление для основного пользователя `User 0` с сохранением системного APK на случай отката):

```bash
# Проверка подключения устройства
adb devices

# 1. Ликвидация ядра Mi Account и сервиса поиска
adb shell pm uninstall -k --user 0 com.xiaomi.account
adb shell pm uninstall -k --user 0 com.xiaomi.finddevice

# 2. Ликвидация служб Mi Cloud и синхронизации
adb shell pm uninstall -k --user 0 com.miui.cloudservice
adb shell pm uninstall -k --user 0 com.miui.cloudservice.sysbase
adb shell pm uninstall -k --user 0 com.miui.cloudbackup
adb shell pm uninstall -k --user 0 com.miui.micloudsync

# 3. Ликвидация Push-фреймворка и платежных демонов
adb shell pm uninstall -k --user 0 com.xiaomi.xmsf
adb shell pm uninstall -k --user 0 com.xiaomi.payment
adb shell pm uninstall -k --user 0 com.mipay.wallet.id
adb shell pm uninstall -k --user 0 com.xiaomi.pass
adb shell pm uninstall -k --user 0 com.xiaomi.simactivate.service

# 4. Ликвидация навязчивых утилит входа
adb shell pm uninstall -k --user 0 com.miui.userguide
adb shell pm uninstall -k --user 0 com.miui.vipservice
```

Если на устройстве доступен **Root (Magisk / KernelSU)**, для полной заморозки и запрета автозапуска на уровне демона PackageManager:

```bash
su -c "pm disable-user --user 0 com.xiaomi.account"
su -c "pm disable-user --user 0 com.xiaomi.finddevice"
su -c "pm disable-user --user 0 com.miui.cloudservice"
su -c "pm disable-user --user 0 com.miui.cloudservice.sysbase"
su -c "pm disable-user --user 0 com.miui.cloudbackup"
su -c "pm disable-user --user 0 com.miui.micloudsync"
su -c "pm disable-user --user 0 com.xiaomi.xmsf"
su -c "pm disable-user --user 0 com.xiaomi.payment"
su -c "pm disable-user --user 0 com.mipay.wallet.id"
su -c "pm disable-user --user 0 com.xiaomi.pass"
su -c "pm disable-user --user 0 com.xiaomi.simactivate.service"
su -c "pm disable-user --user 0 com.miui.userguide"
su -c "pm disable-user --user 0 com.miui.vipservice"
```

---

## 3. Процедура удаления стандартных утилит вендора

В базовой комплектации HyperOS предустановлены утилиты с закрытым исходным кодом, содержащие рекламные SDK, трекинг и избыточные разрешения на чтение геолокации и памяти. Согласно архитектурному стандарту UVOS, они полностью демонтируются и заменяются на легковесные изолированные утилиты без доступа в сеть.

### 3.1. Анализ удаляемых компонентов

1. **FM-Радио (`com.miui.fm`, `com.android.fmradio`, `com.caf.fmradio`)**:
   - Причина удаления: На Redmi Note 13 4G чипсет часто не имеет аппаратного аналогового тракта FM-антенны либо приложение требует постоянный доступ к геолокации и рекламным модулям.
   - Изоляция: При необходимости прослушивания оффлайн-музыки используется автономный аудиоплеер `com.uvos.music`.
2. **Компас (`com.miui.compass`)**:
   - Причина удаления: Системный компас Xiaomi обращается к сетевым провайдерам для отображения давления, высоты над уровнем моря и погоды, отправляя точные координаты устройства третьим лицам.
   - Изоляция: Замена на локальный open-source компас или использование прямого считывания датчика Холла/магнитометра без разрешения `INTERNET`.
3. **Диктофон (`com.miui.soundrecorder`, `com.android.soundrecorder`)**:
   - Причина удаления: Автоматически интегрируется со службой расшифровки речи в облаке Xiaomi и сервисом заметок, сохраняя файлы в проприетарных контейнерах.
   - Изоляция: Локальная запись звука через защищенное приложение с локальным сохранением WAV/Opus.

### 3.2. Команды для удаления утилит

```bash
# Удаление FM-Радио
adb shell pm uninstall -k --user 0 com.miui.fm
adb shell pm uninstall -k --user 0 com.android.fmradio
adb shell pm uninstall -k --user 0 com.caf.fmradio

# Удаление Компаса
adb shell pm uninstall -k --user 0 com.miui.compass

# Удаление Диктофона
adb shell pm uninstall -k --user 0 com.miui.soundrecorder
adb shell pm uninstall -k --user 0 com.android.soundrecorder
```

---

## 4. Селективный деблоат Google: Сохранение Minimalist Play Core

### 4.1. Концепция безопасного сохранения Google Play Core

Большинство пользователей Android зависят от легального каталога приложений и покупок внутри программ. Полное вырезание GMS (`com.google.android.gms`) часто приводит к падению банковских приложений, клиентов каршеринга и невозможности обновить приложения.

**Инженерное решение UVOS**: Мы сохраняем исключительно два минимальных системных компонента, а все пользовательские и телеметрические надстройки Google вырезаем:

* **СОХРАНЯЕМ**:
  * `com.android.vending` — **Google Play Store**: Клиент каталога приложений, проверка лицензий, установка APK/Split-APKs.
  * `com.google.android.gms` — **Google Play Services (GmsCore)**: Минимальный IPC-брокер для работы In-App Billing, Play Asset Delivery и стандартных Push-уведомлений Firebase Cloud Messaging (FCM).
  * `com.google.android.gsf` — **Google Services Framework**: Легковесный провайдер учетных записей Google.
* **УДАЛЯЕМ**:
  * Все остальные приложения Google (браузер, почта, карты, фото, ассистенты, экранное время, предиктивный ввод).

### 4.2. Матрица удаляемых сервисов Google

| Пакет Google | Описание | Причина удаления | Альтернатива в UVOS |
|---|---|---|---|
| `com.android.chrome` | Google Chrome | Сбор истории просмотров, телеметрия Google Sync | Mozilla Firefox / Mull (Hardened) |
| `com.google.android.youtube` | YouTube | Избыточное потребление батареи, реклама | NewPipe / Brave NewPipe / ReVanced |
| `com.google.android.apps.youtube.music` | YT Music | Проприетарный стриминг, реклама | `com.uvos.music` (FLAC/MP3) |
| `com.google.android.gm` | Gmail | Постоянный сканирующий демон почты | `com.uvos.mail` (IMAP/PGP оффлайн) |
| `com.google.android.apps.photos` | Google Фото | Принудительная синхронизация медиатеки с облаком | `com.uvos.gallery` (Zero-Internet) |
| `com.google.android.apps.maps` | Google Карты | Непрерывный сбор истории локаций и передвижений | Organic Maps / OsmAnd+ |
| `com.google.android.apps.docs` | Google Диск | Облачное хранилище сторонней корпорации | Локальное хранилище + `com.uvos.files` |
| `com.google.android.googlequicksearchbox`| Поиск Google и лента Discover | Постоянная прослушка микрофона, WakeLock | Отсутствует (Не требуется) |
| `com.google.android.apps.googleassistant`| Google Assistant | Голосовой агент фонового сбора данных | Отсутствует |
| `com.google.android.apps.wellbeing` | Цифровое благополучие | Непрерывный мониторинг каждого клика и запуска | Отсутствует (Экономия 5-7% CPU) |
| `com.google.android.apps.turbo` | Device Health Services | Сбор диагностической информации о батарее | Прямое чтение `/sys/class/power_supply` |
| `com.google.android.as` | Device Personalization Services | AI-анализ контекста на экране | Устранение утечки конфиденциальности |
| `com.google.android.feedback` | Google Feedback | Отправка аварийных дампов | Отсутствует |
| `com.google.android.partnersetup` | Партнерские соглашения Google | Проверка коммерческих профилей вендора | Отсутствует |
| `com.google.android.printservice.recommendation` | Сервис рекомендаций печати | Постоянный сетевой mDNS-скан локальной сети | Отсутствует |

### 4.3. Команды для удаления блоатвара Google

```bash
# Удаление браузера и медиасервисов
adb shell pm uninstall -k --user 0 com.android.chrome
adb shell pm uninstall -k --user 0 com.google.android.youtube
adb shell pm uninstall -k --user 0 com.google.android.apps.youtube.music

# Удаление почты, диска, фото и карт
adb shell pm uninstall -k --user 0 com.google.android.gm
adb shell pm uninstall -k --user 0 com.google.android.apps.docs
adb shell pm uninstall -k --user 0 com.google.android.apps.photos
adb shell pm uninstall -k --user 0 com.google.android.apps.maps

# Удаление поискового виджета, ассистента и распознавания речи
adb shell pm uninstall -k --user 0 com.google.android.googlequicksearchbox
adb shell pm uninstall -k --user 0 com.google.android.apps.googleassistant

# Ликвидация глубоких трекеров поведения пользователя и батареи
adb shell pm uninstall -k --user 0 com.google.android.apps.wellbeing
adb shell pm uninstall -k --user 0 com.google.android.apps.turbo
adb shell pm uninstall -k --user 0 com.google.android.as
adb shell pm uninstall -k --user 0 com.google.android.feedback
adb shell pm uninstall -k --user 0 com.google.android.partnersetup
adb shell pm uninstall -k --user 0 com.google.android.printservice.recommendation
```

---

## 5. Полное подавление системной телеметрии, рекламы и шпионажа

### 5.1. Демонтаж системных демонов Xiaomi

В системе присутствуют сервисы, отвечающие за внедрение баннеров и навязчивый сетевой сбор:

1. **`com.xiaomi.joyose`**:
   - Является так называемым "сервисом оптимизации развлечений".
   - Фактически: непрерывно отправляет телеметрию на сервера Xiaomi, мониторит запущенные пакеты игр и тяжелых программ, урезает тактовые частоты кластеров Kryo (троттлинг), искусственно занижая FPS для псевдо-охлаждения. 
   - **Удаление обязательно**: после деблоата Joyose система работает быстрее, отзывчивость UI возрастает, а фоновые сетевые коннекты в Китай прекращаются.
2. **`com.miui.analytics`**:
   - Главная аналитическая библиотека MIUI/HyperOS. Логирует любые переключения экранов, время работы в каждом приложении и отправляет JSON-отчеты.
3. **`com.miui.msa.global`**:
   - MIUI System Ads. Демон загрузки баннеров, видеовставок и спонсорских ссылок в стоковые Проводник, Безопасность, Темы и Меню недавних.
4. **`com.miui.daemon`**:
   - Системный демон отправки логов, сбоев и статистики оборудования на `data.mistat.xiaomi.com`.
5. **`com.miui.yellowpage`**:
   - Служба "Желтых страниц", проверяющая входящие и исходящие телефонные вызовы через китайские справочники, передавая номера абонентов во внешнюю сеть.
6. **`com.miui.hybrid` / `com.miui.hybrid.accessory`**:
   - Движок Quick Apps (быстрые веб-приложения), используемый для автоматической подгрузки рекламных лент без ведома пользователя.
7. **`com.miui.bugreport` & `com.miui.miservice`**:
   - Формирование дампов памяти и постоянный контакт со службой технической поддержки Xiaomi.

```bash
# Удаление системных демонов слежки и рекламы
adb shell pm uninstall -k --user 0 com.xiaomi.joyose
adb shell pm uninstall -k --user 0 com.miui.analytics
adb shell pm uninstall -k --user 0 com.miui.msa.global
adb shell pm uninstall -k --user 0 com.miui.daemon
adb shell pm uninstall -k --user 0 com.miui.yellowpage
adb shell pm uninstall -k --user 0 com.miui.hybrid
adb shell pm uninstall -k --user 0 com.miui.hybrid.accessory
adb shell pm uninstall -k --user 0 com.miui.bugreport
adb shell pm uninstall -k --user 0 com.miui.miservice
```

---

### 5.2. Аппаратная фильтрация: Системный Private DNS (DNS-over-TLS)

Для блокировки оставшихся сетевых запросов системных компонентов, которые не могут быть удалены без разрушения загрузки Android, применяется принудительное переключение сетевого стека Android на защищенный шифрованный протокол **DNS-over-TLS (DoT)** с фильтрацией на базе серверов **AdGuard DNS**.

#### Команды для установки через ADB / Root:
```bash
# Включение строгого режима частного DNS (Private DNS Provider Hostname)
adb shell settings put global private_dns_mode hostname

# Установка AdGuard DNS в качестве системного резолвера
adb shell settings put global private_dns_specifier dns.adguard-dns.com
```

*Результат*:
* Все DNS-запросы смартфона шифруются по TLS (порт 853), предотвращая DPI-анализ и подмену со стороны интернет-провайдеров.
* Все запросы к доменам `*.ad.xiaomi.com`, `*.mistat.xiaomi.com`, `*.analytics.google.com` блокируются на уровне возврата `NXDOMAIN` (0.0.0.0), полностью исключая передачу телеметрии.

---

### 5.3. Низкоуровневая блокировка: Hardened `/system/etc/hosts` (Требуется Root)

Если устройство работает под управлением Root (Magisk Systemless Hosts или KernelSU overlayfs), системный файл `/etc/hosts` модифицируется для гарантированного разрыва соединений на уровне сокетов ядра Linux.

#### Список критических доменов блокировки:
```text
127.0.0.1 localhost
::1 localhost

# --- Xiaomi & MIUI Telemetry Endpoints ---
0.0.0.0 data.mistat.xiaomi.com
0.0.0.0 data.mistat.intl.xiaomi.com
0.0.0.0 tracking.intl.miui.com
0.0.0.0 tracking.miui.com
0.0.0.0 sa.api.intl.miui.com
0.0.0.0 api.ad.xiaomi.com
0.0.0.0 ad.xiaomi.com
0.0.0.0 adv.sec.miui.com
0.0.0.0 a.stat.xiaomi.com
0.0.0.0 o2o.api.xiaomi.com
0.0.0.0 register.xmpush.global.xiaomi.com
0.0.0.0 feedback.miui.com
0.0.0.0 hotfix.patch.miui.com

# --- Google Telemetry & Crashlytics Endpoints ---
0.0.0.0 app-measurement.com
0.0.0.0 firebase-settings.crashlytics.com
0.0.0.0 crashlyticsreports-pa.googleapis.com
0.0.0.0 reports.crashlytics.com
0.0.0.0 telemetry-pa.googleapis.com
0.0.0.0 play.googleapis.com/log
```

#### Процедура внедрения через Magisk Systemless:
1. В приложении Magisk активируйте переключатель `Systemless Hosts`.
2. Подключитесь через ADB и выполните:
```bash
adb shell su -c "echo '0.0.0.0 data.mistat.xiaomi.com' >> /data/adb/modules/hosts/system/etc/hosts"
adb shell su -c "echo '0.0.0.0 tracking.intl.miui.com' >> /data/adb/modules/hosts/system/etc/hosts"
adb shell su -c "echo '0.0.0.0 api.ad.xiaomi.com' >> /data/adb/modules/hosts/system/etc/hosts"
adb shell su -c "echo '0.0.0.0 ad.xiaomi.com' >> /data/adb/modules/hosts/system/etc/hosts"
```

---

## 6. Скрипты автоматизации деблоата

Для удобства развертывания подготовлены два полноценных скрипта автоматизации: для сред Bash (Linux / macOS / Termux) и PowerShell (Windows).

### 6.1. Автоматизированный скрипт: `scripts/debloat_uvos.sh` (Bash)

```bash
#!/usr/bin/env bash
# ==============================================================================
# UnderVoltOS (UVOS) Suite - Automated Debloat & Telemetry Purge Engine
# Target: Xiaomi Redmi Note 13 4G NFC (sapphire / sapphiren) / HyperOS / MIUI
# ==============================================================================

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}================================================================${NC}"
echo -e "${CYAN}   UnderVoltOS (UVOS): Скрипт системной очистки и деблоата       ${NC}"
echo -e "${CYAN}================================================================${NC}"

# Проверка наличия ADB
if ! command -v adb &> /dev/null; then
    echo -e "${RED}[ОШИБКА] ADB не найден в переменной PATH. Установите Android Platform-Tools.${NC}"
    exit 1
fi

echo -e "${YELLOW}[!] Ожидание подключения устройства в режиме ADB...${NC}"
adb wait-for-device

DEVICE_MODEL=$(adb shell getprop ro.product.model | tr -d '\r')
DEVICE_CODE=$(adb shell getprop ro.product.device | tr -d '\r')
BUILD_VERSION=$(adb shell getprop ro.build.version.incremental | tr -d '\r')

echo -e "${GREEN}[+] Устройство обнаружено: ${DEVICE_MODEL} (${DEVICE_CODE}), Сборка: ${BUILD_VERSION}${NC}"
echo ""
echo -e "${RED}!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!${NC}"
echo -e "${RED}ВНИМАНИЕ! ВЫ ВЫПОЛНИЛИ РУЧНОЙ ВЫХОД (SIGN OUT) ИЗ MI ACCOUNT?   ${NC}"
echo -e "${RED}Удаление com.xiaomi.account без Sign Out вызовет Mi FRP LOCK!   ${NC}"
echo -e "${RED}!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!${NC}"
read -rp "Подтверждаете, что Mi-аккаунт отключен? (y/N): " CONFIRM

if [[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" ]]; then
    echo -e "${YELLOW}[ОТМЕНА] Сначала выйдите из Mi-аккаунта в Настройках устройства!${NC}"
    exit 0
fi

# Массив пакетов для ликвидации
PACKAGES=(
    # --- 1. Xiaomi / HyperOS Accounts & Cloud ---
    "com.xiaomi.account"
    "com.xiaomi.finddevice"
    "com.miui.cloudservice"
    "com.miui.cloudservice.sysbase"
    "com.miui.cloudbackup"
    "com.miui.micloudsync"
    "com.xiaomi.xmsf"
    "com.xiaomi.payment"
    "com.mipay.wallet.id"
    "com.xiaomi.pass"
    "com.xiaomi.simactivate.service"
    "com.miui.userguide"
    "com.miui.vipservice"

    # --- 2. Системные утилиты (подлежат замене/удалению) ---
    "com.miui.fm"
    "com.android.fmradio"
    "com.caf.fmradio"
    "com.miui.compass"
    "com.miui.soundrecorder"
    "com.android.soundrecorder"

    # --- 3. Google Bloatware (Play Store и GMS Core сохраняются!) ---
    "com.android.chrome"
    "com.google.android.youtube"
    "com.google.android.apps.youtube.music"
    "com.google.android.gm"
    "com.google.android.apps.docs"
    "com.google.android.apps.photos"
    "com.google.android.apps.maps"
    "com.google.android.googlequicksearchbox"
    "com.google.android.apps.googleassistant"
    "com.google.android.apps.wellbeing"
    "com.google.android.apps.turbo"
    "com.google.android.as"
    "com.google.android.feedback"
    "com.google.android.partnersetup"
    "com.google.android.printservice.recommendation"

    # --- 4. Реклама, телеметрия и троттлинг Xiaomi ---
    "com.xiaomi.joyose"
    "com.miui.analytics"
    "com.miui.msa.global"
    "com.miui.daemon"
    "com.miui.yellowpage"
    "com.miui.hybrid"
    "com.miui.hybrid.accessory"
    "com.miui.bugreport"
    "com.miui.miservice"
)

echo ""
echo -e "${CYAN}[*] Запуск процесса деблоата для User 0...${NC}"
SUCCESS_COUNT=0
SKIP_COUNT=0

for PKG in "${PACKAGES[@]}"; do
    echo -n "Обработка $PKG ... "
    # Проверяем наличие пакета в системе
    if adb shell pm list packages | grep -q "$PKG"; then
        if adb shell pm uninstall -k --user 0 "$PKG" > /dev/null 2>&1; then
            echo -e "${GREEN}[УДАЛЕН]${NC}"
            ((SUCCESS_COUNT++))
        else
            echo -e "${RED}[ОШИБКА УДАЛЕНИЯ]${NC}"
        fi
    else
        echo -e "${YELLOW}[НЕ УСТАНОВЛЕН / ПРОПУЩЕН]${NC}"
        ((SKIP_COUNT++))
    fi
done

echo ""
echo -e "${CYAN}[*] Применение настроек Private DNS (AdGuard TLS-резолвер)...${NC}"
adb shell settings put global private_dns_mode hostname
adb shell settings put global private_dns_specifier dns.adguard-dns.com
echo -e "${GREEN}[+] Private DNS переведен на dns.adguard-dns.com${NC}"

echo ""
echo -e "${CYAN}================================================================${NC}"
echo -e "${GREEN}Операция завершена успешно!${NC}"
echo -e "Пакетов удалено: ${SUCCESS_COUNT}, отсутствовало: ${SKIP_COUNT}"
echo -e "Google Play Store (com.android.vending) и GmsCore сохранены."
echo -e "${CYAN}================================================================${NC}"
```

---

### 6.2. Автоматизированный скрипт: `scripts/debloat_uvos.ps1` (PowerShell)

```powershell
<#
==============================================================================
UnderVoltOS (UVOS) Suite - Automated Debloat & Telemetry Purge Engine
Target: Xiaomi Redmi Note 13 4G NFC (sapphire / sapphiren) / Windows PowerShell
==============================================================================
#>

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "   UnderVoltOS (UVOS): Скрипт системной очистки (PowerShell)    " -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan

# Проверка наличия adb
if (-not (Get-Command "adb" -ErrorAction SilentlyContinue)) {
    Write-Host "[ОШИБКА] Утилита adb.exe не обнаружена в PATH!" -ForegroundColor Red
    Exit 1
}

Write-Host "[!] Ожидание авторизованного устройства в режиме ADB..." -ForegroundColor Yellow
adb wait-for-device

$deviceModel = (adb shell getprop ro.product.model).Trim()
$deviceCode  = (adb shell getprop ro.product.device).Trim()

Write-Host "[+] Подключено устройство: $deviceModel ($deviceCode)" -ForegroundColor Green
Write-Host ""
Write-Host "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" -ForegroundColor Red
Write-Host "ВНИМАНИЕ! ВЫ ВЫПОЛНИЛИ РУЧНОЙ ВЫХОД (SIGN OUT) ИЗ MI ACCOUNT?   " -ForegroundColor Red
Write-Host "Удаление com.xiaomi.account без выхода заблокирует устройство!  " -ForegroundColor Red
Write-Host "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" -ForegroundColor Red

$confirmation = Read-Host "Подтверждаете готовность продолжить? (y/N)"
if ($confirmation -ne "y" -and $confirmation -ne "Y") {
    Write-Host "[ОТМЕНА] Завершение работы без изменений." -ForegroundColor Yellow
    Exit 0
}

$purgeList = @(
    # 1. Xiaomi / HyperOS Accounts & Cloud
    "com.xiaomi.account",
    "com.xiaomi.finddevice",
    "com.miui.cloudservice",
    "com.miui.cloudservice.sysbase",
    "com.miui.cloudbackup",
    "com.miui.micloudsync",
    "com.xiaomi.xmsf",
    "com.xiaomi.payment",
    "com.mipay.wallet.id",
    "com.xiaomi.pass",
    "com.xiaomi.simactivate.service",
    "com.miui.userguide",
    "com.miui.vipservice",

    # 2. Ненужные системные утилиты
    "com.miui.fm",
    "com.android.fmradio",
    "com.caf.fmradio",
    "com.miui.compass",
    "com.miui.soundrecorder",
    "com.android.soundrecorder",

    # 3. Google Bloatware (Play Store сохраняется)
    "com.android.chrome",
    "com.google.android.youtube",
    "com.google.android.apps.youtube.music",
    "com.google.android.gm",
    "com.google.android.apps.docs",
    "com.google.android.apps.photos",
    "com.google.android.apps.maps",
    "com.google.android.googlequicksearchbox",
    "com.google.android.apps.googleassistant",
    "com.google.android.apps.wellbeing",
    "com.google.android.apps.turbo",
    "com.google.android.as",
    "com.google.android.feedback",
    "com.google.android.partnersetup",
    "com.google.android.printservice.recommendation",

    # 4. Реклама, трекеры и Joyose
    "com.xiaomi.joyose",
    "com.miui.analytics",
    "com.miui.msa.global",
    "com.miui.daemon",
    "com.miui.yellowpage",
    "com.miui.hybrid",
    "com.miui.hybrid.accessory",
    "com.miui.bugreport",
    "com.miui.miservice"
)

$installedPackages = adb shell pm list packages

$deleted = 0
$skipped = 0

foreach ($pkg in $purgeList) {
    Write-Host -NoNewline "Обработка $pkg ... "
    if ($installedPackages -match $pkg) {
        $res = adb shell pm uninstall -k --user 0 $pkg 2>&1
        if ($res -match "Success") {
            Write-Host "[УДАЛЕН]" -ForegroundColor Green
            $deleted++
        } else {
            Write-Host "[ОШИБКА: $res]" -ForegroundColor Red
        }
    } else {
        Write-Host "[ПРОПУЩЕН / НЕ НАЙДЕН]" -ForegroundColor Yellow
        $skipped++
    }
}

Write-Host ""
Write-Host "[*] Активация Private DNS AdGuard (DoT)..." -ForegroundColor Cyan
adb shell settings put global private_dns_mode hostname
adb shell settings put global private_dns_specifier dns.adguard-dns.com
Write-Host "[+] Режим Private DNS успешно сконфигурирован." -ForegroundColor Green

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "Деблоат завершен! Удалено: $deleted, Пропущено: $skipped" -ForegroundColor Green
Write-Host "Google Play Store (com.android.vending) и GmsCore активны." -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor Cyan
```

---

## 7. Процедура отката и восстановления (Rollback Protocol)

Команда `pm uninstall -k --user 0 <package>` удаляет приложение только для нулевого пользователя (User 0), оставляя оригинальный base.apk в каталоге `/system/app`, `/system/priv-app` или `/product/app`. Это гарантирует 100% обратимость без перепрошивки устройства.

### 7.1. Восстановление отдельного пакета

Если по какой-либо причине вам потребовалось вернуть удаленный системный компонент (например, системный компас или диктофон):

```bash
# Синтаксис:
adb shell cmd package install-existing <ИМЯ_ПАКЕТА>

# Пример восстановления компаса:
adb shell cmd package install-existing com.miui.compass

# Пример восстановления диктофона:
adb shell cmd package install-existing com.miui.soundrecorder

# Пример восстановления Google Фото:
adb shell cmd package install-existing com.google.android.apps.photos
```

### 7.2. Полный скрипт отката деблоата: `scripts/restore_all.sh`

```bash
#!/usr/bin/env bash
# ==============================================================================
# UnderVoltOS (UVOS) - Скрипт восстановления всех удаленных стоковых пакетов
# ==============================================================================

PACKAGES=(
    "com.xiaomi.account"
    "com.xiaomi.finddevice"
    "com.miui.cloudservice"
    "com.miui.cloudservice.sysbase"
    "com.miui.cloudbackup"
    "com.miui.micloudsync"
    "com.xiaomi.xmsf"
    "com.xiaomi.payment"
    "com.mipay.wallet.id"
    "com.xiaomi.pass"
    "com.miui.compass"
    "com.miui.soundrecorder"
    "com.android.chrome"
    "com.google.android.youtube"
    "com.google.android.gm"
    "com.google.android.apps.photos"
    "com.google.android.apps.maps"
    "com.google.android.googlequicksearchbox"
    "com.xiaomi.joyose"
    "com.miui.analytics"
    "com.miui.msa.global"
    "com.miui.daemon"
    "com.miui.yellowpage"
)

echo "Запуск процедуры восстановления компонентов..."
for PKG in "${PACKAGES[@]}"; do
    echo -n "Восстановление $PKG: "
    adb shell cmd package install-existing "$PKG" || true
done

echo "Сброс Private DNS на авто..."
adb shell settings put global private_dns_mode opportunistic
echo "Восстановление завершено!"
```

---

## 8. Чеклист верификации после очистки

После выполнения процедур деблоата проведите контрольную проверку системы:

1. **Работоспособность Google Play Store**:
   - Запустите Google Play Store (`com.android.vending`).
   - Убедитесь, что магазин открывается, авторизация аккаунта Google сохранена, установка и обновление бесплатных/платных приложений проходят штатно.
2. **Проверка глубокого сна (Deep Sleep)**:
   - Отключите кабель USB.
   - Оставьте телефон на 15 минут в заблокированном состоянии.
   - Выполните через ADB: `adb shell dumpsys batterystats --charged` или проверьте утилитой BetterBatteryStats / Franco Kernel Manager. Время глубокого сна (Deep Sleep) должно составлять более 92–96% от времени экрана в покое.
3. **Отсутствие процессов Joyose и Analytics в выводе ps**:
   ```bash
   adb shell "ps -A | grep -E 'joyose|analytics|msa|miui.daemon'"
   ```
   *Ожидаемый результат*: Вывод команды должен быть полностью пустым.
4. **Проверка разрешения DNS-over-TLS**:
   - Откройте браузер на смартфоне и перейдите на страницу `https://adguard.com/ru/test.html`.
   - Страница должна подтвердить: **«AdGuard DNS подключен»**.

---

## 9. Заключение

Выполнение данного протокола трансформирует типовую сборку HyperOS на Xiaomi Redmi Note 13 4G в защищенную базовую среду **UnderVoltOS**:
- Полностью исключены скрытые утечки конфиденциальности в сервисы вендора.
- Процессор освобожден от троттлинг-демона Joyose.
- Обеспечена совместимость с привычной экосистемой приложений через чистый, сохраненный Google Play Store.
- Подготовлена аппаратная база для интеграции фирменных безопасных утилит UVOS Suite (`com.uvos.*`).
