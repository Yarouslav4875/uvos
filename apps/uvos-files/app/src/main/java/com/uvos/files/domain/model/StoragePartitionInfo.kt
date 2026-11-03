package com.uvos.files.domain.model

data class StoragePartitionInfo(
    val mountPoint: String,
    val label: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val isMounted: Boolean
) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0L)
    val usedPercentage: Float get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    val formattedFreeSpace: String
        get() = formatBytes(freeBytes)

    val formattedTotalSpace: String
        get() = formatBytes(totalBytes)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            if (kb < 1024) return "%.0f KB".format(kb)
            val mb = kb / 1024.0
            if (mb < 1024) return "%.1f MB".format(mb)
            val gb = mb / 1024.0
            return "%.1f GB".format(gb)
        }
    }
}
