package com.uvos.gallery.domain.model

data class VaultItem(
    val id: String,
    val originalName: String,
    val mimeType: String,
    val fileSize: Long,
    val encryptedFilePath: String,
    val createdTimestamp: Long,
    val encryptedTimestamp: Long,
    val originalRelativePath: String,
    val ivBase64: String
)
