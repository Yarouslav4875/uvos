package com.uvos.gallery.di

import android.content.Context
import com.uvos.gallery.data.local.crypto.AesGcmVaultEngine
import com.uvos.gallery.data.local.mediastore.MediaStoreScanner
import com.uvos.gallery.data.local.vault.VaultDatabase
import com.uvos.gallery.data.repository.MediaRepositoryImpl
import com.uvos.gallery.data.repository.VaultRepositoryImpl
import com.uvos.gallery.domain.repository.MediaRepository
import com.uvos.gallery.domain.repository.VaultRepository
import com.uvos.gallery.domain.usecase.DecryptMediaUseCase
import com.uvos.gallery.domain.usecase.EncryptToVaultUseCase
import com.uvos.gallery.domain.usecase.ExifScrubberEngine

class AppContainer(val context: Context) {

    val cryptoEngine: AesGcmVaultEngine by lazy {
        AesGcmVaultEngine()
    }

    val vaultDatabase: VaultDatabase by lazy {
        VaultDatabase.getInstance(context)
    }

    val scrubberEngine: ExifScrubberEngine by lazy {
        ExifScrubberEngine(context)
    }

    val mediaStoreScanner: MediaStoreScanner by lazy {
        MediaStoreScanner(context)
    }

    val encryptToVaultUseCase: EncryptToVaultUseCase by lazy {
        EncryptToVaultUseCase(context, cryptoEngine, vaultDatabase.vaultDao())
    }

    val decryptMediaUseCase: DecryptMediaUseCase by lazy {
        DecryptMediaUseCase(context, cryptoEngine, vaultDatabase.vaultDao())
    }

    val mediaRepository: MediaRepository by lazy {
        MediaRepositoryImpl(context, mediaStoreScanner, scrubberEngine)
    }

    val vaultRepository: VaultRepository by lazy {
        VaultRepositoryImpl(vaultDatabase.vaultDao(), encryptToVaultUseCase, decryptMediaUseCase)
    }
}
