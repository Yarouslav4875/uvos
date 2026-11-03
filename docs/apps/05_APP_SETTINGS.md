# Спецификация и руководство разработчика: UVOS Settings (`com.uvos.settings`)

> **Компонент**: Центральная системная панель управления UnderVoltOS  
> **Идентификатор пакета**: `com.uvos.settings`  
> **Целевая платформа**: UnderVoltOS (Xiaomi Redmi Note 13 4G NFC `sapphire`/`sapphiren`, Qualcomm Snapdragon 685 SM6225, Linux 5.15)  
> **Архитектура**: Clean Architecture + MVI (Model-View-Intent), Jetpack Compose  
> **Дизайн-код**: Retro-Instrumental Console / Приборный пульт М4200  
> **Статус**: Архитектурная спецификация и руководство по разработке

---

## 1. Назначение и концепция приложения

Приложение **UVOS Settings** (`com.uvos.settings`) — это единый командный пульт и системный центр управления операционной системы UnderVoltOS. В отличие от стандартных настроек Android (AOSP/MIUI), перегруженных рекламными рекомендациями, аккаунтами и запутанными меню, UVOS Settings спроектирован по принципу **авиационной приборной панели и индустриального щита управления**.

### Ключевые функции
1. **Глобальный селектор профилей**: аппаратное и программное переключение режимов безопасности системы («СТЕРИЛЬНО», «СВЯЗЬ», «СВОЙ»).
2. **Аппаратная телеметрия реального времени**: опрос чипсета Snapdragon 685 (8 ядер, 2 кластера), термальных зон и вольтажа батареи через прямые интерфейсы ядра `sysfs` с отображением на аналоговых приборах М4200.
3. **Прямое управление аппаратными параметрами (Settings Provider)**: управление яркостью, частотой обновления 60/120 Гц, аудиопотоками, ориентацией и индустриальными моноширинными шрифтами.
4. **Модуль аудита приложений (App Audit)**: сквозная инвентаризация установленных пакетов с категоризацией по бейджам изоляции («СИСТЕМА», «ИЗОЛИРОВАНО», «1 КАНАЛ СЕТИ») и мгновенным управлением AppOps/iptables.

---

## 2. Архитектура: Clean Architecture + MVI

Приложение строится по строгой трёхслойной Clean Architecture с реактивным паттерном **MVI (Model-View-Intent)** на базе Kotlin Coroutines и `StateFlow`.

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           PRESENTATION LAYER                             │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │                    Jetpack Compose Dashboard UI                    │  │
│  │  (M4200DialGauge, M4200ToggleSwitch, InstrumentTile, AppAuditCard) │  │
│  └─────────────────────────────────▲──────────────────────────────────┘  │
│                     ViewState Flow │ │ User Intents                      │
│  ┌─────────────────────────────────┴─▼────────────────────────────────┐  │
│  │                      SettingsViewModel (MVI)                       │  │
│  │    Reducer: (CurrentState, Intent) -> NewState + SideEffect        │  │
│  └─────────────────────────────────▲──────────────────────────────────┘  │
└────────────────────────────────────┼─────────────────────────────────────┘
                                     │ Domain Models & UseCases
┌────────────────────────────────────▼─────────────────────────────────────┐
│                              DOMAIN LAYER                                │
│  ┌─────────────────────────┐ ┌──────────────────────┐ ┌───────────────┐  │
│  │ SwitchProfileUseCase    │ │ ObserveTelemetryUC   │ │ AuditAppsUC   │  │
│  ├─────────────────────────┤ ├──────────────────────┤ ├───────────────┤  │
│  │ SetDisplayParamUseCase  │ │ SetAudioStreamUseCase│ │ ToggleAppIsoUC│  │
│  └─────────────────────────┘ └──────────────────────┘ └───────────────┘  │
│            │                             │                    │          │
│            ▼                             ▼                    ▼          │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │                     Repository Interfaces                          │  │
│  │    ProfileRepository, HardwareRepository, SystemSettingsRepository  │  │
│  └─────────────────────────────────▲──────────────────────────────────┘  │
└────────────────────────────────────┼─────────────────────────────────────┘
                                     │ DTOs & Streams
