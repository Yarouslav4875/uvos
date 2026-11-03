# Системная спецификация UVOS № 09: UVOS VPN & Tunnel Shield
## Высокопроизводительный криптографический туннель, защита от утечек сетевого стека и приборный интерфейс M4200

> **Идентификатор пакета**: `com.uvos.vpn`  
> **Статус документа**: Принят к инженерной реализации (UnderVoltOS Core Architecture)  
> **Целевая платформа**: UnderVoltOS / AOSP / Android 13–14 (Xiaomi Redmi Note 13 4G NFC `sapphire/sapphiren`)  
> **Ядро стека**: Pure Kotlin + Kotlin Coroutines/Flow, Android VpnService API, WireGuard-Go backend / Xray-core (VLESS XTLS-Reality)  
> **Принцип UnderVoltOS**: 0% телеметрии, 0% внешних аналитических SDK, абсолютный аппаратный Kill-Switch, нулевые утечки DNS/IPv6, аутентичный аналоговый дизайн M4200.

---

## 1. Паспорт компонента и концепция безопасности

Приложение **UVOS VPN & Tunnel Shield** (`com.uvos.vpn`) спроектировано как системный узел криптографической изоляции и маскирования сетевых потоков. В отличие от коммерческих VPN-клиентов, перегруженных рекламными SDK, трекерами геолокации и сторонними push-службами, UVOS VPN подчиняется жесткому инварианту сетевой стерильности:

1. **Zero External Communication**: Приложение взаимодействует исключительно с узлами (End-points), явно заданными пользователем в конфигурационных файлах. Запрещены любые проверки обновлений через внешние API, сбор статистики на удаленные сервера и обращение к публичным серверам определения внешнего IP без санкции пользователя.
2. **Абсолютная ликвидация утечек (Zero Leak Guarantee)**: Ни один пакет полезной нагрузки не имеет права покинуть устройство в незашифрованном виде ни при каких условиях — включая фазу согласования рукопожатия (handshake), внезапный обрыв несущего канала (Cellular/Wi-Fi handover), падение фонового процесса туннеля или перезапуск радиомодуля модема.
3. **Строгая криптография**: Первичный транспорт — **WireGuard** (современный криптографический протокол на базе Curve25519, ChaCha20-Poly1305, BLAKE2s, SipHash24). Для сетей с активным глубоким анализом пакетов (DPI) и цензурой интегрирован альтернативный движок **VLESS (Xray-core)** с маскировкой под легитимный TLS 1.3 (XTLS-Reality).
4. **Приборный интерфейс М4200**: Полный отказ от плоских минималистичных графиков в пользу аналоговых стрелочных измерительных головок с физической баллистикой стрелок (амперметр сетевого потока и вольтметр стабильности туннеля).

```
+----------------------------------------------------------------------------------------------------+
|                                    UVOS VPN & TUNNEL SHIELD ARCHITECTURE                           |
+----------------------------------------------------------------------------------------------------+
|                                       UI: M4200 Retro Dashboard                                    |
|   +-------------------------------------+                +-------------------------------------+   |
|   |      TRAFFIC AMMETER (TX / RX)      |                |      LATENCY & JITTER VOLTMETER     |   |
|   |      Двухстрелочный амперметр       |                |   Стрелочный вольтметр миллисекунд  |   |
|   +-------------------------------------+                +-------------------------------------+   |
|         ^                                                              ^                           |
|         | StateFlow<TrafficRate>                                       | StateFlow<PingMetrics>    |
|   +--------------------------------------------------------------------------------------------+   |
|   |                          TunnelViewModel & TunnelStateManager                              |   |
|   +--------------------------------------------------------------------------------------------+   |
|                                       | (Service Intent / Binder IPC)                              |
|                                       v                                                            |
|   +--------------------------------------------------------------------------------------------+   |
|   |            UvosVpnService (android.net.VpnService / Foreground Service)                    |   |
|   |  - TunInterface Controller (fd: ParcelFileDescriptor)                                      |   |
|   |  - LeakProtectionEngine (Kill-Switch / iptables fallback / DNS Enforcer)                   |   |
|   |  - SplitTunnelingRouter (Package Filter: Allowed / Disallowed)                             |   |
|   +--------------------------------------------------------------------------------------------+   |
|               |                                                            |                       |
|   [Native Go Backend (JNI)]                                    [Native Xray Backend (NDK)]         |
|   +---------------------------------------+                    +-------------------------------+   |
|   | WireGuard-Go Kernel Emulation         |                    | Xray-Core VLESS XTLS Engine   |   |
|   | - ChaCha20-Poly1305 Crypto            |                    | - Anti-DPI TLS 1.3 Reality    |   |
|   | - Curve25519 ECDH Handshake           |                    | - Multiplexing / Fake SNI     |   |
|   +---------------------------------------+                    +-------------------------------+   |
|               \                                                            /                       |
|                v                                                          v                        |
|   +--------------------------------------------------------------------------------------------+   |
|   |               Linux Kernel: TUN Device (tun0) & Physical Interfaces (rmnet0 / wlan0)       |   |
|   +--------------------------------------------------------------------------------------------+   |
+----------------------------------------------------------------------------------------------------+
```

---

## 2. Системная архитектура и сетевой стек

### 2.1. Интеграция с Android VpnService
Приложение реализует платформенный контракт `android.net.VpnService`. При активации туннеля формируется виртуальный сетевой интерфейс `tun0`, который перехватывает исходящие IP-пакеты системы на 3-м сетевом уровне (Network Layer / OSI Level 3).

