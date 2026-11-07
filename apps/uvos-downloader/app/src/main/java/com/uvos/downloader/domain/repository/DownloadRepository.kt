package com.uvos.downloader.domain.repository

import com.uvos.downloader.domain.model.DownloadChunk
import com.uvos.downloader.domain.model.DownloadStatus
import com.uvos.downloader.domain.model.DownloadTask
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getAllTasks(): Flow<List<DownloadTask>>
    fun getActiveTasks(): Flow<List<DownloadTask>>
    fun getCompletedTasks(): Flow<List<DownloadTask>>
    suspend fun getTaskById(taskId: String): DownloadTask?
    fun observeTaskChunks(taskId: String): Flow<List<DownloadChunk>>

    suspend fun insertTask(task: DownloadTask)
    suspend fun updateTaskStatus(taskId: String, status: DownloadStatus, errorMessage: String? = null)
    suspend fun updateTaskProgress(taskId: String, downloadedBytes: Long, status: DownloadStatus)
    suspend fun updateTaskMetadata(taskId: String, totalBytes: Long, fileName: String, destinationPath: String)
    suspend fun deleteTask(taskId: String)

    suspend fun saveChunks(taskId: String, chunks: List<DownloadChunk>)
    suspend fun updateChunkProgress(chunkId: Long, currentByte: Long)
    suspend fun getChunksForTask(taskId: String): List<DownloadChunk>
}
