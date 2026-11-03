package com.uvos.gallery.domain.usecase

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.exifinterface.media.ExifInterface
import com.uvos.gallery.domain.model.ExifSecurityReport
import com.uvos.gallery.domain.model.ThreatLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ExifScrubberEngine(private val context: Context) {

    // Exhaustive registry of identifying and privacy-compromising metadata tags
    private val criticalTags = listOf(
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_SUBSEC_TIME,
        ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
        ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
        ExifInterface.TAG_BODY_SERIAL_NUMBER,
        ExifInterface.TAG_LENS_MAKE,
        ExifInterface.TAG_LENS_MODEL,
        ExifInterface.TAG_LENS_SERIAL_NUMBER,
        ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION,
        ExifInterface.TAG_IMAGE_UNIQUE_ID,
        ExifInterface.TAG_CAMERA_OWNER_NAME,
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_USER_COMMENT
    )

    /**
     * Inspect and analyze current metadata exposure of a media file
     */
    suspend fun analyzeSecurity(uri: Uri): ExifSecurityReport = withContext(Dispatchers.IO) {
        val inputStream: InputStream? = try {
            context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            null
        }

        if (inputStream == null) {
            return@withContext ExifSecurityReport(
                threatLevel = ThreatLevel.STERILE,
                hasGpsCoordinates = false
            )
        }

        inputStream.use { stream ->
            val exif = ExifInterface(stream)
            val latLong = exif.latLong
            val hasGps = latLong != null
            val make = exif.getAttribute(ExifInterface.TAG_MAKE)
            val model = exif.getAttribute(ExifInterface.TAG_MODEL)
            val date = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            val lens = exif.getAttribute(ExifInterface.TAG_LENS_MODEL)
            val serial = exif.getAttribute(ExifInterface.TAG_BODY_SERIAL_NUMBER)
            val software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)

            val threatLevel = when {
                hasGps -> ThreatLevel.HIGH_RISK
                model != null || make != null || date != null || serial != null -> ThreatLevel.MEDIUM_RISK
                else -> ThreatLevel.STERILE
            }

            val attributes = mutableMapOf<String, String>()
            if (make != null) attributes["Make"] = make
            if (model != null) attributes["Model"] = model
            if (date != null) attributes["Date"] = date
            if (lens != null) attributes["Lens"] = lens
            if (serial != null) attributes["Serial"] = serial
            if (software != null) attributes["Software"] = software
            if (hasGps && latLong != null) {
                attributes["Latitude"] = String.format("%.6f", latLong[0])
                attributes["Longitude"] = String.format("%.6f", latLong[1])
            }

            ExifSecurityReport(
                threatLevel = threatLevel,
                hasGpsCoordinates = hasGps,
                latitude = latLong?.get(0),
                longitude = latLong?.get(1),
                deviceModel = if (model != null) "$make $model".trim() else make,
                timestamp = date,
                lensModel = lens,
                serialNumber = serial,
                software = software,
                attributesMap = attributes
            )
        }
    }

    /**
     * Creates an in-memory or temporary scrubbed copy of the file for safe zero-leak sharing
     */
    suspend fun createScrubbedExportCopy(sourceUri: Uri): File = withContext(Dispatchers.IO) {
        val scrubbedDir = File(context.cacheDir, "scrubbed").apply { mkdirs() }
        val tempFile = File(scrubbedDir, "UVOS_CLEAN_${System.currentTimeMillis()}.jpg")

        // 1. Copy raw bytes to sanitized temp file
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Could not open source media file")

        // 2. Strip all critical and fingerprint tags
        val exif = ExifInterface(tempFile.absolutePath)
        for (tag in criticalTags) {
            exif.setAttribute(tag, null)
        }

        // 3. Strip embedded EXIF thumbnail
        exif.removeThumbnail()

        // 4. Save sanitized headers without pixel recompression
        exif.saveAttributes()

        return@withContext tempFile
    }

    /**
     * In-place permanent EXIF/GPS sterilization
     */
    suspend fun scrubInPlace(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || !file.canWrite()) return@withContext false
            val exif = ExifInterface(file.absolutePath)
            for (tag in criticalTags) {
                exif.setAttribute(tag, null)
            }
            exif.removeThumbnail()
            exif.saveAttributes()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * In-place scrub via ContentResolver ParcelFileDescriptor if direct file path is restricted
     */
    suspend fun scrubInPlaceUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "rw")
            pfd?.use {
                val exif = ExifInterface(it.fileDescriptor)
                for (tag in criticalTags) {
                    exif.setAttribute(tag, null)
                }
                exif.removeThumbnail()
                exif.saveAttributes()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
