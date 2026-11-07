package com.uvos.downloader.data.local.db

import com.uvos.downloader.data.local.entity.DownloadChunkEntity
import kotlinx.coroutines.flow.Flow

interface DownloadChunkDao {
    fun getChunksForTask(taskId: String): List<DownloadChunkEntity>
    fun observeChunksForTask(taskId: String): Flow<List<DownloadChunkEntity>>
    fun insertChunks(chunks: List<DownloadChunkEntity>)
    fun updateProgress(chunkId: Long, currentByte: Long)
    fun deleteChunksForTask(taskId: String)
}
