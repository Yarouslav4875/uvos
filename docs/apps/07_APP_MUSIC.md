# Спецификация и руководство по разработке: UVOS Music (`com.uvos.music`)

> **Пакет**: `com.uvos.music`  
> **Статус**: Архитектурный стандарт UnderVoltOS (UVOS Suite)  
> **Классификация**: Встроенный Hi-Res аудиоплеер студийного класса  
> **Инварианты**: 100% Offline, 0% телеметрии, 0% рекламы, Bit-Perfect воспроизведение, ретро-приборный дизайн M4200  
> **Целевая платформа**: UnderVoltOS (Android 14 AOSP / HyperOS Base — Xiaomi Redmi Note 13 4G `sapphire/sapphiren`)

---

## 1. Концепция и философия продукта

UVOS Music — это бескомпромиссный музыкальный проигрыватель для локальной медиатеки, созданный для ценителей аутентичного аналогового звука, студийной аппаратуры 1970–80-х годов (Revox, Studer, Nagra) и абсолютной цифровой приватности.

```
+-----------------------------------------------------------------------------+
|                               UVOS MUSIC UI                                 |
|  +-----------------------------------------------------------------------+  |
|  |   [M4200 LEFT VU]                     [M4200 RIGHT VU]                |  |
|  |     -20  -10 -7 -3 0 +3                 -20  -10 -7 -3 0 +3           |  |
|  |    (   \           ) [CLIP]            (         /     ) [CLIP]       |  |
|  +-----------------------------------------------------------------------+  |
|  |  Track: "Pink Floyd - Time (Live 1974)"                               |  |
|  |  Format: FLAC 24-bit / 192 kHz (Lossless) | Bitrate: 5840 kbps        |  |
|  +-----------------------------------------------------------------------+  |
|  |  [<<]  [PLAY/PAUSE]  [>>]     [REPEAT: OFF]    [SHUFFLE: OFF]         |  |
|  +-----------------------------------------------------------------------+  |
|  |  STUDIO EQUALIZER & MASTERING RACK                                     |  |
|  |   [31] [62] [125] [250] [500] [1k] [2k] [4k] [8k] [16k]   (BASS) (GAIN)|  |
|  |    ||   ||   ||    ||    ||   ||   ||   ||   ||   ||      ( O )  ( O ) |  |
|  |   [EQ: ON/OFF TOGGLE]     [VU PAD: 0dB/+6dB]     [WARM TUBE: ON]       |  |
|  +-----------------------------------------------------------------------+  |
+-----------------------------------------------------------------------------+
```

### Ключевые принципы
1. **Zero-Network Invariant**: Полное отсутствие сетевого разрешения `android.permission.INTERNET` в манифесте. Приложение физически не способно передавать статистику прослушиваний, запрашивать внешние метаданные или подгружать рекламу.
2. **True Hi-Res Audio Engine**: Прямой вывод аудиопотока через `AndroidX Media3 (ExoPlayer)` с поддержкой 24-bit / 192 kHz, 32-bit float PCM без программного ресемплинга в 48 kHz (Direct AudioTrack / Offload).
3. **Баллистические стрелочные VU-метры**: Реалистичная эмуляция электромеханических приборов М4200 с точным соблюдением баллистики ANSI C16.5-1954 / IEC 60268-17 (время интеграции 300 мс, затухание, обратный отскок).
4. **Студийный пульт управления**: 10-полосный физический эквалайзер с винтажными фейдерами, прецизионными аналоговыми потенциометрами (Gain, Bass Boost, Reverb) и тумблерами-переключателями с тактильным откликом (Haptic Feedback).

---

## 2. Архитектура приложения (Clean Architecture)

Приложение построено по принципам Clean Architecture в связке с шаблоном MVI (Model-View-Intent) и однонаправленным потоком данных (UDF).

