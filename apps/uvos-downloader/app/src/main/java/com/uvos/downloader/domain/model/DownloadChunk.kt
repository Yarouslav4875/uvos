package com.uvos.downloader.domain.model

data class DownloadChunk(
    val chunkId: Long = 0,
    val taskId: String,
    val chunkIndex: Int,
    val startByte: Long,
    val endByte: Long,
    val currentByte: Long
) {
    val totalBytes: Long
        get() = if (endByte >= startByte) (endByte - startByte + 1L) else 0L

    val downloadedBytes: Long
        get() = (currentByte - startByte).coerceIn(0L, totalBytes)

    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    val isCompleted: Boolean
        get() = currentByte > endByte
}
