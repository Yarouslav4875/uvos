package com.uvos.files.data.engine

import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import com.topjohnwu.superuser.io.SuFileOutputStream
import com.uvos.files.domain.model.FileItem
import com.uvos.files.domain.model.FileMetadata
import com.uvos.files.domain.model.FileType
import com.uvos.files.domain.model.PosixPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

class RootStorageEngine : FileSystemEngine {

    private fun isRootShellActive(): Boolean {
        return Shell.isAppGrantedRoot() == true
    }

    override fun listFiles(path: String): Flow<List<FileItem>> = flow {
        if (!isRootShellActive()) {
            emit(emptyList())
            return@flow
        }

        // We use toybox/toolbox ls -la or stat for fast, accurate POSIX info
        val normalizedPath = if (path.endsWith("/")) path else "$path/"
        val escapedPath = escapeShellArg(normalizedPath)
        val result = Shell.cmd("ls -la $escapedPath").exec()

        if (!result.isSuccess) {
            emit(emptyList())
            return@flow
        }

        val items = mutableListOf<FileItem>()
        for (line in result.out) {
            val trimmed = line.trim()
            if (trimmed.startsWith("total ") || trimmed.isEmpty()) continue

            val parsedItem = parseLsLine(trimmed, normalizedPath)
            if (parsedItem != null && parsedItem.name != "." && parsedItem.name != "..") {
                items.add(parsedItem)
            }
        }

        emit(items)
    }.flowOn(Dispatchers.IO)

    private fun parseLsLine(line: String, parentPath: String): FileItem? {
        val parts = line.split("\\s+".toRegex())
        if (parts.size < 7) return null

        val permStr = parts[0]
        val isDir = permStr.startsWith("d")
        val isLink = permStr.startsWith("l")

        val fileType = when {
            isDir -> FileType.DIRECTORY
            isLink -> FileType.SYMLINK
            permStr.startsWith("b") -> FileType.BLOCK_DEVICE
            permStr.startsWith("c") -> FileType.CHARACTER_DEVICE
            permStr.startsWith("s") -> FileType.SOCKET
            permStr.startsWith("-") -> FileType.FILE
            else -> FileType.UNKNOWN
        }

        val user = parts[1]
        val group = parts[2]

        // Format varies between toybox versions:
        // Case 1: perms links owner group size date time name
        // Case 2: perms owner group size date time name
        var sizeIndex = 3
        while (sizeIndex < parts.size && parts[sizeIndex].toLongOrNull() == null) {
            sizeIndex++
        }
        val sizeBytes = if (sizeIndex < parts.size) parts[sizeIndex].toLongOrNull() ?: 0L else 0L

        // Name starts after date/time columns
        val nameStartIndex = if (sizeIndex + 3 < parts.size) sizeIndex + 3 else parts.size - 1
        val rawName = parts.subList(nameStartIndex, parts.size).joinToString(" ")
        val cleanName = if (isLink && rawName.contains(" -> ")) {
            rawName.substringBefore(" -> ")
        } else {
            rawName
        }

        if (cleanName.isEmpty()) return null

        val fullPath = if (parentPath == "/") "/$cleanName" else "$parentPath$cleanName"
        val permissions = PosixPermissions.fromRwx(permStr)

        return FileItem(
            name = cleanName,
            absolutePath = fullPath,
            isDirectory = isDir,
            sizeBytes = sizeBytes,
            lastModifiedEpochMs = System.currentTimeMillis(), // Fallback epoch
            fileType = fileType,
            permissions = permissions,
            owner = user,
            group = group
        )
    }

    override suspend fun readFile(path: String): InputStream = withContext(Dispatchers.IO) {
        val suFile = SuFile(path)
        if (suFile.exists()) {
            try {
                return@withContext SuFileInputStream.open(suFile)
            } catch (e: Exception) {
                // Fallback to cat
            }
        }

        val result = Shell.cmd("cat ${escapeShellArg(path)}").exec()
        val bytes = result.out.joinToString("\n").toByteArray()
        ByteArrayInputStream(bytes)
    }

    override suspend fun writeFile(path: String, append: Boolean): OutputStream = withContext(Dispatchers.IO) {
        remountRw(path)
        val suFile = SuFile(path)
        suFile.parentFile?.mkdirs()
        SuFileOutputStream.open(suFile, append)
    }

    override suspend fun delete(path: String): Boolean = withContext(Dispatchers.IO) {
        remountRw(path)
        val result = Shell.cmd("rm -rf ${escapeShellArg(path)}").exec()
        result.isSuccess
    }

    override suspend fun move(from: String, to: String): Boolean = withContext(Dispatchers.IO) {
        remountRw(to)
        val result = Shell.cmd("mv ${escapeShellArg(from)} ${escapeShellArg(to)}").exec()
        result.isSuccess
    }

