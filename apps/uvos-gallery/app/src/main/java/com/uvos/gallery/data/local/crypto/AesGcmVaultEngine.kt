package com.uvos.gallery.data.local.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AesGcmVaultEngine {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "UVOS_VAULT_AES256_KEY"
        private const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val GCM_IV_LENGTH_BYTES = 12
        val MAGIC_HEADER = byteArrayOf(0x55, 0x56, 0x43, 0x46) // "UVCF"
    }

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    init {
        ensureMasterKeyExists()
    }

    private fun ensureMasterKeyExists() {
        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(false) // IV generated manually via SecureRandom
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        return (keyStore.getEntry(MASTER_KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /**
     * Streaming encryption of file into air-gapped vault
     */
    suspend fun encryptFile(sourceInput: InputStream, destinationFile: File): ByteArray = withContext(Dispatchers.IO) {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), spec)

        destinationFile.parentFile?.mkdirs()
        FileOutputStream(destinationFile).use { fos ->
            // Write magic header and IV prefix
            fos.write(MAGIC_HEADER)
            fos.write(iv)

            CipherOutputStream(fos, cipher).use { cos ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                while (sourceInput.read(buffer).also { bytesRead = it } != -1) {
                    cos.write(buffer, 0, bytesRead)
                }
                cos.flush()
            }
        }
        return@withContext iv
    }

    /**
     * Streaming decryption for direct media viewing / video playback
     */
    fun createDecryptedStream(encryptedFile: File): InputStream {
        val fis = FileInputStream(encryptedFile)

        // Read and validate magic header
        val header = ByteArray(MAGIC_HEADER.size)
        if (fis.read(header) != MAGIC_HEADER.size || !header.contentEquals(MAGIC_HEADER)) {
            fis.close()
            throw SecurityException("Invalid UVOS encrypted file format (Magic header mismatch)")
        }

        // Read IV
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        if (fis.read(iv) != GCM_IV_LENGTH_BYTES) {
            fis.close()
            throw EOFException("Failed to read GCM IV from encrypted file")
        }

        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

        return CipherInputStream(fis, cipher)
    }

    /**
     * Decrypts encrypted file to a destination file
     */
    suspend fun decryptToFile(encryptedFile: File, destinationFile: File): Unit = withContext(Dispatchers.IO) {
        destinationFile.parentFile?.mkdirs()
        createDecryptedStream(encryptedFile).use { inputStream ->
            FileOutputStream(destinationFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
}