┌────────────────────────────────────▼─────────────────────────────────────┐
│                               DATA LAYER                                 │
│  ┌────────────────────────┐  ┌─────────────────────┐  ┌───────────────┐  │
│  │   SysfsDataSource      │  │ SettingsProviderDS  │  │ PackageManager│  │
│  │ (/sys, /proc/stat)     │  │ (Settings.System/G) │  │ & AppOpsDS    │  │
│  └────────────────────────┘  └─────────────────────┘  └───────────────┘  │
│  ┌────────────────────────┐  ┌─────────────────────┐  ┌───────────────┐  │
│  │ RadioControllerDS      │  │ NetworkRegulatorDS  │  │ LocalSettings │  │
│  │ (Telephony/Wifi/BT/NFC)│  │ (iptables/nftables) │  │ DataStore     │  │
│  └────────────────────────┘  └─────────────────────┘  └───────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
```

### 2.1 Контракты MVI (State, Intent, Effect)

```kotlin
package com.uvos.settings.presentation.contract

import com.uvos.settings.domain.model.*

/**
 * Единое неизменяемое состояние консоли настроек
 */
data class SettingsState(
    val currentProfile: SystemProfile = SystemProfile.CUSTOM,
    val isProfileSwitching: Boolean = false,
    
    // Аппаратная телеметрия
    val batteryVoltageMv: Int = 0,
    val batteryTemperatureC: Float = 0f,
    val socTemperatureC: Float = 0f,
    val cpuTotalLoadPercent: Int = 0,
    val coreFrequenciesMhz: List<Int> = List(8) { 0 },
    
    // Параметры экрана и звука
    val screenBrightness: Int = 128,
    val isAutoBrightness: Boolean = false,
    val refreshRateHz: Int = 120,
    val screenTimeoutSeconds: Int = 30,
    val volumeRing: Int = 5,
    val volumeMedia: Int = 7,
    val volumeAlarm: Int = 6,
    val activeFont: SystemFontFamily = SystemFontFamily.JETBRAINS_MONO,
    
    // Аудит приложений
    val installedApps: List<AuditedAppInfo> = emptyList(),
    val appFilter: AppFilterCategory = AppFilterCategory.ALL,
    val isAuditScanning: Boolean = false,
    
    val errorMessage: String? = null
)

/**
 * Пользовательские намерения (Intents)
 */
sealed interface SettingsIntent {
    data class SelectProfile(val profile: SystemProfile) : SettingsIntent
    data class ChangeBrightness(val value: Int) : SettingsIntent
    data class ToggleAutoBrightness(val enabled: Boolean) : SettingsIntent
    data class ChangeRefreshRate(val rateHz: Int) : SettingsIntent
    data class ChangeScreenTimeout(val seconds: Int) : SettingsIntent
    data class ChangeVolume(val streamType: AudioStreamType, val level: Int) : SettingsIntent
    data class SelectFont(val font: SystemFontFamily) : SettingsIntent
    
    // Действия аудита
    data class FilterApps(val category: AppFilterCategory) : SettingsIntent
    data class ToggleAppIsolation(val packageName: String, val isolate: Boolean) : SettingsIntent
    data class SetAppNetworkAccess(val packageName: String, val allowNetwork: Boolean) : SettingsIntent
    data class TerminateApp(val packageName: String) : SettingsIntent
    object RefreshAppsList : SettingsIntent
}

/**
 * Однократные побочные эффекты (Side Effects)
 */
sealed interface SettingsEffect {
    data class ShowToast(val message: String) : SettingsEffect
    data class PlayToggleHaptic(val strength: HapticStrength) : SettingsEffect
    data class RequestSystemPermission(val permission: String) : SettingsEffect
}
```

---

## 3. Дизайн-система приборной консоли (M4200 UI)

Интерфейс строится строго в индустриальном ретро-стиле с матовыми тёмными подложками, винтажными аналоговыми индикаторами со шкалами цвета слоновой кости и металлическими механическими переключателями.

### 3.1 Цветовая палитра токенов
* **`uvos_background`**: `#0E0E12` — глубокий матовый графитово-чёрный фон консоли.
* **`uvos_tile`**: `#1C1E26` — текстурированный модуль/плитка прибора с фаской и скруглением 24dp.
* **`uvos_tile_border`**: `#242838` — контур шасси консоли (толщина 1.5dp).
* **`uvos_dial`**: `#E8E4D8` — циферблат стрелочного вольтметра/амперметра цвета состаренной кости.
* **`uvos_dial_text`**: `#1A1A1A` — насечки и цифры шкалы с высокой контрастностью.
* **`uvos_red`**: `#E02020` — стрелки приборов М4200, индикаторы тревоги и блокировки.
* **`uvos_green_lamp`**: `#00FF66` — активные разрешённые узлы, штатный режим.
* **`uvos_amber_lamp`**: `#FFB000` — предупреждения, ограниченный режим «СВЯЗЬ».
* **`uvos_text_primary`**: `#E2E4EC` — технический светлый текст.
* **`uvos_text_secondary`**: `#71768E` — подписи единиц измерений и служебные лейблы.

