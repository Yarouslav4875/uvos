package com.uvos.downloader.domain.usecase

import com.uvos.downloader.domain.model.DownloadStatus
import com.uvos.downloader.domain.model.DownloadTask
import com.uvos.downloader.domain.repository.DownloadRepository
import java.io.File
import java.util.UUID

class StartDownloadUseCase(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(
        url: String,
        destinationDir: File,
        customFileName: String? = null,
        numThreads: Int = 4,
        onlyWifi: Boolean = false
    ): DownloadTask {
        val initialFileName = customFileName?.ifBlank { null }
            ?: extractFileNameFromUrl(url)

        val targetFile = File(destinationDir, initialFileName)
        val task = DownloadTask(
            id = UUID.randomUUID().toString(),
            url = url,
            destinationPath = targetFile.absolutePath,
            fileName = initialFileName,
            totalBytes = 0L,
            downloadedBytes = 0L,
            status = DownloadStatus.QUEUED,
            numThreads = numThreads.coerceIn(1, 8),
            onlyWifi = onlyWifi
        )

        repository.insertTask(task)
        return task
    }

    private fun extractFileNameFromUrl(url: String): String {
        return try {
            val cleanUrl = url.substringBefore('?').substringBefore('#')
            val rawName = cleanUrl.substringAfterLast('/', "")
            if (rawName.isNotBlank() && rawName.contains('.')) {
                rawName
            } else {
                "download_${System.currentTimeMillis() / 1000}.bin"
            }
        } catch (_: Exception) {
            "download_${System.currentTimeMillis() / 1000}.bin"
        }
    }
}
