package com.uvos.downloader.data.local.entity

import com.uvos.downloader.domain.model.DownloadStatus
import com.uvos.downloader.domain.model.DownloadTask
import java.util.UUID

data class DownloadTaskEntity(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val destinationPath: String,
    val fileName: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: String,
    val numThreads: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val onlyWifi: Boolean = false,
    val errorMessage: String? = null
) {
    fun toDomain(): DownloadTask {
        return DownloadTask(
            id = id,
            url = url,
            destinationPath = destinationPath,
            fileName = fileName,
            totalBytes = totalBytes,
            downloadedBytes = downloadedBytes,
            status = DownloadStatus.fromString(status),
            numThreads = numThreads,
            createdAt = createdAt,
            onlyWifi = onlyWifi,
            errorMessage = errorMessage
        )
    }

    companion object {
        fun fromDomain(domain: DownloadTask): DownloadTaskEntity {
            return DownloadTaskEntity(
                id = domain.id,
                url = domain.url,
                destinationPath = domain.destinationPath,
                fileName = domain.fileName,
                totalBytes = domain.totalBytes,
                downloadedBytes = domain.downloadedBytes,
                status = domain.status.name,
                numThreads = domain.numThreads,
                createdAt = domain.createdAt,
                onlyWifi = domain.onlyWifi,
                errorMessage = domain.errorMessage
            )
        }
    }
}
