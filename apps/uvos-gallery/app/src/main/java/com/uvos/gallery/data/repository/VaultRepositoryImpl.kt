package com.uvos.gallery.data.repository

import com.uvos.gallery.data.local.vault.VaultDao
import com.uvos.gallery.data.local.vault.toDomain
import com.uvos.gallery.domain.model.MediaItem
import com.uvos.gallery.domain.model.VaultItem
import com.uvos.gallery.domain.repository.VaultRepository
import com.uvos.gallery.domain.usecase.DecryptMediaUseCase
import com.uvos.gallery.domain.usecase.EncryptToVaultUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.InputStream

class VaultRepositoryImpl(
    private val vaultDao: VaultDao,
    private val encryptUseCase: EncryptToVaultUseCase,
    private val decryptUseCase: DecryptMediaUseCase
) : VaultRepository {

    override fun observeVaultItems(): Flow<List<VaultItem>> {
        return vaultDao.getAllItems().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun importToVault(mediaItem: MediaItem): Result<VaultItem> {
        return encryptUseCase.execute(mediaItem)
    }

    override suspend fun restoreFromVault(vaultItem: VaultItem): Result<MediaItem> {
        return decryptUseCase.restoreToPublicStorage(vaultItem)
    }

    override suspend fun deletePermanently(vaultItem: VaultItem): Boolean {
        return try {
            val file = File(vaultItem.encryptedFilePath)
            if (file.exists()) {
                file.delete()
            }
            vaultDao.deleteById(vaultItem.id)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getDecryptedStream(vaultItem: VaultItem): InputStream {
        return decryptUseCase.openDecryptedStream(vaultItem)
    }

    override suspend fun decryptToTemporaryPreview(vaultItem: VaultItem): File {
        return decryptUseCase.decryptToTemporaryPreview(vaultItem)
    }

    override fun isVaultInitialized(): Boolean = true
}