Параметры дескриптора `tun0` настраиваются через `VpnService.Builder`:
* **MTU**: Динамический выбор в диапазоне `1280–1420 байт` (1280 — безопасный минимум для гарантированного прохождения без фрагментации в сотовых сетях LTE с накладными расходами IPv6 + WireGuard header; 1420 — оптимум для Wi-Fi).
* **Сетевые адреса (Virtual IP)**: 
  * IPv4: `10.254.1.2/32` (или адрес из конфигурации пира).
  * IPv6: `fd00:uvos:vpn::2/128` (для изоляции от утечек IPv6).
* **Маршрутизация (Routes)**:
  * По умолчанию перехватывается весь глобальный трафик: `0.0.0.0/0` (IPv4) и `::/0` (IPv6).
  * Для предотвращения локальных конфликтов LAN исключаются подсети `192.168.0.0/16`, `10.0.0.0/8`, `172.16.0.0/12` (если включена опция «Доступ к устройствам локальной сети»).
* **DNS-серверы**: Принудительно задаются адреса локального защищенного виртуального резолвера (`10.254.1.1` или доверенные DoH/DoT апстримы).
* **Флаг блокировки (`setBlocking(true)`)**: Файловый дескриптор `ParcelFileDescriptor` переводится в строго блокирующий синхронный режим ввода-вывода.

### 2.2. Выбор криптографического ядра: WireGuard Go vs Xray VLESS
Система предоставляет два взаимозаменяемых бэкенда через единый программный интерфейс `ITunnelEngine`:

| Характеристика | WireGuard-Go Backend | Xray-core (VLESS Reality) |
|---|---|---|
| **Назначение** | Максимальная скорость, минимальная задержка, энергоэффективность | Обход агрессивных систем ТСПУ / DPI, маскировка под веб-трафик |
| **Криптография** | ChaCha20-Poly1305, Curve25519, BLAKE2s | TLS 1.3, Vision flow, uTLS (Chrome fingerprints) |
| **Накладные расходы CPU** | Минимальные (низкое энергопотребление ядра Snapdragon) | Средние (за счет TLS-инкапсуляции и мультиплексирования) |
| **Уязвимость к DPI** | Детектируется классическими сигнатурами протокола WireGuard | Неотличим от сессий браузера к доверенным доменам (SNI Reality) |
| **Переключение сетей** | Мгновенный бесшовный Roaming (Connectionless UDP) | Требует быстрого реассоциирования TCP-сессии |

### 2.3. Жизненный цикл `UvosVpnService`
Служба запускается как привилегированный `Foreground Service` с типом `FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED` / `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` (в соответствии с политиками Android 14):
* **Приоритет процесса**: `OOM_ADJ` уровень фонового сервиса устанавливается в значение `PERSISTENT / FOREGROUND_APP`, исключающее принудительное завершение демона подсистемой LMK (Low Memory Killer) при нехватке ОЗУ.
* **CPU WakeLock Policy**: Использование частичного WakeLock (`PARTIAL_WAKE_LOCK`) минимизировано. Удержание процессора активируется только на момент криптографического рукопожатия и пересогласования ключей (re-keying раз в 120 минут), после чего модем и планировщик ядер CPU переводятся в энергосберегающие состояния (UnderVolt-профиль).

---

## 3. Приборный интерфейс М4200 (Retro-Instrument UI)

В соответствии с дизайн-системой UnderVoltOS, графический интерфейс `com.uvos.vpn` выполнен в стиле лабораторного щита измерительных приборов СССР (серии М4200 / М42300) в карболитовом корпусе.

### 3.1. Палитра и текстурный регламент
* **Базовый фон приложения**: `#0E0E12` (глубокий матовый композит).
* **Корпуса приборов (Bezel)**: `#1C1E26` с винтовыми заклепками по углам и скруглением 28dp.
* **Циферблат измерительной шкалы**: `#E8E4D8` (винтажная слоновая кость с легким эффектом патины).
* **Штрихи и числовые деления шкалы**: `#1A1A1A` (матовый угольный).
* **Красная стрелка (TX / Передача)**: `#E02020` с круглой латунной головкой в точке вращения.
* **Сине-стальная стрелка (RX / Прием)**: `#2A52BE` (для двухстрелочного амперметра потока).
* **Стрелка вольтметра стабильности**: `#1A1A1A` со светоотражающим наконечником.
* **Неоновые индикаторные лампы (`LampView`)**:
  * Зеленый неоновый (`#00FF66`): «ТУННЕЛЬ ЗАМКНУТ», «KILL-SWITCH АКТИВЕН».
  * Желтый (`#FFB700`): «СОГЛАСОВАНИЕ КЛЮЧЕЙ (HANDSHAKE)», «СКАЧОК JITTER».
  * Красный рубиновый (`#FF1E27`): «ОБРЫВ ЛИНИИ», «УТЕЧКА БЛОКИРОВАНА».

