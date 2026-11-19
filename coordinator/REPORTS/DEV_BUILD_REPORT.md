# DEV_BUILD_REPORT.md — Отчёт о сборке и Unit-тестировании модулей UnderVoltOS (UVOS)

**Дата отчёта:** 2026-11-19  
**Агент:** Lead Developer (`ecd50c90-7bc9-4aed-afc1-f66b718f9c81`)  
**Координатор проекта:** Main Agent  
**Целевой статус спринта:** ВЫПОЛНЕНО  
**Целевая платформа:** Xiaomi Redmi Note 13 4G NFC (`sapphire` / `sapphiren`, SM6225)  

---

## 1. Резюме спринта Lead Developer

В соответствии с распределением задач на координационной доске (`coordinator/TASK_BOARD.md`), выполнен блок «Сборка и Unit-тестирование»:
1. **Анализ совместимости сборочного тулчейна (JDK 25 vs JDK 21 LTS):** Выявлены фундаментальные ограничения Android Gradle Plugin (AGP) 8.7.3 при запуске на JDK 25 и сертифицирована стабильная конфигурация на базе JDK 21 LTS.
2. **Устранение Windows-специфичных коллизий путей с не-ASCII символами:** Настроен параметр `android.overridePathCheck=true` во всех модулях и создана прозрачная ASCII-точка монтирования `C:\Users\barab\UVOS\uvos_dev` для детерминированной работы тестового загрузчика классов JUnit.
3. **Реализация чистой бизнес-логики и Unit-тестов для критических модулей:**
   - **`apps/uvos-security`:** Выделены чистые доменные компоненты `FirewallRuleBuilder` и `PackageSecurityValidator`, написан тестовый сьют `FirewallAndSecurityTest` (7 тестов).
   - **`apps/uvos-files`:** Разработан генератор защищённых команд `RootCommandBuilder` с экранированием шелл-аргументов для исключения command injection, написан тестовый сьют `FilesAndRootCommandsTest` (5 тестов).
   - **`apps/uvos-vpn`:** Реализована валидация конфигурации туннеля WireGuard `WireGuardTunnelConfig`, написан тестовый сьют `WireGuardTunnelConfigTest` (5 тестов).
4. **Результаты тестирования:** **17 из 17 тестов успешно пройдены (100% pass rate, 0 failures, 0 errors)**.
5. **Инвентаризация собранных бинарных пакетов:** Подтверждена готовность 16 APK-файлов системы (15 приложений + системный лаунчер/щиток `uvos-dashboard`).

---

## 2. Анализ совместимости сред сборки (JDK Compatibility Audit)

### 2.1. Инцидент с JDK 25 (`C:\Program Files\Android\Android Studio\jbr`)
В рамках координационного задания была выполнена попытка сборки и запуска тестов с использованием JDK 25, поставляемого в составе канареечных сборок Android Studio.

```
FAILURE: Build failed with an exception.
* What went wrong:
25.0.4
> Unsupported class file major version 69 / Android Gradle plugin 8.7.3 requires Java 17-21
```

* **Причина сбоя:** Android Gradle Plugin (AGP) версии `8.7.3` поддерживает запуск только на версиях Java до `Java 21` включительно. Байткод Java 25 (class major version 69) и внутренние структуры JVM 25 вызывают фатальное исключение на этапе конфигурации плагинов `com.android.application` и Kotlin KGP `2.0.21`.
* **Вердикт:** Использование JDK 25 для сборки проекта UVOS на базе AGP 8.7.3 **недопустимо** до выхода обновлений AGP 9.x.

### 2.2. Сертифицированный стандарт: JDK 21 LTS (`C:\Users\barab\UVOS\Java\jdk-21.0.13+9`)
Для всех сборочных операций, компиляции Kotlin/Java и запуска тестовых раннеров утверждена среда:
* **Java Runtime:** `Eclipse Temurin JDK 21.0.13+9 (LTS)` (`C:\Users\barab\UVOS\Java\jdk-21.0.13+9`)
* **Gradle:** `8.14.5` (`C:\Users\barab\UVOS\Gradle\gradle-8.14.5\bin\gradle.bat`)
* **Android SDK:** `C:\Users\barab\UVOS\Sdk` (Build-Tools `35.0.0`, Platform `android-35`)
* **Kotlin:** `2.0.21` (с Jetpack Compose Compiler extension)

