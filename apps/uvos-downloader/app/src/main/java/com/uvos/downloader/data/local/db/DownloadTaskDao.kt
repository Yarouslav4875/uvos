package com.uvos.downloader.data.local.db

import com.uvos.downloader.data.local.entity.DownloadTaskEntity
import kotlinx.coroutines.flow.Flow

interface DownloadTaskDao {
    fun getAllTasks(): Flow<List<DownloadTaskEntity>>
    fun getTasksByStatus(statuses: List<String>): Flow<List<DownloadTaskEntity>>
    fun getTaskById(id: String): DownloadTaskEntity?
    fun insert(task: DownloadTaskEntity)
    fun update(task: DownloadTaskEntity)
    fun updateProgress(id: String, downloadedBytes: Long, status: String)
    fun updateStatus(id: String, status: String, errorMessage: String?)
    fun updateMetadata(id: String, totalBytes: Long, fileName: String, destinationPath: String)
    fun deleteById(id: String)
}