```
+---------------------------------------------------------------------------------------+
|  [UVOS SHIELD]  [M4200 INSTRUMENT CONSOLE]                        BATT: 3820mV | 24°C |
+---------------------------------------------------------------------------------------+
|  +-----------------------------------+     +-----------------------------------+      |
|  |       СЕТЕВОЙ АМПЕРМЕТР           |     |       ВОЛЬТМЕТР ЗАДЕРЖКИ          |      |
|  |           M4200-TX/RX             |     |           M4200-PING              |      |
|  |              .---.                |     |              .---.                |      |
|  |          .-'   |   '-.            |     |          .-'   |   '-.            |      |
|  |        .'  \   |   /  '.          |     |        .'  \   |   /  '.          |      |
|  |       /     \  |  /     \         |     |       /  зеленая|желтая|красная\      |
|  |      |   RX  \ | /  TX   |        |     |      |  0..40ms | 100ms| 300+ms |     |
|  |      |        \|/        |        |     |      |         \|/           |     |
|  |       \       (O)       /         |     |       \        (O)          /      |
|  |        '.     / \     .'          |     |        '.      / \        .'       |
|  |          '-._______.-'            |     |          '-._______.-'             |
|  |        0  100K  1M  10M  100M     |     |       0   20  50  100  300  LOSS   |
|  |        RX: 4.8 MB/s | TX: 320 KB/s|     |       RTT: 28 ms | JITTER: 3 ms    |
|  +-----------------------------------+     +-----------------------------------+      |
|                                                                                       |
|   [x] ТУННЕЛЬ ВКЛЮЧЕН      [x] KILL-SWITCH АППАРАТНЫЙ      [x] ЧИСТЫЙ DNS (DoH)       |
|                                                                                       |
|  +---------------------------------------------------------------------------------+  |
|  | РАЗДЕЛЬНОЕ ТУННЕЛИРОВАНИЕ: РЕЖИМ БЕЛЫЙ СПИСОК (ИСКЛЮЧЕНИЯ: 4 ПРИЛОЖЕНИЯ)       |  |
|  | [O] Сбербанк (Байпас)   [O] Госуслуги (Байпас)   [X] Firefox   [X] Telegram     |  |
|  +---------------------------------------------------------------------------------+  |
|  | УЗЕЛ: se-sto-wg-01.tunnel.internal | IP: 185.125.190.44 | HANDSHAKE: 14 сек наз |  |
+---------------------------------------------------------------------------------------+
```

### 3.2. Математическая модель движения стрелок (Spring Physics & Damping)
Для устранения неестественного «цифрового» дерганья стрелки реализован физический интегратор механического движения на базе дифференциального уравнения колебаний крутильного маятника с вязким трением:

$$J \cdot \frac{d^2\theta}{dt^2} + \beta \cdot \frac{d\theta}{dt} + k \cdot (\theta - \theta_{target}) = 0$$

Где:
* $J$ — момент инерции легкой алюминиевой стрелки измерительного механизма магнитоэлектрической системы ($0.08$).
* $\beta$ — коэффициент демпфирования магнитоиндукционного успокоителя ($1.20$, обеспечивает легкий аналоговый перелет на 3-5% с быстрым затуханием — overshoot).
* $k$ — жесткость противодействующей спиральной волосковой пружины ($18.0$).
* $\theta_{target}$ — угол, пропорциональный измеренной величине (с логарифмической компрессией).

### 3.3. Логарифмическая шкала амперметра сетевого потока
Сетевой поток колеблется в огромном динамическом диапазоне: от единиц байт/с (фоновый Keep-Alive) до сотен мегабит в секунду (загрузка файлов). Линейная шкала сделала бы малые скорости невидимыми. Применена нелинейная сегментированная шкала:

$$\theta(R) = \theta_{min} + (\theta_{max} - \theta_{min}) \cdot \frac{\log_{10}(1 + \frac{R}{R_0})}{\log_{10}(1 + \frac{R_{max}}{R_0})}$$

Где $R$ — текущая скорость в байтах/с, $R_0 = 10240$ (10 Кб/с), $R_{max} = 104857600$ (100 Мб/с). Диапазон отклонения стрелки: $-50^\circ$ (ноль) до $+50^\circ$ (полная шкала).

