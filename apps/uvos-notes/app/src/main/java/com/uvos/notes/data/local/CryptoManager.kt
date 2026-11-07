package com.uvos.notes.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class EncryptedPayload(
    val iv: ByteArray,
    val cipherText: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptedPayload
        if (!iv.contentEquals(other.iv)) return false
        if (!cipherText.contentEquals(other.cipherText)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = iv.contentHashCode()
        result = 31 * result + cipherText.contentHashCode()
        return result
    }
}

class CryptoManager {

    companion object {
        const val DEFAULT_VAULT_KEY_ALIAS = "UVOS_NOTES_VAULT_KEY"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val MAGIC_HEADER = 0x55565343 // "UVSC"
        private const val FORMAT_VERSION = 1
    }

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    @Synchronized
    fun getOrCreateKey(alias: String = DEFAULT_VAULT_KEY_ALIAS): SecretKey {
        val existing = keyStore.getKey(alias, null) as? SecretKey
        if (existing != null) {
            return existing
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keyGenSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }

    fun encryptNote(plainText: String, alias: String = DEFAULT_VAULT_KEY_ALIAS): EncryptedPayload {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = getOrCreateKey(alias)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return EncryptedPayload(iv = iv, cipherText = cipherBytes)
    }

    fun decryptNote(payload: EncryptedPayload, alias: String = DEFAULT_VAULT_KEY_ALIAS): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = getOrCreateKey(alias)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, payload.iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val plainBytes = cipher.doFinal(payload.cipherText)
        return String(plainBytes, Charsets.UTF_8)
    }

    fun serializePayload(payload: EncryptedPayload): ByteArray {
        val baos = ByteArrayOutputStream()
        DataOutputStream(baos).use { dos ->
            dos.writeInt(MAGIC_HEADER)
            dos.writeByte(FORMAT_VERSION)
            dos.writeInt(payload.iv.size)
            dos.write(payload.iv)
            dos.writeInt(payload.cipherText.size)
            dos.write(payload.cipherText)
            dos.flush()
        }
        return baos.toByteArray()
    }

    fun deserializePayload(data: ByteArray): EncryptedPayload {
        DataInputStream(ByteArrayInputStream(data)).use { dis ->
            val magic = dis.readInt()
            if (magic != MAGIC_HEADER) {
                throw IllegalArgumentException("Invalid encrypted note header: $magic")
            }
            val version = dis.readByte()
            if (version > FORMAT_VERSION) {
                throw IllegalArgumentException("Unsupported encrypted note version: $version")
            }
            val ivLength = dis.readInt()
            val iv = ByteArray(ivLength)
            dis.readFully(iv)

            val cipherLength = dis.readInt()
            val cipherText = ByteArray(cipherLength)
            dis.readFully(cipherText)

            return EncryptedPayload(iv = iv, cipherText = cipherText)
        }
    }
}
