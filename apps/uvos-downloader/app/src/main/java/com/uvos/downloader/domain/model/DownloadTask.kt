package com.uvos.downloader.domain.model

import java.util.UUID

data class DownloadTask(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val destinationPath: String,
    val fileName: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: DownloadStatus,
    val numThreads: Int = 4,
    val createdAt: Long = System.currentTimeMillis(),
    val onlyWifi: Boolean = false,
    val errorMessage: String? = null,
    val chunks: List<DownloadChunk> = emptyList()
) {
    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    val isIndeterminate: Boolean
        get() = totalBytes <= 0L

    val isFinished: Boolean
        get() = status == DownloadStatus.COMPLETED || status == DownloadStatus.ERROR
}
