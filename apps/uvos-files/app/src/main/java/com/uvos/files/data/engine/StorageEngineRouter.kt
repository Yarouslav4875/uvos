package com.uvos.files.data.engine

import android.content.Context
import com.topjohnwu.superuser.Shell
import com.uvos.files.domain.model.FileItem
import com.uvos.files.domain.model.FileMetadata
import com.uvos.files.domain.model.PosixPermissions
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream

class StorageEngineRouter(
    private val context: Context,
    private val safEngine: SafStorageEngine,
    private val rootEngine: RootStorageEngine
) : FileSystemEngine {

    fun isRootPath(path: String): Boolean {
        return path == "/" ||
                path.startsWith("/system") ||
                (path.startsWith("/data") && !path.startsWith("/data/media") && !path.startsWith("/storage")) ||
                path.startsWith("/vendor") ||
                path.startsWith("/apex") ||
                path.startsWith("/dev") ||
                path.startsWith("/proc") ||
                path.startsWith("/sys") ||
                path.startsWith("/etc") ||
                path.startsWith("/product")
    }

    fun isRootAvailable(): Boolean {
        return Shell.isAppGrantedRoot() == true
    }

    fun resolveEngine(path: String): FileSystemEngine {
        return if (isRootPath(path) && isRootAvailable()) {
            rootEngine
        } else {
            safEngine
        }
    }

    override fun listFiles(path: String): Flow<List<FileItem>> {
        return resolveEngine(path).listFiles(path)
    }

    override suspend fun readFile(path: String): InputStream {
        return resolveEngine(path).readFile(path)
    }

    override suspend fun writeFile(path: String, append: Boolean): OutputStream {
        return resolveEngine(path).writeFile(path, append)
    }

    override suspend fun delete(path: String): Boolean {
        return resolveEngine(path).delete(path)
    }

    override suspend fun move(from: String, to: String): Boolean {
        return if ((isRootPath(from) || isRootPath(to)) && isRootAvailable()) {
            rootEngine.move(from, to)
        } else {
            safEngine.move(from, to)
        }
    }

    override suspend fun copy(from: String, to: String, onProgress: (Float) -> Unit): Boolean {
        return if ((isRootPath(from) || isRootPath(to)) && isRootAvailable()) {
            rootEngine.copy(from, to, onProgress)
        } else {
            safEngine.copy(from, to, onProgress)
        }
    }

    override suspend fun createDirectory(path: String): Boolean {
        return resolveEngine(path).createDirectory(path)
    }

    override suspend fun getMetadata(path: String): FileMetadata {
        return resolveEngine(path).getMetadata(path)
    }

    override suspend fun setPermissions(path: String, permissions: PosixPermissions): Boolean {
        return resolveEngine(path).setPermissions(path, permissions)
    }

    override suspend fun readChunk(path: String, offset: Long, length: Int): ByteArray {
        return resolveEngine(path).readChunk(path, offset, length)
    }

    override suspend fun writeChunk(path: String, offset: Long, data: ByteArray): Boolean {
        return resolveEngine(path).writeChunk(path, offset, data)
    }
}
