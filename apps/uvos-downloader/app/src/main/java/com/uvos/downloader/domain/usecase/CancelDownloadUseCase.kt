package com.uvos.downloader.domain.usecase

import com.uvos.downloader.domain.repository.DownloadRepository
import java.io.File

class CancelDownloadUseCase(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(taskId: String, deleteFile: Boolean = true) {
        val task = repository.getTaskById(taskId)
        repository.deleteTask(taskId)
        if (deleteFile && task != null) {
            try {
                val file = File(task.destinationPath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {
                // Ignore cleanup errors
            }
        }
    }
}