---

### 3.2 Компонент: Стрелочный циферблат М4200 (`M4200DialGauge`)

Стрелочный индикатор базируется на дизайне стрелочного милливольтметра/амперметра постоянного тока **М4200**. Стрелка имеет физическую инерционность (затухающее колебание с плавностью ~600 мс) и отбрасывает мягкую тень на циферблат.

```kotlin
package com.uvos.settings.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun M4200DialGauge(
    value: Float,
    minValue: Float,
    maxValue: Float,
    unitTitle: String,
    deviceLabel: String,
    modifier: Modifier = Modifier,
    dangerThreshold: Float? = null
) {
    val clampedValue = value.coerceIn(minValue, maxValue)
    val targetAngle = -45f + ((clampedValue - minValue) / (maxValue - minValue)) * 90f
    
    // Эмуляция баллистики стрелки стрелочного механизма магнитоэлектрической системы
    val animatedAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 120f),
        label = "M4200NeedleSpring"
    )

    Box(
        modifier = modifier
            .size(width = 170.dp, height = 120.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val pivot = Offset(w / 2f, h * 1.05f)
            val dialRadius = w * 0.75f

            // Подложка шкалы - слоновая кость
            drawArc(
                color = Color(0xFFE8E4D8),
                startAngle = 215f,
                sweepAngle = 110f,
                useCenter = true,
                topLeft = Offset(pivot.x - dialRadius, pivot.y - dialRadius),
                size = androidx.compose.ui.geometry.Size(dialRadius * 2, dialRadius * 2)
            )

            // Основная дуговая линия шкалы
            drawArc(
                color = Color(0xFF1A1A1A),
                startAngle = 225f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(pivot.x - dialRadius * 0.85f, pivot.y - dialRadius * 0.85f),
                size = androidx.compose.ui.geometry.Size(dialRadius * 1.7f, dialRadius * 1.7f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Засечки шкалы (21 шаг)
            val totalTicks = 20
            for (i in 0..totalTicks) {
                val tickFraction = i.toFloat() / totalTicks
                val tickAngleDeg = 225f + tickFraction * 90f
                val tickAngleRad = Math.toRadians(tickAngleDeg.toDouble())
                val isMajor = i % 5 == 0
                val tickLen = if (isMajor) 10.dp.toPx() else 5.dp.toPx()
                
                val rOuter = dialRadius * 0.85f
                val rInner = rOuter - tickLen

                val isDanger = dangerThreshold != null && 
                    (minValue + tickFraction * (maxValue - minValue)) >= dangerThreshold

                val pOuter = Offset(
                    (pivot.x + rOuter * cos(tickAngleRad)).toFloat(),
                    (pivot.y + rOuter * sin(tickAngleRad)).toFloat()
                )
                val pInner = Offset(
                    (pivot.x + rInner * cos(tickAngleRad)).toFloat(),
                    (pivot.y + rInner * sin(tickAngleRad)).toFloat()
                )

                drawLine(
                    color = if (isDanger) Color(0xFFE02020) else Color(0xFF1A1A1A),
                    start = pInner,
                    end = pOuter,
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                )
            }

            // Стрелка с тенью
            rotate(degrees = animatedAngle, pivot = pivot) {
                // Тень стрелки
                drawLine(
                    color = Color(0x33000000),
                    start = Offset(pivot.x + 3f, pivot.y + 3f),
                    end = Offset(pivot.x + 3f, pivot.y - dialRadius * 0.88f + 3f),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Алая стрелка индикатора
                drawLine(
                    color = Color(0xFFE02020),
                    start = pivot,
                    end = Offset(pivot.x, pivot.y - dialRadius * 0.88f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Центральный латунный болт поворотной рамки
            drawCircle(
                color = Color(0xFF2C2D35),
                radius = 7.dp.toPx(),
                center = pivot
            )
            drawCircle(
                color = Color(0xFF8E92A2),
                radius = 3.dp.toPx(),
                center = pivot
            )
        }

        // Лейбл прибора в стиле заводской маркировки
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = deviceLabel,
                color = Color(0xFF33353D),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = unitTitle,
                color = Color(0xFF1A1A1A),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
```

---

### 3.3 Компонент: Промышленный тумблер переключения профилей (`M4200ToggleSwitch`)