```mermaid
flowchart TD
    subgraph UI_Layer ["Presentation Layer (Jetpack Compose & Custom Canvas)"]
        A1["PlayerScreen / Dashboard"] -->|User Intents| A2["PlayerViewModel"]
        A3["EqualizerScreen"] -->|Knob/Fader Updates| A4["EqualizerViewModel"]
        A5["LibraryScreen"] -->|Track Selection| A6["LibraryViewModel"]
        A7["GaugeView (M4200 L/R)"] <---|Audio Levels (60 FPS)| A2
    end

    subgraph Domain_Layer ["Domain Layer (Pure Kotlin UseCases)"]
        A2 --> B1["ControlPlaybackUseCase"]
        A2 --> B2["GetAudioStreamMetricsUseCase"]
        A4 --> B3["ApplyEqualizerPresetUseCase"]
        A6 --> B4["ScanLocalMediaUseCase"]
        B1 --> B5["AudioPlaybackRepository"]
        B2 --> B5
        B3 --> B6["AudioEffectsRepository"]
        B4 --> B7["MediaLibraryRepository"]
    end

    subgraph Data_Layer ["Data Layer & Audio Engine"]
        B5 --> C1["Media3AudioPlayerImpl\n(ExoPlayer + Custom AudioSink)"]
        B6 --> C2["EqualizerDataSource\n(Android OpenSL / DynamicsProcessing)"]
        B7 --> C3["LocalAudioScanner\n(MediaStore + TagLib Native)"]
        C1 --> C4["MusicPlaybackService\n(MediaSessionService)"]
    end

    subgraph Hardware_Bridge ["OS & Hardware"]
        C4 --> D1["Android AudioTrack (Direct PCM 24/192)"]
        C1 --> D2["Visualizer / TeeAudioProcessor (Dual L/R PCM)"]
        C4 --> D3["MediaSessionCompat (Lockscreen / Bluetooth AVRCP)"]
    end
```

### 2.1. Слои системы
1. **Presentation Layer**:
   * Компонентный UI на Jetpack Compose в стиле UnderVoltOS (`#0E0E12`, `#1C1E26`, `#F2E8A6`, `#E02020`).
   * Высокопроизводительные Custom Canvas компоненты для аналоговых шкал: `StereoVuMeterView`, `RotaryKnobView`, `StudioFaderView`, `MetalToggleSwitchView`.
   * Реактивная подписка на `StateFlow` с частотой кадровой развертки 60/120 Гц для плавной анимации стрелок.
2. **Domain Layer**:
   * Чистые бизнес-правила без зависимостей от Android SDK (`android.*`).
   * Модели: `AudioTrack`, `Album`, `Artist`, `EqualizerBand`, `VuMeterState`, `PlaybackQueue`.
   * Сценарии использования: переключение очередей, парсинг Hi-Res метаданных, расчет баллистики стрелок.
3. **Data Layer**:
   * Реализация репозиториев поверх `AndroidX Media3 (ExoPlayer 1.3+)`.
   * Модуль чтения локальных тегов через NDK (native `TagLib`) для мгновенного парсинга обложек и метаданных без блокировки UI-потока.
   * `Room Database` (локальная база плейлистов, статистики треков, истории прослушивания) — зашифрована SQLCipher.
4. **Service Layer**:
   * Фоновый сервис `MusicPlaybackService`, расширяющий `androidx.media3.session.MediaSessionService`.
   * Управление жизненным циклом сессии, системные нотификации с элементами управления, Bluetooth AVRCP 1.6, аудиофокус и обработка гарнитуры.

---

## 3. Спецификация аудио-движка (Audio Pipeline)

