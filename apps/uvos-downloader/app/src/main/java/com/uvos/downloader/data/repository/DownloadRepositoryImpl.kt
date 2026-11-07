package com.uvos.downloader.data.repository

import com.uvos.downloader.data.local.db.DownloadDatabase
import com.uvos.downloader.data.local.entity.DownloadChunkEntity
import com.uvos.downloader.data.local.entity.DownloadTaskEntity
import com.uvos.downloader.domain.model.DownloadChunk
import com.uvos.downloader.domain.model.DownloadStatus
import com.uvos.downloader.domain.model.DownloadTask
import com.uvos.downloader.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DownloadRepositoryImpl(
    private val database: DownloadDatabase
) : DownloadRepository {

    private val taskDao = database.downloadTaskDao
    private val chunkDao = database.downloadChunkDao

    override fun getAllTasks(): Flow<List<DownloadTask>> {
        return taskDao.getAllTasks().map { list ->
            list.map { entity ->
                val chunks = chunkDao.getChunksForTask(entity.id).map { it.toDomain() }
                entity.toDomain().copy(chunks = chunks)
            }
        }
    }

    override fun getActiveTasks(): Flow<List<DownloadTask>> {
        val activeStatuses = listOf(
            DownloadStatus.QUEUED.name,
            DownloadStatus.DOWNLOADING.name,
            DownloadStatus.PAUSED.name
        )
        return taskDao.getTasksByStatus(activeStatuses).map { list ->
            list.map { entity ->
                val chunks = chunkDao.getChunksForTask(entity.id).map { it.toDomain() }
                entity.toDomain().copy(chunks = chunks)
            }
        }
    }

    override fun getCompletedTasks(): Flow<List<DownloadTask>> {
        val finishedStatuses = listOf(
            DownloadStatus.COMPLETED.name,
            DownloadStatus.ERROR.name
        )
        return taskDao.getTasksByStatus(finishedStatuses).map { list ->
            list.map { entity ->
                val chunks = chunkDao.getChunksForTask(entity.id).map { it.toDomain() }
                entity.toDomain().copy(chunks = chunks)
            }
        }
    }

    override suspend fun getTaskById(taskId: String): DownloadTask? {
        val entity = taskDao.getTaskById(taskId) ?: return null
        val chunks = chunkDao.getChunksForTask(taskId).map { it.toDomain() }
        return entity.toDomain().copy(chunks = chunks)
    }

    override fun observeTaskChunks(taskId: String): Flow<List<DownloadChunk>> {
        return chunkDao.observeChunksForTask(taskId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun insertTask(task: DownloadTask) {
        taskDao.insert(DownloadTaskEntity.fromDomain(task))
        if (task.chunks.isNotEmpty()) {
            chunkDao.insertChunks(task.chunks.map { DownloadChunkEntity.fromDomain(it) })
        }
    }

    override suspend fun updateTaskStatus(taskId: String, status: DownloadStatus, errorMessage: String?) {
        taskDao.updateStatus(taskId, status.name, errorMessage)
    }

    override suspend fun updateTaskProgress(taskId: String, downloadedBytes: Long, status: DownloadStatus) {
        taskDao.updateProgress(taskId, downloadedBytes, status.name)
    }

    override suspend fun updateTaskMetadata(
        taskId: String,
        totalBytes: Long,
        fileName: String,
        destinationPath: String
    ) {
        taskDao.updateMetadata(taskId, totalBytes, fileName, destinationPath)
    }

    override suspend fun deleteTask(taskId: String) {
        taskDao.deleteById(taskId)
    }

    override suspend fun saveChunks(taskId: String, chunks: List<DownloadChunk>) {
        chunkDao.insertChunks(chunks.map { DownloadChunkEntity.fromDomain(it) })
    }

    override suspend fun updateChunkProgress(chunkId: Long, currentByte: Long) {
        chunkDao.updateProgress(chunkId, currentByte)
    }

    override suspend fun getChunksForTask(taskId: String): List<DownloadChunk> {
        return chunkDao.getChunksForTask(taskId).map { it.toDomain() }
    }
}