### 2.3. Решение проблемы путей Windows с кириллицей
Рабочая директория проекта на хосте (`C:\Users\barab\Desktop\Проекты AI\uvos`) содержит кириллический сегмент `Проекты AI`.
1. **AGP Path Warning:** Плагин `com.android.application` блокирует сборку с предупреждением `Your project path contains non-ASCII characters`.
   * **Решение:** Во все `gradle.properties` всех модулей добавлена директива `android.overridePathCheck=true`.
2. **JUnit Worker ClassNotFoundException:** Загрузчик классов JUnit (`BuiltinClassLoader`) в ОС Windows при запуске тест-воркера через сокет получает экранированный URL-путь `%D0%9F%D1%80%D0%BE...`, что приводит к `ClassNotFoundException` при попытке загрузить класс теста.
   * **Решение:** Создана символическая точка соединения файловой системы NTFS:
     ```powershell
     New-Item -ItemType Junction -Path "C:\Users\barab\UVOS\uvos_dev" -Target "C:\Users\barab\Desktop\Проекты AI\uvos"
     ```
   Это позволило выполнять тесты через чистый ASCII-путь `C:\Users\barab\UVOS\uvos_dev`, сохраняя полную прозрачность и мгновенную синхронизацию всех файлов проекта.

---

## 3. Результаты Unit-тестирования критических сервисов

### 3.1. Сводная таблица прогона тестов

| Модуль | Класс теста | Всего тестов | Успешно | Ошибки | Время выполнения | Статус |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| **`apps/uvos-security`** | `com.uvos.security.FirewallAndSecurityTest` | 7 | 7 | 0 | 0.095 с | ✅ **PASSED** |
| **`apps/uvos-files`** | `com.uvos.files.FilesAndRootCommandsTest` | 5 | 5 | 0 | 0.128 с | ✅ **PASSED** |
| **`apps/uvos-vpn`** | `com.uvos.vpn.WireGuardTunnelConfigTest` | 5 | 5 | 0 | 0.085 с | ✅ **PASSED** |
| **ИТОГО** | **3 сьюта** | **17** | **17** | **0** | **0.308 с** | ✅ **100% УСПЕХ** |

---

### 3.2. Детальный разбор тестов модуля `apps/uvos-security`

Файл реализации доменной логики: `apps/uvos-security/app/src/main/java/com/uvos/security/domain/FirewallRuleBuilder.kt`  
Файл валидации пакетов: `apps/uvos-security/app/src/main/java/com/uvos/security/domain/PackageSecurityValidator.kt`  
Файл тестов: `apps/uvos-security/app/src/test/java/com/uvos/security/FirewallAndSecurityTest.kt`

```
Testsuite: com.uvos.security.FirewallAndSecurityTest
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.095 sec

[OK] testAppRiskScoring (17 ms)
[OK] testBlockUidCommandsGeneratesOwnerRule (68 ms)
[OK] testNegativeUidThrowsException (1 ms)
[OK] testUidCategorization (1 ms)
[OK] testInitChainCommandsStructure (1 ms)
[OK] testPackageNameValidation (3 ms)
[OK] testUnblockUidCommandsDeletesOwnerRule (1 ms)
```

**Проверенная логика:**
1. `testInitChainCommandsStructure`: Проверяет создание и привязку изолированной цепочки `UVOS_SHIELD` в `iptables` и `ip6tables` в таблице `OUTPUT`.
2. `testBlockUidCommandsGeneratesOwnerRule`: Проверяет генерацию правила блокировки для заданного UID приложения (`iptables -I UVOS_SHIELD 1 -m owner --uid-owner <UID> -j DROP`) и последующий сброс активных сокетов через `ss -K dst 0.0.0.0/0 uid <UID>`.
3. `testUnblockUidCommandsDeletesOwnerRule`: Проверяет удаление правила из обеих цепочек IPv4 и IPv6 без нарушения целостности других правил.
4. `testNegativeUidThrowsException`: Проверяет валидацию граничных условий (выброс `IllegalArgumentException` при передаче отрицательного UID).
5. `testUidCategorization`: Проверяет корректное разграничение системных служб (`UID < 10000`, `isSystemUid=true`) и пользовательских приложений (`UID >= 10000`, `isAppUid=true`).
6. `testPackageNameValidation`: Проверяет строгое регулярное выражение имени пакета Android (`^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$`), блокируя спецсимволы и попытки шелл-инъекций.
7. `testAppRiskScoring`: Проверяет расчёт интегрального индекса риска изоляции (`IsolationRiskScore`) на основе набора критических разрешений (`INTERNET`, `READ_PRIVILEGED_PHONE_STATE`, `ACCESS_FINE_LOCATION`, `PACKAGE_USAGE_STATS`).