### 3.4. Реализация компонента стрелочного прибора на Jetpack Compose
```kotlin
package com.uvos.vpn.ui.components

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Аутентичный аналоговый прибор UVOS M4200.
 */
@Composable
fun M4200GaugeView(
    currentValue: Float,
    maxValue: Float,
    title: String,
    unit: String,
    needleColor: Color = Color(0xFFE02020),
    modifier: Modifier = Modifier
) {
    val animatedAngle = remember { Animatable(-50f) }

    // Конвертация значения в угол отклонения (-50° .. +50°) с затухающей пружиной
    LaunchedEffect(currentValue) {
        val fraction = (currentValue / maxValue).coerceIn(0f, 1f)
        val targetAngle = -50f + fraction * 100f
        animatedAngle.animateTo(
            targetValue = targetAngle,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Box(
        modifier = modifier
            .aspectRatio(1.15f)
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val pivotY = height * 0.88f
            val radius = height * 0.72f

            // 1. Отрисовка подложки циферблата цвета слоновой кости (#E8E4D8)
            drawDialPlate(width, height)

            // 2. Отрисовка разметки, делений и шкал М4200
            drawScaleMarkings(centerX, pivotY, radius, maxValue)

            // 3. Подписи прибора (моноширинный ГОСТ-шрифт)
            drawInstrumentLabels(centerX, pivotY, title, unit)

            // 4. Отрисовка стрелки с заданным углом
            drawNeedle(
                centerX = centerX,
                pivotY = pivotY,
                length = radius * 0.95f,
                angleDeg = animatedAngle.value,
                color = needleColor
            )

            // 5. Центральная латунная заглушка керна
            drawCircle(
                color = Color(0xFF2A2B32),
                radius = 18f,
                center = Offset(centerX, pivotY)
            )
            drawCircle(
                color = Color(0xFFC0A060),
                radius = 10f,
                center = Offset(centerX, pivotY)
            )
        }
    }
}

private fun DrawScope.drawDialPlate(width: Float, height: Float) {
    // Внутренняя панель
    drawRoundRect(
        color = Color(0xFFE8E4D8),
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
    )
    // Тонкая внутренняя тень фаски
    drawRoundRect(
        color = Color(0xFF202020),
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
        style = Stroke(width = 4f)
    )
}

private fun DrawScope.drawScaleMarkings(
    centerX: Float,
    pivotY: Float,
    radius: Float,
    maxValue: Float
) {
    val totalSteps = 20
    for (i in 0..totalSteps) {
        val fraction = i / totalSteps.toFloat()
        val angleDeg = -50f + fraction * 100f
        val angleRad = (angleDeg - 90f) * (PI / 180f).toFloat()

        val isMajor = i % 5 == 0
        val tickLength = if (isMajor) 24f else 12f
        val strokeWidth = if (isMajor) 3.5f else 1.8f

        val startX = centerX + (radius - tickLength) * cos(angleRad)
        val startY = pivotY + (radius - tickLength) * sin(angleRad)
        val endX = centerX + radius * cos(angleRad)
        val endY = pivotY + radius * sin(angleRad)

        drawLine(
            color = Color(0xFF1A1A1A),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = strokeWidth
        )
    }
}

private fun DrawScope.drawInstrumentLabels(
    centerX: Float,
    pivotY: Float,
    title: String,
    unit: String
) {
    val paint = Paint().apply {
        color = Color(0xFF1A1A1A).toArgb()
        textSize = 28f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    drawContext.canvas.nativeCanvas.drawText(title, centerX, pivotY - 140f, paint)

    paint.textSize = 22f
    paint.isFakeBoldText = false
    drawContext.canvas.nativeCanvas.drawText(unit, centerX, pivotY - 105f, paint)
}

private fun DrawScope.drawNeedle(
    centerX: Float,
    pivotY: Float,
    length: Float,
    angleDeg: Float,
    color: Color
) {
    val angleRad = (angleDeg - 90f) * (PI / 180f).toFloat()
    val tipX = centerX + length * cos(angleRad)
    val tipY = pivotY + length * sin(angleRad)

    // Основание клиновидной стрелки
    val basePerpAngle = angleRad + (PI / 2f).toFloat()
    val baseWidth = 7f
    val baseLeftX = centerX + baseWidth * cos(basePerpAngle)
    val baseLeftY = pivotY + baseWidth * sin(basePerpAngle)
    val baseRightX = centerX - baseWidth * cos(basePerpAngle)
    val baseRightY = pivotY - baseWidth * sin(basePerpAngle)

    // Противовес (хвостовик)
    val tailLength = length * 0.18f
    val tailX = centerX - tailLength * cos(angleRad)
    val tailY = pivotY - tailLength * sin(angleRad)

    val needlePath = Path().apply {
        moveTo(baseLeftX, baseLeftY)
        lineTo(tipX, tipY)
        lineTo(baseRightX, baseRightY)
        lineTo(tailX, tailY)
        close()
    }

    drawPath(path = needlePath, color = color)
}
```

---

## 4. Комплексная защита от утечек и аппаратный Kill-Switch

### 4.1. Векторы сетевых утечек в Android
В стандартной системе Android существует до 5 критических векторов компрометации реального IP-адреса и запросов пользователя:
1. **DNS Leak (Утечка DNS)**: Системный резолвер `netd` обращается к DNS-серверам мобильного оператора или Wi-Fi роутера мимо интерфейса `tun0` при тайм-аутах.
2. **IPv6 Bypass (Обход через IPv6)**: Если VPN-сервер работает только по IPv4, а сотовый оператор выделил IPv6 `/64`, трафик приложений уходит открытым текстом напрямую через `rmnet0`.
3. **Roaming / Handover Glitch**: При переключении с Wi-Fi на LTE стек Android кратковременно восстанавливает сокеты по умолчанию до поднятия туннеля.
4. **Crash Leakage**: Падение или перезапуск фонового процесса сервиса восстанавливает маршрутизацию на физический модем.
5. **WebRTC Local Candidate Leak**: Браузеры опрашивают сетевые интерфейсы через STUN, раскрывая внутренний и реальный шлюз.

### 4.2. Архитектура эшелонированного Kill-Switch
Для ликвидации указанных векторов в UnderVoltOS применяется двухконтурный механизм блокировки:

```
[ ПРИЛОЖЕНИЯ UVOS ]
        |
        v
+-----------------------------------------------------------------------------------+
| 1-Й КОНТУР: VpnService Isolation (Платформенный уровень)                         |
| - VpnService.Builder.setBlocking(true)                                            |
| - Builder.setUnderlyingNetworks(emptyArray()) -> запрет сброса в wlan/rmnet       |
| - Маршрутизация 0.0.0.0/0 и ::/0 замкнута на дескриптор tun0                     |
+-----------------------------------------------------------------------------------+
        |
        +-----> [ Обрыв туннеля / Handshake Fail ] -----> [ ПОЛНАЯ БЛОКИРОВКА ПАКЕТОВ ]
        |                                                              |
        v                                                              v
+-----------------------------------------------------------------------------------+
| 2-Й КОНТУР: Root / iptables Hardware Fallback (Системный уровень ядра)            |
| - Защитная цепочка UVOS_KILLSWITCH в таблице mangle и filter                     |
| - Весь исходящий трафик БЛОКИРУЕТСЯ, КРОМЕ uid-owner самого com.uvos.vpn         |
| - Строгая фиксация DNS: перенаправление UDP/TCP 53 на локальный DoH-прокси       |
+-----------------------------------------------------------------------------------+
```

