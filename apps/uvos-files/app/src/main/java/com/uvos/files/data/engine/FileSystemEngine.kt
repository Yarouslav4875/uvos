package com.uvos.files.data.engine

import com.uvos.files.domain.model.FileItem
import com.uvos.files.domain.model.FileMetadata
import com.uvos.files.domain.model.PosixPermissions
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream

interface FileSystemEngine {
    fun listFiles(path: String): Flow<List<FileItem>>
    suspend fun readFile(path: String): InputStream
    suspend fun writeFile(path: String, append: Boolean = false): OutputStream
    suspend fun delete(path: String): Boolean
    suspend fun move(from: String, to: String): Boolean
    suspend fun copy(from: String, to: String, onProgress: (Float) -> Unit = {}): Boolean
    suspend fun createDirectory(path: String): Boolean
    suspend fun getMetadata(path: String): FileMetadata
    suspend fun setPermissions(path: String, permissions: PosixPermissions): Boolean
    suspend fun readChunk(path: String, offset: Long, length: Int): ByteArray
    suspend fun writeChunk(path: String, offset: Long, data: ByteArray): Boolean
}
