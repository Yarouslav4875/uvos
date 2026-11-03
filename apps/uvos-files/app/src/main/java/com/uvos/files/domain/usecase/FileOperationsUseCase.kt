package com.uvos.files.domain.usecase

import com.uvos.files.data.engine.StorageEngineRouter
import com.uvos.files.domain.model.PosixPermissions

class FileOperationsUseCase(
    private val router: StorageEngineRouter
) {
    suspend fun copy(from: String, to: String, onProgress: (Float) -> Unit = {}): Boolean {
        return router.copy(from, to, onProgress)
    }

    suspend fun move(from: String, to: String): Boolean {
        return router.move(from, to)
    }

    suspend fun delete(path: String): Boolean {
        return router.delete(path)
    }

    suspend fun createDirectory(path: String): Boolean {
        return router.createDirectory(path)
    }

    suspend fun setPermissions(path: String, permissions: PosixPermissions): Boolean {
        return router.setPermissions(path, permissions)
    }
}