Трёхпозиционный механический тумблер с металлическим рычагом, тактильным откликом при фиксации и трёхцветными контрольными лампами индикации.

```kotlin
package com.uvos.settings.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.settings.domain.model.SystemProfile

@Composable
fun M4200ToggleSwitch(
    currentProfile: SystemProfile,
    onProfileSelected: (SystemProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val profiles = listOf(SystemProfile.STERILE, SystemProfile.COMMUNICATION, SystemProfile.CUSTOM)
    val selectedIndex = profiles.indexOf(currentProfile)

    Column(
        modifier = modifier
            .background(Color(0xFF161820), RoundedCornerShape(16.dp))
            .border(1.5.dp, Color(0xFF282C3C), RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "СЕЛЕКТОР ПРОФИЛЯ UVOS",
            color = Color(0xFF71768E),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            profiles.forEach { profile ->
                val isSelected = profile == currentProfile
                val lampColor = when (profile) {
                    SystemProfile.STERILE -> if (isSelected) Color(0xFFE02020) else Color(0xFF3A0D0D)
                    SystemProfile.COMMUNICATION -> if (isSelected) Color(0xFFFFB000) else Color(0xFF382700)
                    SystemProfile.CUSTOM -> if (isSelected) Color(0xFF00FF66) else Color(0xFF063013)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onProfileSelected(profile) }
                ) {
                    // Контрольная лампа состояния
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .shadow(if (isSelected) 6.dp else 0.dp, CircleShape, spotColor = lampColor)
                            .background(lampColor, CircleShape)
                            .border(1.dp, Color(0xFF43485E), CircleShape)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Обозначение позиции тумблера
                    Text(
                        text = profile.displayName,
                        color = if (isSelected) Color(0xFFE2E4EC) else Color(0xFF565B70),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Металлическая кулиса механизма тумблера
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0A0B0E))
                .border(1.dp, Color(0xFF202330), RoundedCornerShape(14.dp))
                .padding(3.dp)
        ) {
            val alignBias by animateFloatAsState(
                targetValue = when (selectedIndex) {
                    0 -> -1f
                    1 -> 0f
                    else -> 1f
                },
                label = "ToggleLeverBias"
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.33f)
                    .align(Alignment(alignBias, 0f))
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF8E93A6), Color(0xFF4C5061), Color(0xFF2C303E))
                        )
                    )
                    .border(1.dp, Color(0xFFAEB4CC), RoundedCornerShape(10.dp))
            )
        }
    }
}
```

---

## 4. Переключатель глобальных профилей системы

В UnderVoltOS управление периферией и правами подчиняется жесткой матрице состояний. Переключение глобального профиля атомарно меняет аппаратные контроллеры и правила фильтрации.

| Подсистема | Профиль «СТЕРИЛЬНО» | Профиль «СВЯЗЬ» | Профиль «СВОЙ» |
|---|---|---|---|
| **Сотовая связь (GSM/LTE)** | Отключена (`AirplaneMode = ON`, RIL выключен) | Только голос и SMS, передача данных строго по whitelist | Согласно пользовательским тумблерам |
| **Wi-Fi / Hotspot** | Аппаратный Reset (`WifiManager.setWifiEnabled(false)`) | Выключен (вручную разрешается по запросу) | Согласно пользовательским тумблерам |
| **Bluetooth / NFC** | Отключены через драйвер | Отключены | Согласно пользовательским тумблерам |
| **Геолокация (GPS/GNSS)** | Провайдеры выключены, координаты подменяются 0,0 | Отключена | По правилам Контрольного щитка |
| **Сенсоры (Камера/Микрофон)** | SensorPrivacy API: аппаратное глушение + возвращение нуля | Доступны только RIL и SMS-клиенту | По правилам AppOps |
| **Сетевой регулятор** | iptables: DROP ALL (`INPUT/OUTPUT`) | iptables: разрешены только UID телефонного стека | Матрица per-app правил |

### 4.1 Реализация в репозитории управления профилями

