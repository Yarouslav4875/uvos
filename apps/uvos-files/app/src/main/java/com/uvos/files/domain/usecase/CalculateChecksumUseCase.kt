package com.uvos.files.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.InputStream
import java.security.MessageDigest

data class HashCalculationProgress(
    val bytesProcessed: Long,
    val totalBytes: Long,
    val percentage: Float,
    val finalHash: String? = null,
    val speedBytesPerSec: Long = 0
)

class CalculateChecksumUseCase {

    operator fun invoke(
        inputStream: InputStream,
        totalBytes: Long,
        algorithm: String = "SHA-256"
    ): Flow<HashCalculationProgress> = flow {
        val digest = MessageDigest.getInstance(algorithm)
        val buffer = ByteArray(64 * 1024)
        var bytesRead: Int
        var totalRead: Long = 0
        var lastTime = System.currentTimeMillis()
        var lastBytes: Long = 0
        var currentSpeed: Long = 0

        inputStream.use { stream ->
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
                totalRead += bytesRead

                val now = System.currentTimeMillis()
                val elapsed = now - lastTime
                if (elapsed >= 200) {
                    currentSpeed = if (elapsed > 0) ((totalRead - lastBytes) * 1000) / elapsed else 0
                    lastBytes = totalRead
                    lastTime = now
                    val progress = if (totalBytes > 0) (totalRead.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
                    emit(HashCalculationProgress(totalRead, totalBytes, progress, null, currentSpeed))
                }
            }
        }

        val hashString = digest.digest().joinToString("") { "%02x".format(it) }
        emit(HashCalculationProgress(totalRead, totalBytes, 1.0f, hashString, 0))
    }.flowOn(Dispatchers.IO)
}