### 3.1. Форматы и поддержка Hi-Res
* **Lossless**: FLAC (до 24-bit / 192 kHz), ALAC (Apple Lossless .m4a), WAV (PCM 16/24/32-bit float), AIFF, APE (Monkey's Audio), DSD over PCM (DoP / DFF / DSF).
* **Lossy**: MP3 (VBR/CBR до 320 kbps), OGG Vorbis, Opus (до 512 kbps), AAC (m4a/aac).
* **Gapless Playback**: Бесшовное воспроизведение live-альбомов и концептуальных записей без щелчков и пауз между дорожками (через ExoPlayer `DefaultRenderersFactory` и предварительную буферизацию следующего трека).
* **ReplayGain / EBU R128**: Автоматическая компенсация громкости по тегам альбома без компрессии динамического диапазона.

### 3.2. Конфигурация AndroidX Media3
```kotlin
// Настройка бесшовного offline-рендеринга без транскодирования
@UnstableApi
fun buildExoPlayer(context: Context): ExoPlayer {
    val renderersFactory = DefaultRenderersFactory(context).apply {
        setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        setEnableAudioFloatOutput(true) // Включение 32-bit float PCM для повышенного динамического диапазона
        setEnableAudioOffload(true)     // Прямой вывод на аппаратный DSP Snapdragon при выключенном экране
    }

    val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    return ExoPlayer.Builder(context, renderersFactory)
        .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
        .setHandleAudioBecomingNoisy(true) // Автопауза при извлечении наушников / отключении BT
        .setWakeMode(C.WAKE_MODE_LOCAL)
        .build()
}
```

---

## 4. Двойные стрелочные VU-метры (M4200 Stereo Engine)

### 4.1. Физическая модель баллистики (Ballistics & Damping)
Традиционный индикатор громкости (VU-метр) — это не безынерционный пик-метр (PPM), а магнитоэлектрический вольтметр с нормированной инерцией стрелки.

Динамика движения стрелки описывается дифференциальным уравнением затухающих колебаний второго порядка:
$$J \frac{d^2\theta}{dt^2} + B \frac{d\theta}{dt} + K(\theta - \theta_0) = \tau(t)$$

Где:
* $J$ — момент инерции подвижной рамки со стрелкой;
* $B$ — коэффициент электромагнитного и воздушного демпфирования;
* $K$ — жесткость возвратной спиральной пружины;
* $\tau(t)$ — вращающий момент, пропорциональный выпрямленному току аудиосигнала.

**Норматив ANSI C16.5-1954**:
1. При подаче синусоидального сигнала 1 кГц стрелка должна достичь отметки 0 VU (99% шкалы) за **$300 \pm 30$ мс**.
2. Перелет стрелки (overshoot) за отметку 0 VU должен составлять не менее 1.0% и не более 1.5%.
3. Время возврата стрелки в исходное положение при снятии сигнала — **$300$ мс**.

### 4.2. Алгоритм дискретной симуляции баллистики (Kotlin)
```kotlin
package com.uvos.music.core.audio.ballistics

/**
 * Физический эмулятор баллистики стрелочного прибора М4200.
 * Вызывается с интервалом кадра (dt ~ 16.6 ms для 60 FPS).
 */
class VuMeterBallistics(
    private val attackTimeSec: Float = 0.300f,
    private val decayTimeSec: Float = 0.300f,
    private val overshootFactor: Float = 1.012f
) {
    private var currentPosition: Float = 0f
    private var velocity: Float = 0f

    // Параметры затухающего осциллятора
    private val omega: Float = 18.0f // Собственная угловая частота системы
    private val zeta: Float = 0.72f  // Коэффициент затухания (критический демпфер с легким отскоком)

    fun update(targetLevelNormalized: Float, dt: Float): Float {
        val target = targetLevelNormalized.coerceIn(0f, 1.15f)
        
        // Сила упругости пружины и электромагнитный тормоз
        val springForce = - (omega * omega) * (currentPosition - target)
        val dampingForce = - 2f * zeta * omega * velocity
        val acceleration = springForce + dampingForce

        // Численное интегрирование по методу Верле/Эйлера
        velocity += acceleration * dt
        currentPosition += velocity * dt

        if (currentPosition < 0f) {
            currentPosition = 0f
            velocity = -velocity * 0.15f // Упругий отскок от нулевого механического упора
        }

        return currentPosition
    }

    fun reset() {
        currentPosition = 0f
        velocity = 0f
    }
}
```

### 4.3. Захват раздельного стереосигнала (L/R Channels)
Стандартный `android.media.audiofx.Visualizer` на многих чипсетах смешивает каналы в моно. Для бескомпромиссного стерео-мониторинга в UVOS Music применяется комбинированный подход:

```
[ExoPlayer Decoder]
        |
        v (Raw PCM Float Stereo)
[TeeAudioProcessor / AudioProcessor] ---> [ExoPlayer AudioSink] ---> [AudioTrack / DAC]
        |
        v (Каждые 20-30 мс: L/R Buffers)
[StereoAudioSampler]
   |--> L Channel RMS & Peak -> dB -> Logarithmic Scaler -> Ballistics (L) -> Left Gauge
   |--> R Channel RMS & Peak -> dB -> Logarithmic Scaler -> Ballistics (R) -> Right Gauge
```

1. **Custom Media3 `AudioProcessor` (`StereoVuAudioProcessor`)**:
   Встраивается непосредственно в аудио-конвейер плеера перед `AudioTrack`. Перехватывает несжатые сэмплы PCM (16/24/32-bit) по каналам с нулевым оверхедом памяти (Ring Buffer).
2. **Формула расчета среднеквадратичного значения (RMS)**:
   $$RMS_L = \sqrt{\frac{1}{N} \sum_{i=0}^{N-1} x_L[i]^2}, \quad dBFS_L = 20 \log_{10}(RMS_L + 10^{-6})$$
3. **Калибровка шкалы VU**:
   * $0\text{ VU} = -14\text{ dBFS}$ (студийный оптимум) или $-18\text{ dBFS}$ (режим High Dynamic Range).
   * Диапазон шкалы: от $-20\text{ dB}$ до $+3\text{ dB}$.
   * Индикатор перегрузки (`CLIP LED`): зажигается алым при превышении $-0.5\text{ dBFS}$ (пиковое значение) на время не менее 120 мс.

### 4.4. Рендеринг циферблата M4200 (Custom Canvas View)
* **Корпус**: сатинированный композитный пластик `#1C1E26` со скругленными углами, четырьмя винтами со шлицами по углам и механическим юстировочным винтом стрелки внизу.
* **Шкала**: слоновая кость (`#E8E4D8` / `#F2E8A6`), дуга шкалы радиусом $R$.
* **Разметка**:
  * Чёрная зона: от $-20$ до $0\text{ VU}$ (`#1A1A1A`). Засечки на $-20, -10, -7, -5, -3, -2, -1, 0$.
  * Красная зона: от $0$ до $+3\text{ VU}$ (`#E02020`).
* **Стрелка**: клиновидная металлическая игла цвета винтажной слоновой кости (`#F4EEDA`) или угольно-чёрная с круглым противовесом и полусферическим латунным колпачком оси вращения.
* **Подсветка**: мягкое янтарное свечение ламп накаливания (`RadialGradient` с теплым тоном `#FFAA33` при прозрачности 18%), создающее ощущение ламповой студийной стойки.

---

## 5. 10-полосный эквалайзер и ретро-консоль управления

```
+-----------------------------------------------------------------------------------+
|                           UVOS MASTERING EQUALIZER                                |
|                                                                                   |
|  +12dB --+----+----+----+----+----+----+----+----+----+     [BASS BOOST]  [REVERB]|
|          |    |    |    |    |    |    |    |    |    |          ( 7 )      ( 2 ) |
|   0dB  --[■]--+----+----+----+--[■]--+----+----+----+--     [PRE-GAIN]   [BALANCE]|
|          |    |   [■]   |    |    |    |   [■]   |    |          ( 0 )      ( L=R)|
|  -12dB --+---[■]-------[■]--[■]-------[■]-------[■]--[■]                          |
|         31Hz 62Hz 125 250  500   1k   2k   4k   8k  16kHz   [EQ ON/OFF] [WARM TUBE]|
|                                                                [ // ]      [ // ] |
+-----------------------------------------------------------------------------------+
```

### 5.1. Полосы частот и добротность
Эквалайзер построен на 10 полосах второго порядка (Bi-quad Peaking filters) со стандартизированными центральными частотами ISO:
* **Sub-Bass**: 31 Гц ($Q = 1.4$)
* **Bass**: 62 Гц ($Q = 1.4$)
* **Upper Bass**: 125 Гц ($Q = 1.4$)
* **Low Mid**: 250 Гц ($Q = 1.4$)
* **Midrange**: 500 Гц ($Q = 1.4$)
* **Upper Mid**: 1 кГц ($Q = 1.4$)
* **Presence**: 2 кГц ($Q = 1.4$)
* **Brilliance**: 4 кГц ($Q = 1.4$)
* **Air**: 8 кГц ($Q = 1.4$)
* **Ultra-Air**: 16 кГц ($Q = 1.4$)

Диапазон регулировки: от $-12.0\text{ dB}$ до $+12.0\text{ dB}$ с шагом $0.1\text{ dB}$.

### 5.2. Аналоговые потенциометры (Rotary Knobs)
1. **Bass Boost (Low-Shelf Driver)**: аналоговый подъем низких частот (ниже 80 Гц) с плавной сатурацией четных гармоник (ламповый окрас).
2. **Reverb (Studio Room Model)**: локальная эмуляция студийной акустической комнаты (Plate / Studio Room / Hall).
3. **Pre-Gain**: регулировка входного уровня от $-6\text{ dB}$ до $+6\text{ dB}$ для исключения цифрового клиппинга при сильном подъеме полос EQ.
4. **Stereo Balance**: плавное панорамирование L $\leftrightarrow$ R с компенсацией закона равной мощности (-3 dB в центре).

### 5.3. Механические тумблеры (Toggle Switches)
* **Master EQ Bypass**: аппаратное включение/отключение обработки звука с характерным звуковым кликом реле и вибрацией линейного вибромотора haptic-движка.
* **VU Sensitivity (Pad Switch)**: аттенюатор шкалы VU-метров:
  * `0 dB` (стандартная чувствительность для громких треков современных мастеров);
  * `+6 dB` (повышенная чувствительность для классической музыки и виниловых рипов с широким динамическим диапазоном).
* **Loudness Contour**: тонкомпенсация (кривые Флетчера-Мэнсона) для сохранения плотности баса и верхов на малых уровнях громкости.

---

## 6. Управление воспроизведением и системная интеграция

### 6.1. MediaSessionService и экран блокировки
Служба `MusicPlaybackService` регистрирует системную медиа-сессию:
* Отображение обложки альбома в высоком разрешении на экране блокировки UVOS.
* Системная карточка управления в шторке уведомлений (Play, Pause, Previous, Next, Seekbar, Favorite toggle).
* Передача метаданных на внешние Bluetooth-устройства (магнитолы, часы, ресиверы) по протоколу AVRCP 1.6 (включая имя исполнителя, трек, альбом, позицию и длительность).

### 6.2. Обработка гарнитуры и кнопок
* **Single Click**: Воспроизведение / Пауза.
* **Double Click**: Следующий трек.
* **Triple Click**: Предыдущий трек (или перемотка в начало).
* **Long Press**: Быстрый вызов эквалайзера или добавление трека в избранное.
* **Событие `ACTION_AUDIO_BECOMING_NOISY`**: Мгновенный плавный фейд-аут звука на 150 мс и постановка на паузу при отключении кабеля 3.5 мм или разрыве связи Bluetooth.

---

## 7. Структура проекта и исходного кода

```
com.uvos.music/
├── AndroidManifest.xml                     # Без android.permission.INTERNET!
├── build.gradle.kts                        # Media3, Room, Compose, Hilt
├── core/
│   ├── audio/
│   │   ├── engine/
│   │   │   ├── ExoPlayerHolder.kt          # Инициализация и жизненный цикл ExoPlayer
│   │   │   ├── StereoVuAudioProcessor.kt   # Аудио-процессор перехвата L/R PCM
│   │   │   └── AudioFocusController.kt     # Управление аудиофокусом Android
│   │   ├── ballistics/
│   │   │   ├── VuMeterBallistics.kt        # Физическая модель прибора M4200
│   │   │   └── RmsCalculator.kt            # Быстрый SIMD/NEON расчет RMS и dBFS
│   │   └── effects/
│   │       ├── EqualizerController.kt      # Управление 10 полосами EQ
│   │       ├── BassBoostController.kt      # Сатуратор и подъем НЧ
│   │       └── DynamicsLimiter.kt          # Студийный пиковый лимитер
│   ├── ui/
│   │   ├── theme/
│   │   │   ├── Color.kt                    # Цветовые палитры UVOS Retro
│   │   │   ├── Typography.kt               # JetBrains Mono & Sans Serif
│   │   │   └── Theme.kt
│   │   └── components/
│   │       ├── GaugeView.kt                # Ретро-прибор M4200 (Custom View)
│   │       ├── StereoVuPanel.kt            # Двойной спаренный блок L + R
│   │       ├── RotaryKnob.kt               # Круговой потенциометр с тактильным откликом
│   │       ├── StudioFader.kt              # Ползунок эквалайзера с металлической ручкой
│   │       └── MetalToggleSwitch.kt        # Двухпозиционный ретро-тумблер
│   └── database/
│       ├── MusicDatabase.kt                # Room DB с шифрованием SQLCipher
│       ├── dao/TrackDao.kt
│       └── entity/TrackEntity.kt
├── domain/
│   ├── model/
│   │   ├── AudioTrack.kt
│   │   ├── AudioFormatInfo.kt             # FLAC/ALAC, разрядность bit-depth, kHz
│   │   ├── EqualizerPreset.kt
│   │   └── VuLevels.kt
│   └── usecase/
│       ├── PlayTrackUseCase.kt
│       ├── SetEqualizerBandUseCase.kt
│       ├── ScanLocalLibraryUseCase.kt
│       └── GetStereoVuFlowUseCase.kt
├── service/
│   ├── MusicPlaybackService.kt             # MediaSessionService (Media3)
│   ├── MediaNotificationProvider.kt        # Кастомная шторка управления
│   └── MediaSessionCallbackHandler.kt      # Команды гарнитуры и Lockscreen
└── feature/
    ├── player/
    │   ├── PlayerScreen.kt                 # Главный экран с большими VU-метрами
    │   └── PlayerViewModel.kt
    ├── library/
    │   ├── LibraryScreen.kt                # Списки треков, альбомов, папок
    │   └── LibraryViewModel.kt
    └── equalizer/
        ├── EqualizerScreen.kt              # Рэковая стойка с фейдерами и тумблерами
        └── EqualizerViewModel.kt
```

---

## 8. AndroidManifest.xml: Политика приватности и разрешения

Приложение заявляет только минимально необходимые разрешения для доступа к локальным аудиофайлам и удержания фонового воспроизведения.

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.uvos.music">

    <!-- ВНИМАНИЕ: android.permission.INTERNET СТРОГО ЗАПРЕЩЕН -->

    <!-- Доступ к локальным медиафайлам -->
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
    <!-- Для совместимости с Android 12 и ниже -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
                     android:maxSdkVersion="32" />

    <!-- Фоновое воспроизведение и медиа-сессия -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <!-- Тактильная отдача тумблеров и потенциометров -->
    <uses-permission android:name="android.permission.VIBRATE" />

    <application
        android:name=".UvosMusicApp"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher_music"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_music_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Uvos.Music">

        <service
            android:name=".service.MusicPlaybackService"
            android:exported="true"
            android:foregroundServiceType="mediaPlayback">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
            </intent-filter>
        </service>

        <activity
            android:name=".feature.main.MainActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:theme="@style/Theme.Uvos.Music">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
                <category android:name="android.intent.category.APP_MUSIC" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

---

## 9. План и сценарии тестирования (Test Matrix)

### 9.1. Unit-тесты (Баллистика и расчеты)
* `VuMeterBallisticsTest`: проверка времени нарастания сигнала 1 кГц (ровно 300 мс до 0 VU $\pm 10\%$). Проверка перелета стрелки (overshoot не более $1.5\%$).
* `RmsCalculatorTest`: проверка математической точности перевода линейных PCM сэмплов в логарифмическую шкалу dBFS и калибровки в единицы VU.
* `EqualizerConversionTest`: валидация граничных частот (31 Гц – 16 кГц) и отсутствие фазовых искажений при нейтральном положении (0 dB).

### 9.2. Интеграционные тесты (Audio Engine & MediaSession)
* `HiResAudioFormatPlaybackTest`: воспроизведение набора тестовых аудиофайлов (FLAC 24/192, ALAC 24/96, WAV 32 float, MP3 320, VBR OGG). Контроль отсутствия артефактов и корректности заполнения метаданных.
* `AudioBecomingNoisyTest`: эмуляция отключения проводных наушников (`Intent.ACTION_AUDIO_BECOMING_NOISY`). Проверка: плеер переходит в `STATE_PAUSED` в течение < 50 мс без треска в динамике.
* `AudioFocusLossTransientTest`: эмуляция входящего телефонного звонка (`AUDIOFOCUS_LOSS_TRANSIENT`). Воспроизведение ставится на паузу и автоматически восстанавливается после завершения вызова.

### 9.3. Профилирование производительности и энергопотребления
* **Частота обновления UI (FPS)**: стабильные 60/120 FPS при активной отрисовке двух стрелочных приборов.
* **Нагрузка на CPU**: не более $2.5\%$ суммарной нагрузки на CPU Snapdragon 685 при воспроизведении 24-bit/192kHz FLAC с включенными VU-метрами.
* **Утечки памяти (Memory Leaks)**: 0 аллокаций объектов (`0 allocations`) в методе `onDraw()` шкал M4200. Переиспользование `Path`, `Paint` и матричных преобразований.
* **Энергопотребление в фоне**: при выключенном экране и активном `AudioOffload` разряд батареи не должен превышать $1.2\text{ – }1.5\%$ в час.