```kotlin
package com.uvos.settings.data.repository

import android.content.Context
import android.hardware.SensorPrivacyManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.bluetooth.BluetoothAdapter
import android.nfc.NfcAdapter
import android.provider.Settings
import com.topjohnwu.superuser.Shell
import com.uvos.settings.domain.model.SystemProfile
import com.uvos.settings.domain.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SystemProfileRepositoryImpl(
    private val context: Context,
    private val sensorPrivacyManager: SensorPrivacyManager?,
    private val wifiManager: WifiManager,
    private val bluetoothAdapter: BluetoothAdapter?,
    private val nfcAdapter: NfcAdapter?
) : ProfileRepository {

    override suspend fun applyProfile(profile: SystemProfile): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            when (profile) {
                SystemProfile.STERILE -> executeSterilePolicy()
                SystemProfile.COMMUNICATION -> executeCommunicationPolicy()
                SystemProfile.CUSTOM -> executeCustomPolicy()
            }
        }
    }

    private fun executeSterilePolicy() {
        // 1. Авиарежим и отключение радиопередатчиков
        Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 1)
        wifiManager.isWifiEnabled = false
        bluetoothAdapter?.disable()

        // 2. Блокировка сенсоров (Камера, Микрофон) через SensorPrivacy API
        sensorPrivacyManager?.let { spm ->
            if (spm.supportsSensorToggle(SensorPrivacyManager.Sensors.CAMERA)) {
                spm.setSensorPrivacy(SensorPrivacyManager.Sensors.CAMERA, true)
            }
            if (spm.supportsSensorToggle(SensorPrivacyManager.Sensors.MICROPHONE)) {
                spm.setSensorPrivacy(SensorPrivacyManager.Sensors.MICROPHONE, true)
            }
        }

        // 3. Ядерный файрвол: полная блокировка всех исходящих и входящих пакетов
        Shell.cmd(
            "iptables -F OUTPUT",
            "iptables -P OUTPUT DROP",
            "iptables -F INPUT",
            "iptables -P INPUT DROP",
            "ip link set wlan0 down"
        ).exec()
    }

    private fun executeCommunicationPolicy() {
        // Выключение авиарежима, но отключение второстепенных каналов
        Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0)
        wifiManager.isWifiEnabled = false
        bluetoothAdapter?.disable()

        // iptables: разрешить только системные UID радиомодуля (telephony UID 1001, dns)
        Shell.cmd(
            "iptables -F OUTPUT",
            "iptables -P OUTPUT DROP",
            "iptables -A OUTPUT -m owner --uid-owner 1001 -j ACCEPT", // Radio RIL
            "iptables -A OUTPUT -p udp --dport 53 -j ACCEPT",
            "iptables -P INPUT ACCEPT"
        ).exec()
    }

    private fun executeCustomPolicy() {
        // Восстановление таблицы правил из локальной БД щитка
        Shell.cmd(
            "iptables -P OUTPUT ACCEPT",
            "iptables -P INPUT ACCEPT"
        ).exec()
    }
}
```

---

## 5. Интеграция с Android Settings Provider и аппаратным обеспечением

UVOS Settings напрямую модифицирует ключи `Settings.System`, `Settings.Global` и `Settings.Secure`, исключая посреднические оверлеи вендоров.

### 5.1 Матрица системных ключей управления

```kotlin
package com.uvos.settings.data.provider

import android.content.ContentResolver
import android.media.AudioManager
import android.provider.Settings

class SystemSettingsController(
    private val contentResolver: ContentResolver,
    private val audioManager: AudioManager
) {
    // Яркость экрана (0 - 255)
    fun setBrightness(value: Int) {
        val clamped = value.coerceIn(1, 255)
        Settings.System.putInt(
            contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        Settings.System.putInt(
            contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            clamped
        )
    }

    // Частота обновления (Redmi Note 13: 60Hz / 120Hz)
    fun setScreenRefreshRate(rateHz: Int) {
        val rateFloat = if (rateHz == 120) 120f else 60f
        Settings.Global.putFloat(contentResolver, "peak_refresh_rate", rateFloat)
        Settings.Global.putFloat(contentResolver, "min_refresh_rate", rateFloat)
    }

    // Таймаут отключения экрана в миллисекундах
    fun setScreenTimeout(seconds: Int) {
        Settings.System.putInt(
            contentResolver,
            Settings.System.SCREEN_OFF_TIMEOUT,
            seconds * 1000
        )
    }

    // Акселерометр / Автоповорот экрана
    fun setAccelerometerRotation(enabled: Boolean) {
        Settings.System.putInt(
            contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            if (enabled) 1 else 0
        )
    }

    // Управление дискретными громкостями каналов звука
    fun setVolume(streamType: Int, level: Int) {
        audioManager.setStreamVolume(streamType, level, AudioManager.FLAG_SHOW_UI)
    }

    // Индустриальный масштаб шрифта
    fun setFontScale(scale: Float) {
        Settings.System.putFloat(contentResolver, Settings.System.FONT_SCALE, scale)
    }
}
```

---

## 6. Мониторинг аппаратных датчиков в реальном времени

