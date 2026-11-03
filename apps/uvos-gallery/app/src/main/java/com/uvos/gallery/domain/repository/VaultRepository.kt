package com.uvos.gallery.domain.repository

import com.uvos.gallery.domain.model.MediaItem
import com.uvos.gallery.domain.model.VaultItem
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.InputStream

interface VaultRepository {
    fun observeVaultItems(): Flow<List<VaultItem>>
    suspend fun importToVault(mediaItem: MediaItem): Result<VaultItem>
    suspend fun restoreFromVault(vaultItem: VaultItem): Result<MediaItem>
    suspend fun deletePermanently(vaultItem: VaultItem): Boolean
    fun getDecryptedStream(vaultItem: VaultItem): InputStream
    suspend fun decryptToTemporaryPreview(vaultItem: VaultItem): File
    fun isVaultInitialized(): Boolean
}
