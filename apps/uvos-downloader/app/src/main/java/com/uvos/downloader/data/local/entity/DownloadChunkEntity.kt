package com.uvos.downloader.data.local.entity

import com.uvos.downloader.domain.model.DownloadChunk

data class DownloadChunkEntity(
    val chunkId: Long = 0,
    val taskId: String,
    val chunkIndex: Int,
    val startByte: Long,
    val endByte: Long,
    val currentByte: Long
) {
    fun toDomain(): DownloadChunk {
        return DownloadChunk(
            chunkId = chunkId,
            taskId = taskId,
            chunkIndex = chunkIndex,
            startByte = startByte,
            endByte = endByte,
            currentByte = currentByte
        )
    }

    companion object {
        fun fromDomain(domain: DownloadChunk): DownloadChunkEntity {
            return DownloadChunkEntity(
                chunkId = domain.chunkId,
                taskId = domain.taskId,
                chunkIndex = domain.chunkIndex,
                startByte = domain.startByte,
                endByte = domain.endByte,
                currentByte = domain.currentByte
            )
        }
    }
}