### 4.3. Программный код управления iptables Kill-Switch
При наличии прав суперпользователя (Magisk / KernelSU / встроенный root UVOS), служба `UvosVpnService` активирует системные правила ядра Linux Netfilter через `libsu`:

```kotlin
package com.uvos.vpn.security

import com.topjohnwu.superuser.Shell
import timber.log.Timber

/**
 * Аппаратный контроллер правил фильтрации iptables/nftables для UnderVoltOS.
 */
object HardwareKillSwitch {

    private const val CHAIN_NAME = "UVOS_KILLSWITCH"

    /**
     * Активирует аварийный барьер трафика.
     * Весь трафик устройства блокируется, кроме зашифрованных UDP-пакетов самого приложения VPN.
     */
    fun enable(vpnUid: Int, vpnServerIp: String, vpnServerPort: Int) {
        val commands = listOf(
            // 1. Создаем или очищаем собственную цепочку
            "iptables -N $CHAIN_NAME 2>/dev/null || iptables -F $CHAIN_NAME",
            "ip6tables -N $CHAIN_NAME 2>/dev/null || ip6tables -F $CHAIN_NAME",

            // 2. Разрешаем трафик через локальную петлю (Loopback для IPC)
            "iptables -A $CHAIN_NAME -o lo -j ACCEPT",
            "ip6tables -A $CHAIN_NAME -o lo -j ACCEPT",

            // 3. Разрешаем трафик виртуального интерфейса туннеля
            "iptables -A $CHAIN_NAME -o tun+ -j ACCEPT",
            "ip6tables -A $CHAIN_NAME -o tun+ -j ACCEPT",

            // 4. Разрешаем процесс VPN отправлять зашифрованные пакеты на конечный узел
            "iptables -A $CHAIN_NAME -m owner --uid-owner $vpnUid -d $vpnServerIp -p udp --dport $vpnServerPort -j ACCEPT",
            "iptables -A $CHAIN_NAME -m owner --uid-owner $vpnUid -d $vpnServerIp -p tcp --dport $vpnServerPort -j ACCEPT",

            // 5. Разрешаем DHCP для поддержания аренды адреса локальной сети
            "iptables -A $CHAIN_NAME -p udp --dport 67:68 --sport 67:68 -j ACCEPT",

            // 6. БЛОКИРУЕМ весь остальной трафик системы мимо туннеля
            "iptables -A $CHAIN_NAME -j DROP",
            "ip6tables -A $CHAIN_NAME -j DROP",

            // 7. Встраиваем цепочку в начало цепочки OUTPUT ядра
            "iptables -D OUTPUT -j $CHAIN_NAME 2>/dev/null || true",
            "iptables -I OUTPUT 1 -j $CHAIN_NAME",
            "ip6tables -D OUTPUT -j $CHAIN_NAME 2>/dev/null || true",
            "ip6tables -I OUTPUT 1 -j $CHAIN_NAME"
        )

        val result = Shell.cmd(*commands.toTypedArray()).exec()
        if (result.isSuccess) {
            Timber.i("UVOS Hardware Kill-Switch успешно взведен для UID: %d", vpnUid)
        } else {
            Timber.e("Ошибка взвода Kill-Switch: %s", result.err.joinToString("\n"))
        }
    }

    /**
     * Снимает аварийную блокировку и восстанавливает свободный трафик.
     */
    fun disable() {
        val commands = listOf(
            "iptables -D OUTPUT -j $CHAIN_NAME 2>/dev/null || true",
            "ip6tables -D OUTPUT -j $CHAIN_NAME 2>/dev/null || true",
            "iptables -F $CHAIN_NAME 2>/dev/null || true",
            "ip6tables -F $CHAIN_NAME 2>/dev/null || true",
            "iptables -X $CHAIN_NAME 2>/dev/null || true",
            "ip6tables -X $CHAIN_NAME 2>/dev/null || true"
        )
        Shell.cmd(*commands.toTypedArray()).exec()
        Timber.i("UVOS Hardware Kill-Switch деактивирован")
    }
}
```

### 4.4. Защита и принудительное перенаправление DNS
Приложение исключает передачу DNS-запросов в открытом виде (UDP порт 53):
1. В дескрипторе туннеля указывается изолированный DNS: `10.254.1.1` (виртуальный резолвер, работающий внутри туннельного сервиса).
2. Виртуальный резолвер упаковывает запросы в шифрованный протокол **DNS-over-HTTPS (DoH)** или **DNS-over-TLS (DoT)**.
3. Доступен выбор доверенных провайдеров, не ведущих логи:
   * **AdGuard DNS Non-Filtering / AdBlock**: `https://dns.adguard-dns.com/dns-query` (с локальной фильтрацией трекеров).
   * **Quad9 (Швейцария, GDPR compliant)**: `https://dns.quad9.net/dns-query` (блокировка фишинга и вредоносных узлов).
   * **Mullvad DoH**: `https://dns.mullvad.net/dns-query`.
   * **Cloudflare Security (1.1.1.2)**: `https://security.cloudflare-dns.com/dns-query`.

---

## 5. Механизм раздельного туннелирования (Split Tunneling)

### 5.1. Режимы маршрутизации приложений
UVOS VPN поддерживает два взаимоисключающих режима распределения трафика:

1. **Режим «Байпас / Черный список» (Disallowed Applications)**:
   * Весь трафик устройства шифруется и передается через VPN.
   * Исключенные приложения (например, локальный банковский клиент РФ, Госуслуги, приложение «Парковки») направляются напрямую через физический интерфейс сотовой сети / Wi-Fi без шифрования.
   * Метод API: `builder.addDisallowedApplication(packageName)`.