Для Redmi Note 13 4G (Snapdragon 685 SM6225) критически важно отслеживать вольтаж, энергопотребление и тепловые профили без запуска тяжелых фоновых демонов. Опрос производится прямым чтением виртуальной файловой системы `sysfs` и `/proc`.

### 6.1 Спецификация путей датчиков ядра Linux (Snapdragon 685 / msm-5.15)
* **Напряжение батареи (mV)**: `/sys/class/power_supply/battery/voltage_now` (значение в $\mu V$, делить на 1000).
* **Ток потребления (mA)**: `/sys/class/power_supply/battery/current_now` (отрицательный при разряде).
* **Температура батареи**: `/sys/class/power_supply/battery/temp` (значение в десятых долях $^\circ C$, делить на 10).
* **Температура SoC Snapdragon 685**: `/sys/class/thermal/thermal_zone0/temp` (или зона `tsens_tz_sensor0`).
* **Тактовые частоты ядер CPU**:
  * Кластер Silver (ядра 0–3, Cortex-A53, энергоэффективные, 1.9 ГГц):  
    `/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq` ... `cpu3`
  * Кластер Gold (ядра 4–7, Cortex-A73, производительные, 2.8 ГГц):  
    `/sys/devices/system/cpu/cpu4/cpufreq/scaling_cur_freq` ... `cpu7`
* **Общая загрузка CPU**: чтение дельты квантов времени в `/proc/stat`.

### 6.2 Реализация DataSource телеметрии

```kotlin
package com.uvos.settings.data.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

data class CpuCoreMetrics(
    val coreId: Int,
    val frequencyMhz: Int,
    val isGoldCluster: Boolean
)

data class HardwareTelemetrySnapshot(
    val batteryVoltageMv: Int,
    val batteryCurrentMa: Int,
    val batteryTempC: Float,
    val socTempC: Float,
    val totalCpuUsagePercent: Int,
    val cores: List<CpuCoreMetrics>
)

class SysfsTelemetryDataSource {

    private var lastTotalTime = 0L
    private var lastIdleTime = 0L

    fun getTelemetryStream(pollIntervalMs: Long = 1000L): Flow<HardwareTelemetrySnapshot> = flow {
        while (true) {
            val snapshot = readHardwareSnapshot()
            emit(snapshot)
            delay(pollIntervalMs)
        }
    }.flowOn(Dispatchers.IO)

    private fun readHardwareSnapshot(): HardwareTelemetrySnapshot {
        // Чтение параметров батареи
        val rawVoltage = readSysfsInt("/sys/class/power_supply/battery/voltage_now", 4000000)
        val rawCurrent = readSysfsInt("/sys/class/power_supply/battery/current_now", 0)
        val rawBatTemp = readSysfsInt("/sys/class/power_supply/battery/temp", 250)
        val rawSocTemp = readSysfsInt("/sys/class/thermal/thermal_zone0/temp", 35000)

        // Чтение частот 8 ядер Snapdragon 685
        val cores = (0..7).map { coreId ->
            val freqPath = "/sys/devices/system/cpu/cpu$coreId/cpufreq/scaling_cur_freq"
            val freqKhz = readSysfsInt(freqPath, 0)
            CpuCoreMetrics(
                coreId = coreId,
                frequencyMhz = freqKhz / 1000,
                isGoldCluster = coreId >= 4
            )
        }

        val cpuLoad = calculateCpuLoad()

        return HardwareTelemetrySnapshot(
            batteryVoltageMv = rawVoltage / 1000,
            batteryCurrentMa = rawCurrent / 1000,
            batteryTempC = rawBatTemp / 10f,
            socTempC = if (rawSocTemp > 1000) rawSocTemp / 1000f else rawSocTemp.toFloat(),
            totalCpuUsagePercent = cpuLoad,
            cores = cores
        )
    }

    private fun readSysfsInt(path: String, default: Int): Int {
        return try {
            val file = File(path)
            if (file.exists()) file.readText().trim().toIntOrNull() ?: default else default
        } catch (e: Exception) {
            default
        }
    }

    private fun calculateCpuLoad(): Int {
        return try {
            val statFile = File("/proc/stat")
            if (!statFile.exists()) return 0
            val line = statFile.bufferedReader().readLine() ?: return 0
            val parts = line.split("\\s+".toRegex()).filter { it.isNotEmpty() }
            if (parts.size < 5) return 0

            val user = parts[1].toLong()
            val nice = parts[2].toLong()
            val system = parts[3].toLong()
            val idle = parts[4].toLong()
            val iowait = if (parts.size > 5) parts[5].toLong() else 0L
            val irq = if (parts.size > 6) parts[6].toLong() else 0L
            val softirq = if (parts.size > 7) parts[7].toLong() else 0L

            val total = user + nice + system + idle + iowait + irq + softirq
            val totalDiff = total - lastTotalTime
            val idleDiff = idle - lastIdleTime

            lastTotalTime = total
            lastIdleTime = idle

            if (totalDiff <= 0) return 0
            val load = ((totalDiff - idleDiff) * 100 / totalDiff).toInt()
            load.coerceIn(0, 100)
        } catch (e: Exception) {
            0
        }
    }
}
```

