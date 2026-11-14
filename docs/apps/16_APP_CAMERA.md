# Спецификация 16: Оптический визор UVOS Camera (`com.uvos.camera`)

## 1. Концепция и назначение
**UVOS Camera** — это изолированный оптический визор операционной системы UnderVoltOS, спроектированный по стандарту **Zero-Telemetry Invariant** и оформленный в эстетике измерительных приборов **M4200 Retro-Industrial Laboratory Scope**.

Приложение предназначено для фотосъемки высокой четкости без утечки идентификационных данных пользователя и устройства.

---

## 2. Безопасность и архитектурные инварианты

1. **Аппаратный EXIF-скруббер (`ExifStripper.kt`):**
   - При сохранении каждого снимка производится мгновенная зачистка всех геолокационных меток (`TAG_GPS_*`), серийных номеров сенсора и оптики (`TAG_BODY_SERIAL_NUMBER`, `TAG_LENS_SERIAL_NUMBER`), марки/модели смартфона (`TAG_MAKE`, `TAG_MODEL`) и точных таймстемпов.
   - Поле `TAG_SOFTWARE` перезаписывается обезличенной сигнатурой `UVOS-SecureCamera/1.0 (Zero-Telemetry)`.

2. **Полное отсутствие аудиозаписи:**
   - В манифесте отсутствует разрешение `android.permission.RECORD_AUDIO`.
   - В кодовой базе отсутствуют ссылки на `MediaRecorder`, `AudioRecord` и сопутствующие кодеки.

3. **Сетевая изоляция:**
   - Разрешение `android.permission.INTERNET` отсутствует. Снимки сохраняются исключительно в локальное хранилище устройства (`Pictures/UVOS_Camera`).

---

## 3. Архитектура модулей

```
apps/uvos-camera/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/uvos/camera/
│   │   │   ├── UvosCameraApplication.kt
│   │   │   ├── data/
│   │   │   │   ├── CameraHardwareManager.kt   # CameraX Lifecycle, ImageCapture, Preview
│   │   │   │   └── ExifStripper.kt            # Очистка метаданных на лету
│   │   │   └── ui/
│   │   │       ├── CameraActivity.kt          # Основной экран видоискателя
│   │   │       ├── CameraViewModel.kt         # MVI/StateFlow состояние камеры
│   │   │       ├── components/
│   │   │       │   ├── OscilloscopeGridOverlay.kt # Приборная сетка и авиагоризонт
│   │   │       │   ├── ShutterControl.kt          # Механическая рифленая кнопка затвора
│   │   │       │   └── StatusLamp.kt              # Неоновые лампы статуса
│   │   │       └── theme/
│   │   │           ├── Color.kt               # Палитра M4200
│   │   │           └── Theme.kt
│   │   └── res/
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
└── settings.gradle.kts
```

---

## 4. Графический макет
Макет интерфейса зафиксирован в:
- [`docs/mockups/m4200_camera_viewfinder.png`](../mockups/m4200_camera_viewfinder.png)
