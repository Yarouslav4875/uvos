package com.uvos.gallery.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.uvos.gallery.data.local.mediastore.MediaStoreScanner
import com.uvos.gallery.domain.model.MediaItem
import com.uvos.gallery.domain.repository.MediaRepository
import com.uvos.gallery.domain.usecase.ExifScrubberEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class MediaRepositoryImpl(
    private val context: Context,
    private val scanner: MediaStoreScanner,
    private val scrubberEngine: ExifScrubberEngine
) : MediaRepository {

    private val _mediaFlow = MutableStateFlow<List<MediaItem>>(emptyList())

    override fun observePublicMedia(): Flow<List<MediaItem>> = _mediaFlow.asStateFlow()

    override suspend fun scanMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = scanner.queryAllMedia()
        _mediaFlow.value = items
        return@withContext items
    }

    override suspend fun deleteMedia(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val rowsDeleted = context.contentResolver.delete(item.contentUri, null, null)
            val success = rowsDeleted > 0 || (item.filePath != null && File(item.filePath).delete())
            if (success) {
                _mediaFlow.value = _mediaFlow.value.filter { it.id != item.id }
            }
            success
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun sanitizeExifInPlace(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        val file = item.filePath?.let { File(it) }
        val success = if (file != null && file.exists()) {
            scrubberEngine.scrubInPlace(file)
        } else {
            scrubberEngine.scrubInPlaceUri(item.contentUri)
        }
        if (success) {
            // Trigger rescan to refresh security indicators
            scanMedia()
        }
        success
    }

    override suspend fun generateScrubbedExportUri(item: MediaItem): Uri = withContext(Dispatchers.IO) {
        val scrubbedFile = scrubberEngine.createScrubbedExportCopy(item.contentUri)
        return@withContext FileProvider.getUriForFile(
            context,
            "com.uvos.gallery.provider",
            scrubbedFile
        )
    }
}