---

## 7. Модуль аудита приложений (App Audit & Isolation Manager)

Модуль аудита выводит список установленного ПО с бейджами аппаратной изоляции, выявляя сторонние приложения, пытающиеся выйти в сеть или получить доступ к сенсорам.

### 7.1 Бейджи статуса изоляции
* `«СИСТЕМА»` (Серая плашка `#4C5061`): Встроенный доверенный компонент прошивки (`ApplicationInfo.FLAG_SYSTEM`).
* `«ИЗОЛИРОВАНО»` (Зелёная плашка `#063013`, текст `#00FF66`): Приложение полностью отрезано от сети (отсутствует `INTERNET` или заблокировано через iptables), каналы идентификаторов подменяются нулями.
* `«1 КАНАЛ СЕТИ»` (Янтарная плашка `#382700`, текст `#FFB000`): Приложению разрешён только ограниченный доступ к Интернету без права чтения контактов, хранилища и геолокации.
* `«ОПАСНОСТЬ»` (Красная плашка `#3A0D0D`, текст `#E02020`): Приложение имеет доступ к сенсорам и сети одновременно.

```kotlin
package com.uvos.settings.domain.model

data class AuditedAppInfo(
    val packageName: String,
    val appName: String,
    val uid: Int,
    val isSystemApp: Boolean,
    val hasInternetPermission: Boolean,
    val isIptablesBlocked: Boolean,
    val isolationBadge: AppIsolationBadge,
    val activeChannelsCount: Int
)

enum class AppIsolationBadge(val label: String, val colorHex: Long) {
    SYSTEM("СИСТЕМА", 0xFF4C5061),
    ISOLATED("ИЗОЛИРОВАНО", 0xFF00FF66),
    SINGLE_CHANNEL_NET("1 КАНАЛ СЕТИ", 0xFFFFB000),
    EXPOSED("ОТКРЫТО", 0xFFE02020)
}
```

### 7.2 Карточка приложения в Compose (`AppAuditItemCard`)

```kotlin
package com.uvos.settings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.settings.domain.model.AppIsolationBadge
import com.uvos.settings.domain.model.AuditedAppInfo

@Composable
fun AppAuditItemCard(
    app: AuditedAppInfo,
    onToggleIsolation: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1E26), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF242838), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                color = Color(0xFFE2E4EC),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${app.packageName} (UID: ${app.uid})",
                color = Color(0xFF71768E),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Бейдж статуса изоляции
        Box(
            modifier = Modifier
                .background(
                    Color(when (app.isolationBadge) {
                        AppIsolationBadge.SYSTEM -> 0xFF2A2D3A
                        AppIsolationBadge.ISOLATED -> 0xFF063013
                        AppIsolationBadge.SINGLE_CHANNEL_NET -> 0xFF382700
                        AppIsolationBadge.EXPOSED -> 0xFF3A0D0D
                    }),
                    RoundedCornerShape(6.dp)
                )
                .border(
                    1.dp,
                    Color(app.isolationBadge.colorHex),
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = app.isolationBadge.label,
                color = Color(app.isolationBadge.colorHex),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
```

---

## 8. Экран приборной панели: Главная консоль настроек

Главный контейнер собирает метрики телеметрии, блок переключения профиля, регуляторы яркости/звука и секцию аудита.

