package com.uvos.downloader.data.network

import com.uvos.downloader.domain.model.DownloadChunk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile

class ChunkWorker(
    private val client: OkHttpClient,
    private val targetFile: File,
    private val onProgress: (chunkId: Long, currentByte: Long, bytesReadDelta: Long) -> Unit
) {
    suspend fun downloadChunk(url: String, chunk: DownloadChunk) {
        val startFrom = chunk.currentByte
        val endAt = chunk.endByte

        // Chunk already complete?
        if (startFrom > endAt) {
            return
        }

        val requestBuilder = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", "UVOS-Downloader/1.0")

        if (endAt > 0) {
            requestBuilder.header("Range", "bytes=$startFrom-$endAt")
        }

        val request = requestBuilder.build()
        val call = client.newCall(request)

        var raf: RandomAccessFile? = null
        var inputStream: InputStream? = null

        try {
            val response = call.execute()
            if (!response.isSuccessful && response.code != 206) {
                throw IOException("Server returned HTTP ${response.code} for chunk ${chunk.chunkIndex}")
            }

            val body = response.body ?: throw IOException("Empty response body for chunk ${chunk.chunkIndex}")
            inputStream = body.byteStream()

            raf = RandomAccessFile(targetFile, "rw")
            raf.seek(startFrom)

            val buffer = ByteArray(32 * 1024) // 32 KB buffer
            var bytesRead: Int
            var currentPos = startFrom

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                currentCoroutineContext().ensureActive()

                // Thread-safe seek and write per chunk
                synchronized(targetFile) {
                    raf.seek(currentPos)
                    raf.write(buffer, 0, bytesRead)
                }

                currentPos += bytesRead
                onProgress(chunk.chunkId, currentPos, bytesRead.toLong())

                if (endAt in 1..currentPos) {
                    break
                }
            }
        } catch (e: CancellationException) {
            call.cancel()
            throw e
        } catch (e: Exception) {
            call.cancel()
            throw IOException("Chunk ${chunk.chunkIndex} download error: ${e.message}", e)
        } finally {
            try {
                inputStream?.close()
            } catch (_: Exception) {}
            try {
                raf?.close()
            } catch (_: Exception) {}
        }
    }
}
