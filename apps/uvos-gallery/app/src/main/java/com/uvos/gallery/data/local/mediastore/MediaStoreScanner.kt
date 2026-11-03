package com.uvos.gallery.data.local.mediastore

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.uvos.gallery.domain.model.MediaItem
import com.uvos.gallery.domain.model.MediaType
import com.uvos.gallery.domain.model.ThreatLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class MediaStoreScanner(private val context: Context) {

    suspend fun queryAllMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()
        mediaList.addAll(queryImages())
        mediaList.addAll(queryVideos())
        mediaList.sortByDescending { it.dateModified }
        return@withContext mediaList
    }

    private fun queryImages(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.DATA
        )

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "IMG_$id.jpg"
                    val mime = cursor.getString(mimeCol) ?: "image/jpeg"
                    val size = cursor.getLong(sizeCol)
                    val dateModified = cursor.getLong(dateCol)
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)
                    val filePath = cursor.getString(dataCol)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val resolutionText = if (width > 0 && height > 0) {
                        "${width}x${height}  ${formatSize(size)}"
                    } else {
                        formatSize(size)
                    }

                    val threatLevel = evaluateThreatLevel(contentUri)

                    items.add(
                        MediaItem(
                            id = id,
                            contentUri = contentUri,
                            name = name,
                            mimeType = mime,
                            size = size,
                            dateModified = dateModified,
                            mediaType = MediaType.IMAGE,
                            resolutionText = resolutionText,
                            threatLevel = threatLevel,
                            filePath = filePath
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Log/handle permission or cursor read issues in sandbox
        }
        return items
    }

    private fun queryVideos(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATA
        )

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "VID_$id.mp4"
                    val mime = cursor.getString(mimeCol) ?: "video/mp4"
                    val size = cursor.getLong(sizeCol)
                    val dateModified = cursor.getLong(dateCol)
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)
                    val duration = cursor.getLong(durationCol)
                    val filePath = cursor.getString(dataCol)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val res = if (width > 0 && height > 0) "${width}x${height} " else ""
                    val resolutionText = "$res[VIDEO ${formatDuration(duration)}]  ${formatSize(size)}"

                    items.add(
                        MediaItem(
                            id = id,
                            contentUri = contentUri,
                            name = name,
                            mimeType = mime,
                            size = size,
                            dateModified = dateModified,
                            mediaType = MediaType.VIDEO,
                            resolutionText = resolutionText,
                            threatLevel = ThreatLevel.STERILE,
                            filePath = filePath,
                            durationMs = duration
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Handled
        }
        return items
    }

    private fun evaluateThreatLevel(uri: Uri): ThreatLevel {
        return try {
            val stream: InputStream? = context.contentResolver.openInputStream(uri)
            if (stream == null) return ThreatLevel.STERILE

            stream.use {
                val exif = ExifInterface(it)
                if (exif.latLong != null) {
                    ThreatLevel.HIGH_RISK
                } else {
                    val model = exif.getAttribute(ExifInterface.TAG_MODEL)
                    val make = exif.getAttribute(ExifInterface.TAG_MAKE)
                    val date = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    if (model != null || make != null || date != null) {
                        ThreatLevel.MEDIUM_RISK
                    } else {
                        ThreatLevel.STERILE
                    }
                }
            }
        } catch (e: Exception) {
            ThreatLevel.STERILE
        }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%d KB", bytes / 1024)
            else -> "$bytes B"
        }
    }

    private fun formatDuration(ms: Long): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format("%02d:%02d", mins, secs)
    }
}
