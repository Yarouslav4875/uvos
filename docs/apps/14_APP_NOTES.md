# UVOS Notes (`com.uvos.notes`) — Техническая спецификация и методы разработки

> **Статус**: Архитектурная спецификация и документация разработки  
> **Пакет**: `com.uvos.notes`  
> **Целевая платформа**: UnderVoltOS / Android 14 (API 34)  
> **Политика приватности**: Zero Telemetry, 0% рекламы, полное отсутствие разрешения `INTERNET`.

---

## 1. Назначение и концепция

`UVOS Notes` — защищённое оффлайн-приложение для ведения заметок и технической документации в среде UnderVoltOS. Полностью заменяет проприетарные облачные решения (Google Keep, MIUI Notes), предотвращая утечку персональных мыслей, паролей и рабочих черновиков.

### Ключевые требования:
- **Открытый файловый формат**: хранение заметок в виде стандартных текстовых файлов `.md` (Markdown) в выбранной пользователем директории на накопителе, что делает заметки доступными для резервного копирования и синхронизации через Syncthing / USB.
- **Шифрованный сейф (Crypto Vault)**: возможность шифрования отдельных критических заметок по алгоритму AES-256-GCM с защитой мастер-паролем или биометрией.
- **Полная изоляция (No-Network Invariant)**: приложение компилируется **без** разрешения `android.permission.INTERNET`, что на уровне ядра Linux делает отправку заметок в сеть физически невозможной.
- **Индустриальный стиль M4200**:
  - Минималистичный текстовый редактор с моноширинной типографикой.
  - Подсветка синтаксиса Markdown (заголовки, чек-листы, таблицы, код).
  - Счётчики символов и слов со стрелочным вольтметром объема текста.

---

## 2. Архитектура приложения

```
com.uvos.notes/
├── data/
│   ├── local/
│   │   ├── NoteFileManager.kt
│   │   ├── CryptoManager.kt
│   │   └── NotesIndexDatabase.kt // Room для быстрого полнотекстового поиска FTS4
│   └── repository/
│       └── NotesRepositoryImpl.kt
├── domain/
│   ├── model/
│   │   ├── Note.kt
│   │   └── NoteFolder.kt
│   └── usecase/
│       ├── SaveNoteUseCase.kt
│       ├── EncryptNoteUseCase.kt
│       └── SearchNotesUseCase.kt
└── ui/
    ├── list/
    │   ├── NotesListScreen.kt
    │   └── NotesViewModel.kt
    ├── editor/
    │   ├── MarkdownEditorScreen.kt
    │   └── MarkwonRenderer.kt
    └── components/
        └── TextVolumeGauge.kt
```

---

## 3. Криптографический модуль (Crypto Vault)

Для защиты чувствительных заметок используется библиотека `Android KeyStore` с симметричным ключом AES-256:

```kotlin
class CryptoManager {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun encryptNote(plainText: String, alias: String): EncryptedPayload {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = getOrCreateKey(alias)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return EncryptedPayload(iv = iv, cipherText = cipherBytes)
    }

    fun decryptNote(payload: EncryptedPayload, alias: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = keyStore.getKey(alias, null) as SecretKey
        val spec = GCMParameterSpec(128, payload.iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val plainBytes = cipher.doFinal(payload.cipherText)
        return String(plainBytes, Charsets.UTF_8)
    }
}
```

---

## 4. UI: Рендеринг Markdown и стрелочный индикатор объема

Редактор заметок включает компонент `Markwon` для плавной отрисовки форматирования на лету и аналоговый индикатор M4200 Gauge:

```kotlin
@Composable
fun TextVolumeGauge(
    wordCount: Int,
    maxTargetWords: Int = 2000,
    modifier: Modifier = Modifier
) {
    val progress = (wordCount.toFloat() / maxTargetWords).coerceIn(0f, 1f)
    val angle = -135f + progress * 270f

    Canvas(modifier = modifier.size(120.dp)) {
        drawCircle(color = Color(0xFFE8E4D8))
        rotate(degrees = angle) {
            drawLine(
                color = Color(0xFFE02020),
                start = center,
                end = Offset(center.x, center.y - size.height * 0.40f),
                strokeWidth = 2.5.dp.toPx()
            )
        }
        drawCircle(color = Color(0xFF1A1A1A), radius = 4.dp.toPx())
    }
}
```

---

## 5. Манифест приложения

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.uvos.notes">

    <!-- ВНИМАНИЕ: Разрешение INTERNET полностью исключено! -->
    <uses-permission android:name="android.permission.USE_BIOMETRIC" />

    <application
        android:label="UVOS Заметки"
        android:theme="@style/Theme.UnderVoltOS.Dark">

        <activity
            android:name=".ui.list.NotesActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```