```kotlin
package com.uvos.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.settings.presentation.components.*
import com.uvos.settings.presentation.contract.SettingsIntent

@Composable
fun SettingsDashboardScreen(
    viewModel: SettingsViewModel
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E12))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "UVOS КОНСОЛЬ УПРАВЛЕНИЯ",
                color = Color(0xFFE2E4EC),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Text(
                text = "Штабной пульт настройки платформы UnderVoltOS",
                color = Color(0xFF71768E),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Блок 1: Глобальный селектор профилей
        item {
            M4200ToggleSwitch(
                currentProfile = state.currentProfile,
                onProfileSelected = { profile ->
                    viewModel.handleIntent(SettingsIntent.SelectProfile(profile))
                }
            )
        }

        // Блок 2: Аппаратная телеметрия М4200
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Вольтметр батареи
                M4200DialGauge(
                    value = state.batteryVoltageMv.toFloat(),
                    minValue = 3400f,
                    maxValue = 4450f,
                    unitTitle = "mV",
                    deviceLabel = "ВОЛЬТЫ АКБ",
                    modifier = Modifier.weight(1f),
                    dangerThreshold = 4350f
                )

                // Температура чипсета Snapdragon 685
                M4200DialGauge(
                    value = state.socTemperatureC,
                    minValue = 20f,
                    maxValue = 85f,
                    unitTitle = "°C",
                    deviceLabel = "T(SOC) SM6225",
                    modifier = Modifier.weight(1f),
                    dangerThreshold = 65f
                )
            }
        }

        // Блок 3: Индикаторы частот 8 ядер CPU Snapdragon 685
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1C1E26), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "ТАКТОВЫЕ ЧАСТОТЫ ЯДЕР (SM6225)",
                    color = Color(0xFF71768E),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    state.coreFrequenciesMhz.forEachIndexed { index, mhz ->
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Text(
                                text = "C$index",
                                color = if (index >= 4) Color(0xFFFFB000) else Color(0xFF8E93A6),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "$mhz",
                                color = Color(0xFFE2E4EC),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Блок 4: Модуль аудита приложений
        item {
            Text(
                text = "РЕВИЗИЯ И АУДИТ ПРИЛОЖЕНИЙ",
                color = Color(0xFFE2E4EC),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        items(state.installedApps) { app ->
            AppAuditItemCard(
                app = app,
                onToggleIsolation = { pkg, isolate ->
                    viewModel.handleIntent(SettingsIntent.ToggleAppIsolation(pkg, isolate))
                }
            )
        }
    }
}
```

---

## 9. Манифест и системные привилегии

Для исполнения роли системного пульта приложение `com.uvos.settings` оформляется как привилегированное (`privileged_app`) с правами подписи системы:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.uvos.settings"
    android:sharedUserId="android.uid.system">

    <!-- Привилегии изменения системных настроек -->
    <uses-permission android:name="android.permission.WRITE_SETTINGS" />
    <uses-permission android:name="android.permission.WRITE_SECURE_SETTINGS" />
    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
    <uses-permission android:name="android.permission.CONTROL_DISPLAY_BRIGHTNESS" />
    
    <!-- Привилегии управления аппаратными модулями -->
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.NFC" />
    <uses-permission android:name="android.permission.MANAGE_SENSOR_PRIVACY" />
    
    <!-- Привилегии аудита пакетов -->
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
    <uses-permission android:name="android.permission.MANAGE_APP_OPS_MODES" />
    <uses-permission android:name="android.permission.FORCE_STOP_PACKAGES" />

    <!-- ВАЖНО: Разрешение INTERNET ОТСУТСТВУЕТ (Zero Telemetry Invariant) -->

    <application
        android:label="Настройки UVOS"
        android:icon="@drawable/ic_m4200_dial"
        android:theme="@style/Theme.UVOS.Settings"
        android:directBootAware="true">

        <activity
            android:name=".presentation.SettingsMainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.settings.SETTINGS" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

---

## 10. План поэтапной разработки

1. **Фаза 1: Сборка дизайн-библиотеки М4200**:
   - Реализация `M4200DialGauge` с баллистикой пружины стрелки.
   - Реализация `M4200ToggleSwitch` и фоновых плиток шасси `#1C1E26`.
2. **Фаза 2: Телеметрический драйвер (Sysfs)**:
   - Проверка узлов виртуальной ФС на ядре `msm-5.15` Redmi Note 13.
   - Тестирование расчёта нагрузки на 8 ядрах Snapdragon 685 через `StateFlow`.
3. **Фаза 3: Интеграция SettingsProvider**:
   - Реализация переключателей яркости, таймаута экрана, частоты обновления 120 Гц.
4. **Фаза 4: Аудит и изоляция**:
   - Построение сканера установленных пакетов.
   - Формирование бейджей («СИСТЕМА», «ИЗОЛИРОВАНО», «1 КАНАЛ СЕТИ») и сопряжение с блокировками iptables.
5. **Фаза 5: Валидация профилей безопасности**:
   - Проверка мгновенного глушения радиомодулей и сенсоров при активации профиля «СТЕРИЛЬНО».
