package com.uvos.files.data.engine

import android.content.Context
import com.uvos.files.domain.model.FileItem
import com.uvos.files.domain.model.FileMetadata
import com.uvos.files.domain.model.FileType
import com.uvos.files.domain.model.PosixPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile

class SafStorageEngine(
    private val context: Context
) : FileSystemEngine {

    override fun listFiles(path: String): Flow<List<FileItem>> = flow {
        val targetDir = File(path)
        if (!targetDir.exists() || !targetDir.isDirectory) {
            emit(emptyList())
            return@flow
        }

        val files = targetDir.listFiles() ?: emptyArray()
        val items = files.map { file ->
            val isDir = file.isDirectory
            val fileType = when {
                isDir -> FileType.DIRECTORY
                file.isFile -> FileType.FILE
                else -> FileType.UNKNOWN
            }

            val perms = PosixPermissions(
                userRead = file.canRead(),
                userWrite = file.canWrite(),
                userExecute = file.canExecute(),
                groupRead = file.canRead(),
                groupWrite = file.canWrite(),
                groupExecute = file.canExecute(),
                otherRead = file.canRead(),
                otherWrite = false,
                otherExecute = file.canExecute(),
                octalString = if (file.canWrite()) "0755" else "0644"
            )

            FileItem(
                name = file.name,
                absolutePath = file.absolutePath,
                isDirectory = isDir,
                sizeBytes = if (isDir) 0L else file.length(),
                lastModifiedEpochMs = file.lastModified(),
                fileType = fileType,
                permissions = perms,
                owner = "media",
                group = "media_rw"
            )
        }
        emit(items)
    }.flowOn(Dispatchers.IO)

    override suspend fun readFile(path: String): InputStream = withContext(Dispatchers.IO) {
        FileInputStream(File(path))
    }

    override suspend fun writeFile(path: String, append: Boolean): OutputStream = withContext(Dispatchers.IO) {
        val file = File(path)
        file.parentFile?.mkdirs()
        FileOutputStream(file, append)
    }

    override suspend fun delete(path: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.isDirectory) file.deleteRecursively() else file.delete()
    }

    override suspend fun move(from: String, to: String): Boolean = withContext(Dispatchers.IO) {
        val src = File(from)
        val dst = File(to)
        dst.parentFile?.mkdirs()
        src.renameTo(dst)
    }

    override suspend fun copy(from: String, to: String, onProgress: (Float) -> Unit): Boolean = withContext(Dispatchers.IO) {
        val src = File(from)
        val dst = File(to)
        if (src.isDirectory) {
            src.copyRecursively(dst, overwrite = true)
        } else {
            dst.parentFile?.mkdirs()
            val totalBytes = src.length()
            var copiedBytes = 0L

            src.inputStream().use { input ->
                dst.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } >= 0) {
                        output.write(buffer, 0, bytes)
                        copiedBytes += bytes
                        if (totalBytes > 0) {
                            onProgress((copiedBytes.toFloat() / totalBytes).coerceIn(0f, 1f))
                        }
                    }
                }
            }
        }
        true
    }

    override suspend fun createDirectory(path: String): Boolean = withContext(Dispatchers.IO) {
        File(path).mkdirs()
    }

    override suspend fun getMetadata(path: String): FileMetadata = withContext(Dispatchers.IO) {
        val file = File(path)
        val perms = PosixPermissions(
            userRead = file.canRead(),
            userWrite = file.canWrite(),
            userExecute = file.canExecute(),
            groupRead = file.canRead(),
            groupWrite = file.canWrite(),
            groupExecute = file.canExecute(),
            otherRead = file.canRead(),
            otherWrite = false,
            otherExecute = file.canExecute(),
            octalString = if (file.canWrite()) "0755" else "0644"
        )
        FileMetadata(
            path = file.absolutePath,
            sizeBytes = file.length(),
            permissions = perms,
            owner = "u0_a0",
            group = "media_rw",
            lastModified = file.lastModified(),
            isDirectory = file.isDirectory
        )
    }

    override suspend fun setPermissions(path: String, permissions: PosixPermissions): Boolean = withContext(Dispatchers.IO) {
        val file = File(path)
        file.setReadable(permissions.userRead, false)
        file.setWritable(permissions.userWrite, false)
        file.setExecutable(permissions.userExecute, false)
    }

    override suspend fun readChunk(path: String, offset: Long, length: Int): ByteArray = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists() || offset >= file.length()) return@withContext ByteArray(0)
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(offset)
            val toRead = length.coerceAtMost((file.length() - offset).toInt())
            val buffer = ByteArray(toRead)
            raf.readFully(buffer)
            buffer
        }
    }

    override suspend fun writeChunk(path: String, offset: Long, data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val file = File(path)
        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(offset)
            raf.write(data)
        }
        true
    }
}