---

### 3.3. Детальный разбор тестов модуля `apps/uvos-files`

Файл реализации доменной логики: `apps/uvos-files/app/src/main/java/com/uvos/files/data/engine/RootCommandBuilder.kt`  
Файл тестов: `apps/uvos-files/app/src/test/java/com/uvos/files/FilesAndRootCommandsTest.kt`

```
Testsuite: com.uvos.files.FilesAndRootCommandsTest
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.128 sec

[OK] testPosixPermissionsFromRwx (51 ms)
[OK] testShellArgumentEscaping (13 ms)
[OK] testRemountCommandGeneration (50 ms)
[OK] testCommandGeneration (6 ms)
[OK] testPosixPermissionsFromOctal (5 ms)
```

**Проверенная логика:**
1. `testShellArgumentEscaping`: Проверяет функцию защиты от шелл-инъекций `escapeShellArg()`. Убеждается, что любые пути, содержащие одинарные кавычки, пробелы, точки с запятой (`/sdcard/test'; rm -rf /; '`), оборачиваются в одинарные кавычки с заменой внутренних `'` на `'\''`, нейтрализуя вектор атаки Command Injection.
2. `testCommandGeneration`: Проверяет генерацию команд `ls`, `rm`, `mv`, `cp`, `chmod` с обязательным экранированием аргументов через `escapeShellArg`.
3. `testRemountCommandGeneration`: Проверяет генерацию команд монтирования разделов (`mount -o remount,rw /system`, `mount -o remount,ro /vendor`).
4. `testPosixPermissionsFromOctal`: Проверяет корректный парсинг восьмеричных прав POSIX (`0755` -> `rwxr-xr-x`, `0644` -> `rw-r--r--`, `0600` -> `rw-------`).
5. `testPosixPermissionsFromRwx`: Проверяет обратный парсинг символьной маски прав в числовую маску и расчёт битов `read`, `write`, `execute` для категорий User, Group, Others.

---

### 3.4. Детальный разбор тестов модуля `apps/uvos-vpn`

Файл реализации доменной логики: `apps/uvos-vpn/app/src/main/java/com/uvos/vpn/core/WireGuardTunnelConfig.kt`  
Файл тестов: `apps/uvos-vpn/app/src/test/java/com/uvos/vpn/WireGuardTunnelConfigTest.kt`

```
Testsuite: com.uvos.vpn.WireGuardTunnelConfigTest
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.085 sec

[OK] testInvalidIpAddressValidation (70 ms)
[OK] testKillSwitchLeakProtection (1 ms)
[OK] testIpv4Helper (1 ms)
[OK] testDefaultConfigIsValid (1 ms)
[OK] testInvalidMtuValidation (11 ms)
```

**Проверенная логика:**
1. `testDefaultConfigIsValid`: Проверяет валидность фабричной конфигурации туннеля по умолчанию (MTU 1420, адрес клиента `10.0.0.2/32`, шлюз `10.0.0.1`, DNS `1.1.1.1`, полный маршрут `0.0.0.0/0`).
2. `testInvalidMtuValidation`: Проверяет валидацию допустимого диапазона MTU для туннелей WireGuard (1280...1500). Любые значения ниже 1280 (минимум IPv6) или выше 1500 (стандартный Ethernet) выбрасывают `IllegalArgumentException`.
3. `testInvalidIpAddressValidation`: Проверяет валидацию IPv4-адресов и масок подсети (отсечение октетов `> 255`, некорректных префиксов CIDR `> 32`, некорректных форматов строк).
4. `testIpv4Helper`: Проверяет вспомогательный парсер IP-адресов на граничных значениях (`0.0.0.0`, `255.255.255.255`).
5. `testKillSwitchLeakProtection`: Проверяет работу свойства `isLeakProtected()`. Гарантирует, что туннель считается защищённым от утечек только тогда, когда в `allowedIps` входит маршрут по умолчанию `0.0.0.0/0` (полный туннель) и включен флаг `killSwitch = true`.

