package com.uvos.downloader.data.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class OkHttpDownloadClient {

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    data class ProbeResult(
        val totalBytes: Long,
        val supportsRanges: Boolean,
        val suggestedFileName: String?
    )

    fun probe(url: String): ProbeResult {
        // Try HEAD first
        try {
            val headRequest = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", "UVOS-Downloader/1.0")
                .build()

            client.newCall(headRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val lengthHeader = response.header("Content-Length")?.toLongOrNull() ?: -1L
                    val acceptRanges = response.header("Accept-Ranges")
                    val disposition = response.header("Content-Disposition")
                    val fileName = parseFileNameFromDisposition(disposition)
                    val supportsRanges = acceptRanges?.contains("bytes", ignoreCase = true) == true

                    if (lengthHeader > 0) {
                        return ProbeResult(
                            totalBytes = lengthHeader,
                            supportsRanges = supportsRanges,
                            suggestedFileName = fileName
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Fall back to Range probe
        }

        // Fallback: Range GET bytes=0-0 probe
        try {
            val rangeRequest = Request.Builder()
                .url(url)
                .get()
                .header("Range", "bytes=0-0")
                .header("User-Agent", "UVOS-Downloader/1.0")
                .build()

            client.newCall(rangeRequest).execute().use { response ->
                val disposition = response.header("Content-Disposition")
                val fileName = parseFileNameFromDisposition(disposition)

                if (response.code == 206) {
                    // 206 Partial Content: Content-Range format: bytes 0-0/4728912
                    val contentRange = response.header("Content-Range")
                    val total = contentRange?.substringAfterLast('/')?.toLongOrNull() ?: -1L
                    return ProbeResult(
                        totalBytes = total,
                        supportsRanges = true,
                        suggestedFileName = fileName
                    )
                } else if (response.isSuccessful) {
                    val lengthHeader = response.header("Content-Length")?.toLongOrNull() ?: -1L
                    return ProbeResult(
                        totalBytes = lengthHeader,
                        supportsRanges = false,
                        suggestedFileName = fileName
                    )
                }
            }
        } catch (e: Exception) {
            throw IOException("Probe failed: ${e.message}", e)
        }

        return ProbeResult(
            totalBytes = -1L,
            supportsRanges = false,
            suggestedFileName = null
        )
    }

    private fun parseFileNameFromDisposition(disposition: String?): String? {
        if (disposition == null) return null
        val filenameMarker = "filename="
        val index = disposition.indexOf(filenameMarker, ignoreCase = true)
        if (index != -1) {
            var raw = disposition.substring(index + filenameMarker.length).trim()
            if (raw.startsWith('"')) {
                raw = raw.removePrefix("\"").substringBefore('"')
            } else {
                raw = raw.substringBefore(';').trim()
            }
            if (raw.isNotBlank()) return raw
        }
        return null
    }
}
