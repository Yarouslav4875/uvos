package com.uvos.downloader.domain.usecase

import com.uvos.downloader.domain.model.DownloadStatus
import com.uvos.downloader.domain.repository.DownloadRepository

class PauseDownloadUseCase(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(taskId: String) {
        repository.updateTaskStatus(taskId, DownloadStatus.PAUSED)
    }
}
