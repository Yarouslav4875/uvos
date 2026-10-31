package com.uvos.dashboard.repository

import android.content.Context
import com.uvos.dashboard.model.VaultNote
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * VaultRepository - репозиторий безопасного аналогового сейфа UVOS.
 *
 * Криптография:
 * - PBKDF2WithHmacSHA256 (20 000 итераций) для деривации 256-битного ключа из комбинации лимба.
 * - AES-256-GCM (12-байт IV, 128-бит тег аутентификации).
 * - Стойкость к подбору: неверная комбинация отсекается проверкой GCM тега без раскрытия открытого текста.
 */
class VaultRepository private constructor(private val context: Context) {

    private val vaultFile = File(context.filesDir, "uvos_vault.dat")
    private val notes = mutableListOf<VaultNote>()
    private var isVaultUnlocked = false
    private var activeKey: SecretKeySpec? = null

    init {
        // Проверяем, существует ли сейф. Если нет — режим первой настройки.
    }

    fun isInitialized(): Boolean = vaultFile.exists() && vaultFile.length() > 28

    fun isUnlocked(): Boolean = isVaultUnlocked

    fun getNotes(): List<VaultNote> = synchronized(this) {
        if (!isVaultUnlocked) emptyList() else notes.toList()
    }

    /**
     * Первоначальная установка кода сейфа.
     */
    fun setupVault(combination: List<Int>): Boolean {
        synchronized(this) {
            val salt = ByteArray(16)
            SecureRandom().nextBytes(salt)
            val key = deriveKey(combination, salt)
            activeKey = key
            notes.clear()

            // Добавляем приветственную памятку о защите сейфа
            notes.add(
                VaultNote(
                    id = 1L,
                    title = "Добро пожаловать в Сейф UVOS",
                    content = "Все данные защищены шифрованием AES-256-GCM. При выходе или сворачивании экран блокируется, память зачищается.",
                    timestamp = System.currentTimeMillis()
                )
            )

            isVaultUnlocked = true
            return saveVaultToDisk(salt, key)
        }
    }

    /**
     * Попытка открыть сейф по введенной комбинации.
     */
    fun unlock(combination: List<Int>): Boolean {
        synchronized(this) {
            if (!isInitialized()) return false
            try {
                val fileBytes = FileInputStream(vaultFile).use { it.readBytes() }
                if (fileBytes.size < 28) return false // 16 байт salt + 12 байт IV

                val salt = fileBytes.copyOfRange(0, 16)
                val iv = fileBytes.copyOfRange(16, 28)
                val ciphertext = fileBytes.copyOfRange(28, fileBytes.size)

                val key = deriveKey(combination, salt)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))

                val decryptedBytes = cipher.doFinal(ciphertext)
                val jsonString = String(decryptedBytes, Charsets.UTF_8)

                // Парсинг записей
                notes.clear()
                val root = JSONObject(jsonString)
                val arr = root.optJSONArray("notes") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    notes.add(
                        VaultNote(
                            id = obj.getLong("id"),
                            title = obj.getString("title"),
                            content = obj.getString("content"),
                            timestamp = obj.getLong("timestamp")
                        )
                    )
                }

                activeKey = key
                isVaultUnlocked = true
                return true
            } catch (_: Exception) {
                // Неверная комбинация или повреждение данных
                lock()
                return false
            }
        }
    }

    /**
     * Мгновенная блокировка и зачистка памяти.
     */
    fun lock() {
        synchronized(this) {
            isVaultUnlocked = false
            activeKey = null
            notes.clear()
        }
    }

    fun addNote(title: String, content: String): VaultNote? {
        synchronized(this) {
            if (!isVaultUnlocked || activeKey == null) return null
            val newNote = VaultNote(
                id = System.currentTimeMillis(),
                title = title,
                content = content,
                timestamp = System.currentTimeMillis()
            )
            notes.add(0, newNote)
            saveCurrentState()
            return newNote
        }
    }

    fun deleteNote(id: Long): Boolean {
        synchronized(this) {
            if (!isVaultUnlocked || activeKey == null) return false
            val removed = notes.removeAll { it.id == id }
            if (removed) {
                saveCurrentState()
            }
            return removed
        }
    }

    private fun saveCurrentState(): Boolean {
        val key = activeKey ?: return false
        val fileBytes = FileInputStream(vaultFile).use { it.readBytes() }
        val salt = fileBytes.copyOfRange(0, 16)
        return saveVaultToDisk(salt, key)
    }

    private fun saveVaultToDisk(salt: ByteArray, key: SecretKeySpec): Boolean {
        return try {
            val root = JSONObject()
            val arr = JSONArray()
            notes.forEach { note ->
                val o = JSONObject().apply {
                    put("id", note.id)
                    put("title", note.title)
                    put("content", note.content)
                    put("timestamp", note.timestamp)
                }
                arr.put(o)
            }
            root.put("notes", arr)

            val plainBytes = root.toString().toByteArray(Charsets.UTF_8)
            val iv = ByteArray(12)
            SecureRandom().nextBytes(iv)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
            val ciphertext = cipher.doFinal(plainBytes)

            FileOutputStream(vaultFile).use { fos ->
                fos.write(salt)
                fos.write(iv)
                fos.write(ciphertext)
                fos.flush()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun deriveKey(combination: List<Int>, salt: ByteArray): SecretKeySpec {
        val passphrase = combination.joinToString(separator = "-") { String.format("%02d", it) }
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, 20_000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    companion object {
        @Volatile
        private var instance: VaultRepository? = null

        fun getInstance(context: Context): VaultRepository {
            return instance ?: synchronized(this) {
                instance ?: VaultRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