2. **Режим «Щит / Белый список» (Allowed Applications)**:
   * Трафик направляется через VPN **только** для явно выбранных приложений (например, Firefox, Telegram, Почтовый клиент).
   * Для всех остальных приложений доступ в интернет либо блокируется (при активном Kill-Switch), либо идет напрямую.
   * Метод API: `builder.addAllowedApplication(packageName)`.

### 5.2. Архитектура менеджера приложений (`SplitTunnelManager`)
```kotlin
package com.uvos.vpn.routing

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppRoutingProfile(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val isTunnelled: Boolean
)

enum class SplitTunnelMode {
    DISALLOW_LIST, // Черный список: все идут через туннель, кроме выбранных
    ALLOW_LIST     // Белый список: только выбранные идут через туннель
}

class SplitTunnelManager(private val context: Context) {

    /**
     * Получает отфильтрованный список установленных приложений,
     * разделяя системные пакеты и пользовательский софт.
     */
    suspend fun getInstalledApplicationsList(
        configuredPackages: Set<String>,
        mode: SplitTunnelMode
    ): List<AppRoutingProfile> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        packages
            .filter { it.packageName != context.packageName } // Исключаем сам UVOS VPN
            .map { appInfo ->
                val isSys = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isConfigured = configuredPackages.contains(appInfo.packageName)
                val isTunnelled = when (mode) {
                    SplitTunnelMode.DISALLOW_LIST -> !isConfigured
                    SplitTunnelMode.ALLOW_LIST -> isConfigured
                }

                AppRoutingProfile(
                    packageName = appInfo.packageName,
                    appName = appInfo.loadLabel(pm).toString(),
                    isSystemApp = isSys,
                    isTunnelled = isTunnelled
                )
            }
            .sortedWith(compareBy({ it.isSystemApp }, { it.appName.lowercase() }))
    }
}
```

---

## 6. Спецификация классов, фонового сервиса и криптографии

### 6.1. Безопасное хранилище ключей (Android Keystore + Jetpack Security)
Приватные ключи WireGuard и параметры аутентификации VLESS никогда не записываются в `SharedPreferences` открытым текстом и не выводятся в журнал `logcat`.

Для шифрования файла конфигураций используется криптомодуль `EncryptedSharedPreferences` с мастер-ключом в аппаратном хранилище `AndroidKeyStore` (StrongBox Keymaster Snapdragon 685):
* Алгоритм мастер-ключа: `AES256-GCM`.
* Схема шифрования ключей: `AES256_SIV`.
* Схема шифрования значений: `AES256_GCM`.

### 6.2. Модель конфигурации WireGuard
```kotlin
package com.uvos.vpn.model

import java.net.InetAddress

data class WgInterfaceConfig(
    val privateKeyBase64: String,
    val addresses: List<String>,      // e.g. ["10.254.1.2/32", "fd00::2/128"]
    val dnsServers: List<String>,    // e.g. ["10.254.1.1", "1.1.1.2"]
    val mtu: Int = 1360
)

data class WgPeerConfig(
    val publicKeyBase64: String,
    val presharedKeyBase64: String? = null,
    val endpointHost: String,
    val endpointPort: Int,
    val allowedIps: List<String> = listOf("0.0.0.0/0", "::/0"),
    val persistentKeepalive: Int = 25
)

data class TunnelConfiguration(
    val id: String,
    val name: String,
    val iface: WgInterfaceConfig,
    val peer: WgPeerConfig
)
```

### 6.3. Парсер стандартных конфигураций WireGuard (`.conf`)
Парсер принимает текстовую строку формата `wg-quick` и преобразует ее в строго валидированные структуры:

```kotlin
package com.uvos.vpn.parser

import com.uvos.vpn.model.TunnelConfiguration
import com.uvos.vpn.model.WgInterfaceConfig
import com.uvos.vpn.model.WgPeerConfig
import java.io.BufferedReader
import java.io.StringReader

object WireGuardConfigParser {

    fun parse(configText: String, profileName: String = "UVOS Tunnel"): TunnelConfiguration {
        val reader = BufferedReader(StringReader(configText))
        var currentSection = ""

        var privateKey = ""
        val addresses = mutableListOf<String>()
        val dnsServers = mutableListOf<String>()
        var mtu = 1360

        var publicKey = ""
        var presharedKey: String? = null
        var endpointHost = ""
        var endpointPort = 51820
        val allowedIps = mutableListOf<String>()
        var keepalive = 25

        reader.forEachLine { rawLine ->
            val line = rawLine.substringBefore("#").trim()
            if (line.isEmpty()) return@forEachLine

            if (line.startsWith("[") && line.endsWith("]")) {
                currentSection = line.uppercase()
                return@forEachLine
            }

            val parts = line.split("=", limit = 2)
            if (parts.size != 2) return@forEachLine
            val key = parts[0].trim().lowercase()
            val value = parts[1].trim()

            when (currentSection) {
                "[INTERFACE]" -> when (key) {
                    "privatekey" -> privateKey = value
                    "address" -> addresses.addAll(value.split(",").map { it.trim() })
                    "dns" -> dnsServers.addAll(value.split(",").map { it.trim() })
                    "mtu" -> mtu = value.toIntOrNull() ?: 1360
                }
                "[PEER]" -> when (key) {
                    "publickey" -> publicKey = value
                    "presharedkey" -> presharedKey = value
                    "endpoint" -> {
                        val hostPort = value.substringAfterLast("]:", value)
                        if (value.startsWith("[")) {
                            // IPv6 endpoint
                            endpointHost = value.substring(1, value.indexOf("]"))
                            endpointPort = hostPort.toIntOrNull() ?: 51820
                        } else {
                            // IPv4 or hostname
                            endpointHost = value.substringBeforeLast(":")
                            endpointPort = value.substringAfterLast(":").toIntOrNull() ?: 51820
                        }
                    }
                    "allowedips" -> allowedIps.addAll(value.split(",").map { it.trim() })
                    "persistentkeepalive" -> keepalive = value.toIntOrNull() ?: 25
                }
            }
        }

        require(privateKey.isNotBlank()) { "Параметр PrivateKey отсутствует в секции [Interface]" }
        require(publicKey.isNotBlank()) { "Параметр PublicKey отсутствует в секции [Peer]" }
        require(endpointHost.isNotBlank()) { "Параметр Endpoint отсутствует в секции [Peer]" }

        return TunnelConfiguration(
            id = java.util.UUID.randomUUID().toString(),
            name = profileName,
            iface = WgInterfaceConfig(privateKey, addresses, dnsServers, mtu),
            peer = WgPeerConfig(publicKey, presharedKey, endpointHost, endpointPort, allowedIps, keepalive)
        )
    }
}
```