---

## 4. Полный реестр бинарных пакетов (16 APK Inventory)

Все 16 модулей системы собраны в бинарные APK-пакеты (архитектура `arm64-v8a` / pure Kotlin, Android API 35) и размещены в рабочем репозитории:

| № | Приложение | Модуль | Имя пакета | Тип сборки | Размер APK | Статус |
| :-: | :--- | :--- | :--- | :---: | :---: | :---: |
| 1 | **Dashboard (Launcher)** | `uvos-dashboard` | `com.uvos.dashboard` | Debug | 45.4 MB | ✅ Готов |
| 2 | **Settings** | `apps/uvos-settings` | `com.uvos.settings` | Debug | 48.9 MB | ✅ Готов |
| 3 | **Terminal** | `apps/uvos-terminal` | `com.uvos.terminal` | Debug | 47.7 MB | ✅ Готов |
| 4 | **Files** | `apps/uvos-files` | `com.uvos.files` | Debug | 49.3 MB | ✅ Готов |
| 5 | **Security Shield** | `apps/uvos-security` | `com.uvos.security` | Debug | 49.2 MB | ✅ Готов |
| 6 | **VPN Client** | `apps/uvos-vpn` | `com.uvos.vpn` | Debug | 49.6 MB | ✅ Готов |
| 7 | **Messages** | `apps/uvos-messages` | `com.uvos.messages` | Debug | 47.9 MB | ✅ Готов |
| 8 | **Mail Client** | `apps/uvos-mail` | `com.uvos.mail` | Debug | 51.5 MB | ✅ Готов |
| 9 | **Music Player** | `apps/uvos-music` | `com.uvos.music` | Debug | 55.4 MB | ✅ Готов |
| 10 | **Notes Vault** | `apps/uvos-notes` | `com.uvos.notes` | Debug | 49.8 MB | ✅ Готов |
| 11 | **Downloader** | `apps/uvos-downloader` | `com.uvos.downloader` | Debug | 51.6 MB | ✅ Готов |
| 12 | **Calendar** | `apps/uvos-calendar` | `com.uvos.calendar` | Debug | 50.3 MB | ✅ Готов |
| 13 | **Clock & Timer** | `apps/uvos-clock` | `com.uvos.clock` | Debug | 49.7 MB | ✅ Готов |
| 14 | **Compass & Azimuth** | `apps/uvos-compass` | `com.uvos.compass` | Debug | 48.7 MB | ✅ Готов |
| 15 | **Gallery & Vault** | `apps/uvos-gallery` | `com.uvos.gallery` | Debug | 49.6 MB | ✅ Готов |
| 16 | **Camera & Reticle** | `apps/uvos-camera` | `com.uvos.camera` | Debug | 58.0 MB | ✅ Готов |

---

## 5. Выводы и рекомендации для следующего этапа

1. **Тулчейн стабилен:** Установлен и протестирован сборочный пайплайн на базе `JDK 21 LTS + Gradle 8.14.5 + AGP 8.7.3`. Сборка всех модулей и прогон тестов происходят воспроизводимо и без ошибок.
2. **Изоляция доменного слоя:** Вынесение генерации команд и валидаций в независимые Kotlin-объекты (`FirewallRuleBuilder`, `RootCommandBuilder`, `PackageSecurityValidator`, `WireGuardTunnelConfig`) позволяет выполнять быстрое модульное тестирование за миллисекунды без необходимости запуска эмулятора или подключения физического устройства.
3. **Безопасность проверена кодом:** Unit-тестами подтверждено строгое экранирование одинарных кавычек при root-вызовах, корректная генерация правил iptables с UID-владельцами и валидация сетевых конфигураций WireGuard.
4. **Готовность к системной интеграции:** Проект полностью готов к переходу к следующей фазе координации — развёртыванию и тестированию на физическом устройстве `sapphire` (после соответствующего распоряжения пользователя).
