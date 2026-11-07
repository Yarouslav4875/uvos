package com.uvos.downloader.data.network

import com.uvos.downloader.domain.model.DownloadChunk
import com.uvos.downloader.domain.model.DownloadStatus
import com.uvos.downloader.domain.model.DownloadTask
import com.uvos.downloader.domain.model.NetworkSpeedMetric
import com.uvos.downloader.domain.repository.DownloadRepository
import com.uvos.downloader.domain.usecase.CalculateSpeedMetricsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class DownloadEngine(
    private val repository: DownloadRepository,
    private val httpClient: OkHttpDownloadClient = OkHttpDownloadClient()
) {
    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val activeBytesDownloaded = AtomicLong(0L)
    private val speedCalculator = CalculateSpeedMetricsUseCase()

    private val _speedMetric = MutableStateFlow(NetworkSpeedMetric())
    val speedMetric: StateFlow<NetworkSpeedMetric> = _speedMetric.asStateFlow()

    init {
        // Ticker to compute speed metrics periodically
        engineScope.launch {
            while (isActive) {
                delay(300)
                val activeCount = activeJobs.size
                val currentBytes = activeBytesDownloaded.get()
                val metric = speedCalculator.invoke(
                    currentTotalBytes = currentBytes,
                    activeConnections = activeCount,
                    isWifi = true
                )
                _speedMetric.value = metric
            }
        }
    }

    fun startTask(taskId: String) {
        if (activeJobs.containsKey(taskId)) return

        val job = engineScope.launch {
            try {
                executeTask(taskId)
            } catch (_: CancellationException) {
                repository.updateTaskStatus(taskId, DownloadStatus.PAUSED)
            } catch (e: Exception) {
                repository.updateTaskStatus(taskId, DownloadStatus.ERROR, e.message ?: "Unknown error")
            } finally {
                activeJobs.remove(taskId)
                if (activeJobs.isEmpty()) {
                    speedCalculator.reset()
                    _speedMetric.value = NetworkSpeedMetric()
                }
            }
        }

        activeJobs[taskId] = job
    }

    fun pauseTask(taskId: String) {
        val job = activeJobs.remove(taskId)
        job?.cancel()
        engineScope.launch {
            repository.updateTaskStatus(taskId, DownloadStatus.PAUSED)
        }
    }

    fun cancelTask(taskId: String) {
        val job = activeJobs.remove(taskId)
        job?.cancel()
        engineScope.launch {
            val task = repository.getTaskById(taskId)
            repository.deleteTask(taskId)
            if (task != null) {
                try {
                    File(task.destinationPath).delete()
                } catch (_: Exception) {}
            }
        }
    }

    private suspend fun executeTask(taskId: String) {
        var task = repository.getTaskById(taskId) ?: return
        repository.updateTaskStatus(taskId, DownloadStatus.DOWNLOADING)

        val targetFile = File(task.destinationPath)
        targetFile.parentFile?.mkdirs()

        // 1. Probe & Initialize Chunks if not yet done
        var chunks = repository.getChunksForTask(taskId)
        if (chunks.isEmpty() || task.totalBytes <= 0L) {
            val probe = httpClient.probe(task.url)
            val totalBytes = probe.totalBytes
            val updatedFileName = probe.suggestedFileName ?: task.fileName
            val actualFile = File(targetFile.parentFile ?: File("."), updatedFileName)

            repository.updateTaskMetadata(
                taskId = taskId,
                totalBytes = totalBytes,
                fileName = updatedFileName,
                destinationPath = actualFile.absolutePath
            )

            // Reload task with updated metadata
            task = repository.getTaskById(taskId) ?: return

            // Prepare pre-allocated file
            if (totalBytes > 0) {
                RandomAccessFile(actualFile, "rw").use { raf ->
                    raf.setLength(totalBytes)
                }
            }

            // Create chunks
            val threadCount = if (probe.supportsRanges && totalBytes > 1024 * 1024) {
                task.numThreads.coerceIn(2, 8)
            } else {
                1
            }

            chunks = createChunks(taskId, totalBytes, threadCount)
            repository.saveChunks(taskId, chunks)
            chunks = repository.getChunksForTask(taskId)
        }

        // 2. Execute parallel chunk downloads
        val currentFile = File(task.destinationPath)
        val chunkWorkers = chunks.map { chunk ->
            async(Dispatchers.IO) {
                val worker = ChunkWorker(
                    client = httpClient.client,
                    targetFile = currentFile
                ) { chunkId, currentByte, bytesDelta ->
                    activeBytesDownloaded.addAndGet(bytesDelta)
                    engineScope.launch {
                        repository.updateChunkProgress(chunkId, currentByte)
                    }
                }
                worker.downloadChunk(task.url, chunk)
            }
        }

        // Progress synchronization loop while workers are downloading
        val syncJob = engineScope.launch {
            while (isActive) {
                delay(500)
                val currentChunks = repository.getChunksForTask(taskId)
                val downloadedSum = currentChunks.sumOf { it.downloadedBytes }
                repository.updateTaskProgress(taskId, downloadedSum, DownloadStatus.DOWNLOADING)
            }
        }

        try {
            chunkWorkers.awaitAll()
        } finally {
            syncJob.cancel()
        }

        // 3. Final verification and complete
        val finalChunks = repository.getChunksForTask(taskId)
        val finalDownloaded = finalChunks.sumOf { it.downloadedBytes }
        val isAllFinished = finalChunks.all { it.isCompleted || (task.totalBytes > 0 && it.currentByte >= it.endByte) }

        if (isAllFinished || (task.totalBytes > 0 && finalDownloaded >= task.totalBytes)) {
            repository.updateTaskProgress(taskId, task.totalBytes.coerceAtLeast(finalDownloaded), DownloadStatus.COMPLETED)
        } else {
            repository.updateTaskProgress(taskId, finalDownloaded, DownloadStatus.PAUSED)
        }
    }

    private fun createChunks(taskId: String, totalBytes: Long, threadCount: Int): List<DownloadChunk> {
        val list = mutableListOf<DownloadChunk>()
        if (totalBytes <= 0 || threadCount <= 1) {
            list.add(
                DownloadChunk(
                    chunkId = 0,
                    taskId = taskId,
                    chunkIndex = 0,
                    startByte = 0,
                    endByte = totalBytes.coerceAtLeast(0),
                    currentByte = 0
                )
            )
            return list
        }

        val partSize = totalBytes / threadCount
        for (i in 0 until threadCount) {
            val start = i * partSize
            val end = if (i == threadCount - 1) (totalBytes - 1) else ((i + 1) * partSize - 1)
            list.add(
                DownloadChunk(
                    chunkId = 0,
                    taskId = taskId,
                    chunkIndex = i,
                    startByte = start,
                    endByte = end,
                    currentByte = start
                )
            )
        }
        return list
    }
}
