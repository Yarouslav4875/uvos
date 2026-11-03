package com.uvos.files.domain.model

data class FileMetadata(
    val path: String,
    val sizeBytes: Long,
    val permissions: PosixPermissions?,
    val owner: String,
    val group: String,
    val lastModified: Long,
    val isDirectory: Boolean,
    val selinuxContext: String? = null
)
