package com.uvos.files.data.repository

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.topjohnwu.superuser.Shell
import com.uvos.files.domain.model.StoragePartitionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

class StoragePartitionRepository(
    private val context: Context
) {

    fun observePartitions(): Flow<List<StoragePartitionInfo>> = flow {
        while (true) {
            val partitions = mutableListOf<StoragePartitionInfo>()

            // 1. /data partition
            val dataPartition = getPartitionInfo("/data", "DATA (/data)")
            partitions.add(dataPartition)

            // 2. /sdcard partition (User storage)
            val sdcardPath = Environment.getExternalStorageDirectory().absolutePath
            val sdcardPartition = getPartitionInfo(sdcardPath, "STORAGE (/sdcard)")
            partitions.add(sdcardPartition)

            // 3. USB-OTG partition
            val usbPartition = detectUsbOtg()
            partitions.add(usbPartition)

            emit(partitions)
            delay(3000) // update every 3 seconds
        }
    }.flowOn(Dispatchers.IO)

    private fun getPartitionInfo(path: String, label: String): StoragePartitionInfo {
        return try {
            val stat = StatFs(path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = blockSize * totalBlocks
            val freeBytes = blockSize * availableBlocks

            StoragePartitionInfo(
                mountPoint = path,
                label = label,
                totalBytes = totalBytes,
                freeBytes = freeBytes,
                isMounted = true
            )
        } catch (e: Exception) {
            // Try via root shell df if StatFs fails
            queryViaRootDf(path, label) ?: StoragePartitionInfo(
                mountPoint = path,
                label = label,
                totalBytes = 0L,
                freeBytes = 0L,
                isMounted = false
            )
        }
    }

    private fun detectUsbOtg(): StoragePartitionInfo {
        // Look into /storage/ and /mnt/media_rw/
        val storageDir = File("/storage")
        if (storageDir.exists() && storageDir.isDirectory) {
            val dirs = storageDir.listFiles() ?: emptyArray()
            for (dir in dirs) {
                val name = dir.name
                if (name != "emulated" && name != "self" && !name.startsWith(".")) {
                    try {
                        val stat = StatFs(dir.absolutePath)
                        val total = stat.blockSizeLong * stat.blockCountLong
                        val free = stat.blockSizeLong * stat.availableBlocksLong
                        if (total > 0) {
                            return StoragePartitionInfo(
                                mountPoint = dir.absolutePath,
                                label = "USB-OTG",
                                totalBytes = total,
                                freeBytes = free,
                                isMounted = true
                            )
                        }
                    } catch (e: Exception) {
                        // ignore and continue
                    }
                }
            }
        }

        // Check root mounts if root available
        if (Shell.isAppGrantedRoot() == true) {
            val result = Shell.cmd("mount | grep -E '/mnt/media_rw|/storage/[0-9A-Z]{4}-[0-9A-Z]{4}'").exec()
            if (result.isSuccess && result.out.isNotEmpty()) {
                val line = result.out.first()
                val parts = line.split("\\s+".toRegex())
                if (parts.size >= 2) {
                    val mountPath = parts[1]
                    val statInfo = queryViaRootDf(mountPath, "USB-OTG")
                    if (statInfo != null) return statInfo
                }
            }
        }

        // Unmounted / disconnected state
        return StoragePartitionInfo(
            mountPoint = "/storage/usb-otg",
            label = "USB-OTG",
            totalBytes = 0L,
            freeBytes = 0L,
            isMounted = false
        )
    }

    private fun queryViaRootDf(path: String, label: String): StoragePartitionInfo? {
        if (Shell.isAppGrantedRoot() != true) return null
        val result = Shell.cmd("df -k '$path'").exec()
        if (result.isSuccess && result.out.size >= 2) {
            val dataLine = result.out[1]
            val cols = dataLine.split("\\s+".toRegex())
            if (cols.size >= 4) {
                val totalKb = cols[1].toLongOrNull() ?: 0L
                val usedKb = cols[2].toLongOrNull() ?: 0L
                val freeKb = cols[3].toLongOrNull() ?: 0L

                val total = totalKb * 1024
                val free = freeKb * 1024

                return StoragePartitionInfo(
                    mountPoint = path,
                    label = label,
                    totalBytes = total,
                    freeBytes = free,
                    isMounted = total > 0
                )
            }
        }
        return null
    }
}
