package com.uvos.gallery.presentation.viewer

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.uvos.gallery.data.local.crypto.AesGcmVaultEngine
import java.io.File
import java.io.InputStream

class EncryptedMediaDataSource(
    private val cryptoEngine: AesGcmVaultEngine,
    private val encryptedFile: File
) : BaseDataSource(/* isNetwork = */ false) {

    private var inputStream: InputStream? = null
    private var bytesRemaining: Long = 0
    private var isOpen = false

    override fun open(dataSpec: DataSpec): Long {
        inputStream = cryptoEngine.createDecryptedStream(encryptedFile)

        // Support position seek
        if (dataSpec.position > 0) {
            var toSkip = dataSpec.position
            while (toSkip > 0) {
                val skipped = inputStream?.skip(toSkip) ?: 0L
                if (skipped <= 0) break
                toSkip -= skipped
            }
        }

        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            val totalRawLen = encryptedFile.length()
            val netLength = maxOf(0L, totalRawLen - (AesGcmVaultEngine.MAGIC_HEADER.size + 12 + 16))
            maxOf(0L, netLength - dataSpec.position)
        }

        isOpen = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val bytesToRead = if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
            length
        } else {
            minOf(bytesRemaining, length.toLong()).toInt()
        }

        val bytesRead = inputStream?.read(buffer, offset, bytesToRead) ?: -1
        if (bytesRead == -1) return C.RESULT_END_OF_INPUT

        if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            bytesRemaining -= bytesRead
        }
        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri = Uri.fromFile(encryptedFile)

    override fun close() {
        if (isOpen) {
            isOpen = false
            inputStream?.close()
            inputStream = null
            transferEnded()
        }
    }

    class Factory(
        private val cryptoEngine: AesGcmVaultEngine,
        private val encryptedFile: File
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return EncryptedMediaDataSource(cryptoEngine, encryptedFile)
        }
    }
}
