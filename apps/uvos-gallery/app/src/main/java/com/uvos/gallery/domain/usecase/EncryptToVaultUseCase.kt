package com.uvos.gallery.domain.usecase

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import com.uvos.gallery.data.local.crypto.AesGcmVaultEngine
import com.uvos.gallery.data.local.vault.VaultDao
import com.uvos.gallery.data.local.vault.VaultEntity
import com.uvos.gallery.domain.model.MediaItem
import com.uvos.gallery.domain.model.VaultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID

class EncryptToVaultUseCase(
    private val context: Context,
    private val cryptoEngine: AesGcmVaultEngine,
    private val vaultDao: VaultDao
) {

    suspend fun execute(item: MediaItem): Result<VaultItem> = withContext(Dispatchers.IO) {
        try {
            val vaultDir = File(context.filesDir, "vault").apply {
                mkdirs()
                // Ensure media scanner ignores this directory
                val noMedia = File(this, ".nomedia")
                if (!noMedia.exists()) noMedia.createNewFile()
            }

            val vaultId = UUID.randomUUID().toString()
            val destinationFile = File(vaultDir, "$vaultId.uvenc")

            // Read original media stream
            val inputStream: InputStream = context.contentResolver.openInputStream(item.contentUri)
                ?: throw IllegalStateException("Cannot access source file at ${item.contentUri}")

            val iv = inputStream.use { stream ->
                cryptoEngine.encryptFile(stream, destinationFile)
            }

            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val now = System.currentTimeMillis()

            val entity = VaultEntity(
                id = vaultId,
                originalName = item.name,
                mimeType = item.mimeType,
                fileSize = item.size,
                encryptedFilePath = destinationFile.absolutePath,
                createdTimestamp = if (item.dateModified > 0) item.dateModified * 1000 else now,
                encryptedTimestamp = now,
                originalRelativePath = item.filePath ?: "Pictures/",
                ivBase64 = ivBase64
            )

            // Save record in SQL database
            vaultDao.insertItem(entity)

            // Permanently remove original unencrypted file from public storage
            deleteOriginal(item)

            val vaultItem = VaultItem(
                id = entity.id,
                originalName = entity.originalName,
                mimeType = entity.mimeType,
                fileSize = entity.fileSize,
                encryptedFilePath = entity.encryptedFilePath,
                createdTimestamp = entity.createdTimestamp,
                encryptedTimestamp = entity.encryptedTimestamp,
                originalRelativePath = entity.originalRelativePath,
                ivBase64 = entity.ivBase64
            )

            Result.success(vaultItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun deleteOriginal(item: MediaItem) {
        try {
            // Delete via ContentResolver MediaStore
            context.contentResolver.delete(item.contentUri, null, null)
        } catch (e: Exception) {
            // Try direct filesystem deletion if path exists
            item.filePath?.let { path ->
                val f = File(path)
                if (f.exists()) f.delete()
            }
        }
    }
}
