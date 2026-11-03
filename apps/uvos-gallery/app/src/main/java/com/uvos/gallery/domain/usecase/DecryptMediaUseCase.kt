package com.uvos.gallery.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.uvos.gallery.data.local.crypto.AesGcmVaultEngine
import com.uvos.gallery.data.local.vault.VaultDao
import com.uvos.gallery.domain.model.MediaItem
import com.uvos.gallery.domain.model.MediaType
import com.uvos.gallery.domain.model.ThreatLevel
import com.uvos.gallery.domain.model.VaultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class DecryptMediaUseCase(
    private val context: Context,
    private val cryptoEngine: AesGcmVaultEngine,
    private val vaultDao: VaultDao
) {

    fun openDecryptedStream(vaultItem: VaultItem): InputStream {
        val file = File(vaultItem.encryptedFilePath)
        return cryptoEngine.createDecryptedStream(file)
    }

    /**
     * Decrypts vault item to a temporary preview file in app cache
     */
    suspend fun decryptToTemporaryPreview(vaultItem: VaultItem): File = withContext(Dispatchers.IO) {
        val previewDir = File(context.cacheDir, "vault_preview").apply { mkdirs() }
        val ext = when {
            vaultItem.mimeType.contains("png") -> ".png"
            vaultItem.mimeType.contains("mp4") -> ".mp4"
            else -> ".jpg"
        }
        val tempFile = File(previewDir, "preview_${vaultItem.id}$ext")
        if (!tempFile.exists() || tempFile.length() == 0L) {
            val encFile = File(vaultItem.encryptedFilePath)
            cryptoEngine.decryptToFile(encFile, tempFile)
        }
        return@withContext tempFile
    }

    /**
     * Restores media item back to public MediaStore storage and removes from Vault
     */
    suspend fun restoreToPublicStorage(vaultItem: VaultItem): Result<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val isVideo = vaultItem.mimeType.startsWith("video/")
            val collection = if (isVideo) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, vaultItem.originalName)
                put(MediaStore.MediaColumns.MIME_TYPE, vaultItem.mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val relPath = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "$relPath/UVOS_Restored")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val targetUri = context.contentResolver.insert(collection, values)
                ?: throw IllegalStateException("Failed to create restored file entry in MediaStore")

            val encFile = File(vaultItem.encryptedFilePath)
            cryptoEngine.createDecryptedStream(encFile).use { input ->
                context.contentResolver.openOutputStream(targetUri)?.use { output: OutputStream ->
                    input.copyTo(output)
                } ?: throw IllegalStateException("Could not open output stream for restoration")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(targetUri, values, null, null)
            }

            // Remove encrypted file on disk
            if (encFile.exists()) encFile.delete()

            // Remove database entry
            vaultDao.deleteById(vaultItem.id)

            val restoredMedia = MediaItem(
                id = System.currentTimeMillis(),
                contentUri = targetUri,
                name = vaultItem.originalName,
                mimeType = vaultItem.mimeType,
                size = vaultItem.fileSize,
                dateModified = System.currentTimeMillis() / 1000,
                mediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE,
                resolutionText = "${vaultItem.fileSize / 1024} KB",
                threatLevel = ThreatLevel.STERILE
            )

            Result.success(restoredMedia)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