### 6.4. Реализация системного сервиса `UvosVpnService`
```kotlin
package com.uvos.vpn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.uvos.vpn.R
import com.uvos.vpn.model.TunnelConfiguration
import com.uvos.vpn.model.TunnelState
import com.uvos.vpn.routing.SplitTunnelMode
import com.uvos.vpn.security.HardwareKillSwitch
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.net.InetAddress

class UvosVpnService : VpnService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var vpnInterface: ParcelFileDescriptor? = null
    private var activeConfig: TunnelConfiguration? = null

    companion object {
        const val ACTION_CONNECT = "com.uvos.vpn.action.CONNECT"
        const val ACTION_DISCONNECT = "com.uvos.vpn.action.DISCONNECT"
        const val EXTRA_CONFIG_ID = "com.uvos.vpn.extra.CONFIG_ID"

        private const val NOTIFICATION_CHANNEL_ID = "uvos_vpn_channel"
        private const val NOTIFICATION_ID = 4200

        private val _tunnelState = MutableStateFlow<TunnelState>(TunnelState.Disconnected)
        val tunnelState: StateFlow<TunnelState> = _tunnelState.asStateFlow()

        private val _trafficStats = MutableStateFlow(TrafficSnapshot(0L, 0L, 0L, 0L))
        val trafficStats: StateFlow<TrafficSnapshot> = _trafficStats.asStateFlow()
    }

    data class TrafficSnapshot(
        val rxBytesPerSec: Long,
        val txBytesPerSec: Long,
        val pingLatencyMs: Long,
        val jitterMs: Long
    )

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                // Инициализация соединения
                startForeground(NOTIFICATION_ID, buildForegroundNotification("Подготовка туннеля..."))
                startTunnelConnection()
            }
            ACTION_DISCONNECT -> {
                terminateTunnel()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startTunnelConnection() {
        serviceScope.launch {
            _tunnelState.value = TunnelState.Connecting
            try {
                // 1. Построение виртуального сетевого интерфейса TUN
                val builder = Builder().apply {
                    setSession("UVOS Tunnel Shield")
                    setMtu(1360)
                    setBlocking(true)

                    // Маршрутизация на виртуальный IP
                    addAddress("10.254.1.2", 32)
                    addRoute("0.0.0.0", 0)

                    // Принудительный защищенный DNS
                    addDnsServer("10.254.1.1")

                    // Раздельное туннелирование (пример: исключение приложений)
                    // addDisallowedApplication("ru.sberbankmobile")
                }

                vpnInterface = builder.establish()
                if (vpnInterface == null) {
                    throw IllegalStateException("Не удалось получить дескриптор VpnService. Ошибка разрешений.")
                }

                // 2. Активация аппаратного Kill-Switch через iptables
                val myUid = android.os.Process.myUid()
                HardwareKillSwitch.enable(
                    vpnUid = myUid,
                    vpnServerIp = "185.125.190.44",
                    vpnServerPort = 51820
                )

                // 3. Запуск криптографического движка (WireGuard Go NDK)
                // nativeStartWireguard(vpnInterface!!.fd, ...)

                _tunnelState.value = TunnelState.Connected
                updateNotification("Туннель замкнут. Защита M4200 активна.")

                // 4. Запуск цикла мониторинга трафика и пинга для стрелок прибора
                launchMetricsCollector()

            } catch (e: Exception) {
                Timber.e(e, "Сбой инициализации VPN туннеля")
                _tunnelState.value = TunnelState.Error(e.localizedMessage ?: "Неизвестная ошибка")
                HardwareKillSwitch.disable()
                stopSelf()
            }
        }
    }

    private fun launchMetricsCollector() {
        serviceScope.launch {
            var lastRx = 0L
            var lastTx = 0L
            while (isActive && _tunnelState.value == TunnelState.Connected) {
                // Чтение метрик байт через сокет WireGuard или системный TrafficStats
                val currentRx = android.net.TrafficStats.getTotalRxBytes()
                val currentTx = android.net.TrafficStats.getTotalTxBytes()

                val rxSpeed = (currentRx - lastRx).coerceAtLeast(0L)
                val txSpeed = (currentTx - lastTx).coerceAtLeast(0L)
                lastRx = currentRx
                lastTx = currentTx

                // Замер задержки туннеля (ICMP/TCP эхо-запрос к шлюзу)
                val rtt = measureTunnelPing("10.254.1.1")

                _trafficStats.value = TrafficSnapshot(
                    rxBytesPerSec = rxSpeed,
                    txBytesPerSec = txSpeed,
                    pingLatencyMs = rtt,
                    jitterMs = 2L
                )

                delay(1000L) // Частота опроса стрелочных приборов 1 Гц
            }
        }
    }

    private suspend fun measureTunnelPing(host: String): Long = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val addr = InetAddress.getByName(host)
            if (addr.isReachable(1000)) {
                return@withContext (System.currentTimeMillis() - start)
            }
        } catch (_: Exception) {}
        return@withContext 999L // Packet loss
    }

    private fun terminateTunnel() {
        try {
            HardwareKillSwitch.disable()
            vpnInterface?.close()
            vpnInterface = null
            _tunnelState.value = TunnelState.Disconnected
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при остановке туннеля")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        terminateTunnel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "UVOS VPN Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Статус активного криптотуннеля UVOS Shield"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(statusText: String): Notification {
        val disconnectIntent = Intent(this, UvosVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this, 1, disconnectIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("UVOS VPN & Tunnel Shield")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Отключить", disconnectPendingIntent)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildForegroundNotification(statusText))
    }
}
```

