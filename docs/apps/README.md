# Каталог технической документации приложений UnderVoltOS (UVOS)

В данном каталоге собрана полная проектная и инженерная документация по разработке собственных системных утилит и очистке операционной системы UnderVoltOS.

## Структура документации по компонентам

### 1. Деблоат, Очистка и Базовый Браузер
- [01_DEBLOAT_AND_PURGE.md](file:///c:/Users/barab/Desktop/UVOC/docs/apps/01_DEBLOAT_AND_PURGE.md) — Протокол полной ликвидации аккаунтов Xiaomi/HyperOS/MIUI, деблоат радио, компаса, диктофона, аналитических демонов и служб Google (с сохранением Google Play Store).
- [02_BROWSER_FIREFOX.md](file:///c:/Users/barab/Desktop/UVOC/docs/apps/02_BROWSER_FIREFOX.md) — Спецификация внедрения Mozilla Firefox / Mull с защищённым профилем `user.js`, расширением uBlock Origin и системной ролью браузера по умолчанию.

### 2. Системное Ядро и Безопасность
- [03_APP_SECURITY.md](file:///c:/Users/barab/Desktop/UVOC/docs/apps/03_APP_SECURITY.md) — `com.uvos.security`: брандмауэр iptables, изоляция AppOps, подделка сенсоров и стрелочный индикатор уровня тревоги.
- [04_APP_FILES.md](file:///c:/Users/barab/Desktop/UVOC/docs/apps/04_APP_FILES.md) — `com.uvos.files`: двухпанельный файловый менеджер со стрелочными шкалами дисков M4200, Root-движком и встроенным hex-просмотрщиком.
- [05_APP_SETTINGS.md](file:///c:/Users/barab/Desktop/UVOC/docs/apps/05_APP_SETTINGS.md) — `com.uvos.settings`: пульт управления системой, профили («СТЕРИЛЬНО», «СВЯЗЬ», «СВОЙ»), мониторинг вольтажа и аудит изоляции.
- [06_APP_TERMINAL.md](file:///c:/Users/barab/Desktop/UVOC/docs/apps/06_APP_TERMINAL.md) — `com.uvos.terminal`: PTY-консоль с правами root (`su`), стилизацией под промышленный CRT/приборный дисплей и амперметрами нагрузки CPU/RAM.

### 3. Мультимедиа и Сеть
- [07_APP_MUSIC.md](file:///c:/Users/barab/Desktop/UVOC/docs/apps/07_APP_MUSIC.md) — `com.uvos.music`: Hi-Res оффлайн аудиоплеер с живыми стрелочными стерео VU-метрами (L/R) и 10-полосным эквалайзером.
- [08_APP_GALLERY.md](file:///c:/Users/barab/Desktop/UVOC/docs/apps/08_APP_GALLERY.md) — `com.uvos.gallery`: локальная оффлайн-галерея, инструмент мгновенного удаления EXIF/GPS метаданных и зашифрованный сейф.
- [09_APP_VPN.md](file:///c:/Users/barab/Desktop/UVOC/docs/apps/09_APP_VPN.md) — `com.uvos.vpn`: WireGuard-клиент со стрелочным амперметром скорости, постоянным Kill-Switch и раздельным туннелированием.
- [10_APP_MAIL.md](file:///c:/Users/barab/Desktop/UVOC/docs/apps/10_APP_MAIL.md) — `com.uvos.mail`: защищённый почтовый клиент с шифрованием OpenKeychain PGP и блокировкой трекинг-пикселей.
- [12_APP_DOWNLOADER.md](file:///c:/Users/barab/Desktop/UVOC/docs/apps/12_APP_DOWNLOADER.md) — `com.uvos.downloader`: многопоточный сегментированный менеджер загрузок со стрелочным спидометром скорости.

### 4. Связь и Персональный Органайзер
- [11_APP_MESSAGES.md](file:///c:/Users/barab/Desktop/UVOC/docs/apps/11_APP_MESSAGES.md) — `com.uvos.messages`: SMS/MMS менеджер со встроенным локальным regex-антиспамом и безопасным просмотром.
- [13_APP_CALENDAR.md](file:///c:/Users/barab/Desktop/UVOC/docs/apps/13_APP_CALENDAR.md) — `com.uvos.calendar`: автономный локальный календарь / CalDAV без трекинга Google с ретро-циферблатом даты.
- [14_APP_NOTES.md](file:///c:/Users/barab/Desktop/UVOC/docs/apps/14_APP_NOTES.md) — `com.uvos.notes`: оффлайн Markdown-заметки в открытых файлах `.md` с криптографическим сейфом AES-256.
- [15_APP_CLOCK_TASKS.md](file:///c:/Users/barab/Desktop/UVOC/docs/apps/15_APP_CLOCK_TASKS.md) — `com.uvos.clock` & `com.uvos.tasks`: аналоговый хронометр, таймер со шкалами M4200, нарастающий будильник и матрица задач.

### 5. Оптика и Фотофиксация
- [16_APP_CAMERA.md](16_APP_CAMERA.md) — `com.uvos.camera`: оптический визор M4200 с аппаратной зачисткой EXIF/GPS на лету, осциллографической сеткой и нулевой звукозаписью.