    override suspend fun copy(from: String, to: String, onProgress: (Float) -> Unit): Boolean = withContext(Dispatchers.IO) {
        remountRw(to)
        onProgress(0.1f)
        val result = Shell.cmd("cp -rf ${escapeShellArg(from)} ${escapeShellArg(to)}").exec()
        onProgress(1.0f)
        result.isSuccess
    }

    override suspend fun createDirectory(path: String): Boolean = withContext(Dispatchers.IO) {
        remountRw(path)
        val result = Shell.cmd("mkdir -p ${escapeShellArg(path)}").exec()
        result.isSuccess
    }

    override suspend fun getMetadata(path: String): FileMetadata = withContext(Dispatchers.IO) {
        val escaped = escapeShellArg(path)
        val statResult = Shell.cmd("stat -c \"%s|%Y|%a|%U|%G|%F\" $escaped").exec()
        val selinuxResult = Shell.cmd("ls -Z $escaped").exec()
        val selinuxContext = selinuxResult.out.firstOrNull()?.split("\\s+".toRegex())?.firstOrNull { it.contains(":") }

        if (statResult.isSuccess && statResult.out.isNotEmpty()) {
            val parts = statResult.out.first().split("|")
            if (parts.size >= 6) {
                val size = parts[0].toLongOrNull() ?: 0L
                val lastMod = (parts[1].toLongOrNull() ?: 0L) * 1000
                val octal = parts[2]
                val user = parts[3]
                val group = parts[4]
                val typeStr = parts[5].lowercase()
                val isDir = typeStr.contains("directory")

                return@withContext FileMetadata(
                    path = path,
                    sizeBytes = size,
                    permissions = PosixPermissions.fromOctal(octal),
                    owner = user,
                    group = group,
                    lastModified = lastMod,
                    isDirectory = isDir,
                    selinuxContext = selinuxContext
                )
            }
        }

        // Fallback
        FileMetadata(
            path = path,
            sizeBytes = 0L,
            permissions = PosixPermissions.fromOctal("0755"),
            owner = "root",
            group = "root",
            lastModified = System.currentTimeMillis(),
            isDirectory = false,
            selinuxContext = selinuxContext
        )
    }

    override suspend fun setPermissions(path: String, permissions: PosixPermissions): Boolean = withContext(Dispatchers.IO) {
        remountRw(path)
        val cmd = "chmod ${permissions.octalString} ${escapeShellArg(path)}"
        val result = Shell.cmd(cmd).exec()
        result.isSuccess
    }

    override suspend fun readChunk(path: String, offset: Long, length: Int): ByteArray = withContext(Dispatchers.IO) {
        val suFile = SuFile(path)
        if (suFile.exists()) {
            try {
                SuFileInputStream.open(suFile).use { input ->
                    input.skip(offset)
                    val buffer = ByteArray(length)
                    var totalRead = 0
                    while (totalRead < length) {
                        val r = input.read(buffer, totalRead, length - totalRead)
                        if (r <= 0) break
                        totalRead += r
                    }
                    return@withContext if (totalRead == length) buffer else buffer.copyOf(totalRead)
                }
            } catch (e: Exception) {
                // fallback to dd
            }
        }

        val escaped = escapeShellArg(path)
        val ddCmd = "dd if=$escaped bs=1 skip=$offset count=$length 2>/dev/null"
        val outStream = ByteArrayOutputStream()
        Shell.cmd(ddCmd).to(outStream).exec()
        outStream.toByteArray()
    }

    override suspend fun writeChunk(path: String, offset: Long, data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        remountRw(path)
        val suFile = SuFile(path)
        if (suFile.exists()) {
            try {
                val existing = SuFileInputStream.open(suFile).use { it.readBytes() }
                val newSize = maxOf(existing.size.toLong(), offset + data.size).toInt()
                val merged = ByteArray(newSize)
                System.arraycopy(existing, 0, merged, 0, existing.size)
                System.arraycopy(data, 0, merged, offset.toInt(), data.size)

                SuFileOutputStream.open(suFile, false).use { out ->
                    out.write(merged)
                }
                return@withContext true
            } catch (e: Exception) {
                // fallback
            }
        }
        false
    }

    private fun remountRw(path: String) {
        when {
            path.startsWith("/system") -> Shell.cmd("mount -o remount,rw /system", "mount -o remount,rw /").exec()
            path.startsWith("/vendor") -> Shell.cmd("mount -o remount,rw /vendor", "mount -o remount,rw /").exec()
            path.startsWith("/apex") -> Shell.cmd("mount -o remount,rw /apex").exec()
            path == "/" -> Shell.cmd("mount -o remount,rw /").exec()
        }
    }

    private fun escapeShellArg(arg: String): String {
        return "'" + arg.replace("'", "'\\''") + "'"
    }
}