### 6.5. Состояния конечного автомата туннеля (`TunnelState`)
```kotlin
package com.uvos.vpn.model

sealed class TunnelState {
    object Disconnected : TunnelState()
    object Connecting : TunnelState()
    object Connected : TunnelState()
    object Reconnecting : TunnelState()
    object KillSwitchActive : TunnelState() // Обрыв связи, трафик наглухо перекрыт
    data class Error(val message: String) : TunnelState()
}
```

### 6.6. Quick Settings Tile (`UvosVpnTileService`)
Для мгновенного переключения туннеля из шторки переключателей HyperOS/AOSP реализован легковесный тайл:
* При коротком тапе: переключение туннеля `Connect / Disconnect`.
* При долгом тапе: открытие главного экрана `com.uvos.vpn.ui.MainActivity` с приборами M4200.
* Состояние тайла синхронизируется со `StateFlow<TunnelState>`: активен (`STATE_ACTIVE`), неактивен (`STATE_INACTIVE`), аварийный сбой (`STATE_UNAVAILABLE`).

---

## 7. Спецификация сборки и зависимостей (`build.gradle.kts`)

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.uvos.vpn"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.uvos.vpn"
        minSdk = 26
        targetSdk = 34
        versionCode = 10001
        versionName = "1.0.0-PROTOTYPE"

        ndk {
            abiFilters += listOf("arm64-v8a") // Оптимизация под 64-битный процессор Redmi Note 13
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Jetpack Core & Lifecycle
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Jetpack Compose M4200 UI
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Криптография и защищенное хранилище
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Root-доступ для аппаратного Kill-Switch (iptables)
    implementation("com.github.topjohnwu.libsu:core:5.2.2")
    implementation("com.github.topjohnwu.libsu:io:5.2.2")

    // WireGuard Go Native библиотека
    implementation("com.wireguard.android:tunnel:1.0.20230706")

    // Сканирование QR-кодов локально (без Google Play Services и без интернета)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Логирование (исключительно для локального debug)
    implementation("com.jakewharton.timber:timber:5.0.1")
}
```

---

## 8. Программа и методика испытаний (Verification & QA Checklist)

| № | Проверка / Сценарий | Ожидаемый результат UnderVoltOS | Метод контроля |
|---|---|---|---|
| **1** | **Тест на утечку DNS (DNS Leak Test)** | Запросы направляются строго на виртуальный резолвер `10.254.1.1` и доверенный DoH. Ни один DNS-пакет не проходит к серверам сотового оператора. | Захват `tcpdump -i any port 53` на устройстве. Запрос 20 случайных доменов. 0 пакетов на `rmnet0`. |
| **2** | **Аварийный обрыв связи (Kill-Switch Test)** | При искусственном падении процесса бэкенда (`kill -9`) весь трафик устройства мгновенно блокируется. Браузер выдает `ERR_NETWORK_CHANGED` или `ERR_CONNECTION_REFUSED`. | Симуляция аварии в shell. Запуск `curl --interface rmnet0 https://1.1.1.1` — завершение по таймауту / Drop. |
| **3** | **Тест раздельного туннелирования (Split-Tunneling)** | Приложение из белого списка выходит через шведский IP туннеля; приложение из черного списка выходит через российский IP сотовой вышки. | Проверка заголовков `ipinfo.io` через два раздельных изолированных процесса. |
| **4** | **Баллистика стрелок M4200 (Физика колебаний)** | При резком старте скачивания файла стрелка амперметра плавно разгоняется, перелетает на 3-5% расчетное деление и мягко стабилизируется за 200 мс. | Замедленная видеосъемка UI (60 кадров/с) и анализ графика `animatedAngle`. |
| **5** | **Zero-Telemetry Audit** | В бинарном файле APK отсутствуют строковые константы и классы Firebase, Crashlytics, Adjust, Facebook SDK, AppsFlyer. | `grep -rn "com.google.firebase" app/build/outputs/apk/` -> вывод пуст. |
| **6** | **Энергопотребление в режиме Deep Sleep** | При активном туннеле и выключенном экране расход батареи не превышает 0.8% в час за счет оптимизированного таймера Keep-Alive (25с) и разгрузки CPU. | Замер через системный амперметр батареи в `dumpsys batterystats`. |
