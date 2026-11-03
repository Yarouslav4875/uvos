package com.uvos.gallery.domain.repository

import android.net.Uri
import com.uvos.gallery.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun observePublicMedia(): Flow<List<MediaItem>>
    suspend fun scanMedia(): List<MediaItem>
    suspend fun deleteMedia(item: MediaItem): Boolean
    suspend fun sanitizeExifInPlace(item: MediaItem): Boolean
    suspend fun generateScrubbedExportUri(item: MediaItem): Uri
}
