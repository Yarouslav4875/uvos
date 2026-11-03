package com.uvos.gallery.domain.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val contentUri: Uri,
    val name: String,
    val mimeType: String,
    val size: Long,
    val dateModified: Long,
    val mediaType: MediaType,
    val resolutionText: String,
    val threatLevel: ThreatLevel = ThreatLevel.STERILE,
    val filePath: String? = null,
    val durationMs: Long = 0L,
    val isVaultItem: Boolean = false,
    val vaultId: String? = null
)
