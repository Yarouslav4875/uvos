package com.uvos.gallery.data.local.vault

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.uvos.gallery.domain.model.VaultItem

@Entity(
    tableName = "vault_items",
    indices = [Index(value = ["created_timestamp"])]
)
data class VaultEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "original_name") val originalName: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "file_size") val fileSize: Long,
    @ColumnInfo(name = "encrypted_file_path") val encryptedFilePath: String,
    @ColumnInfo(name = "created_timestamp") val createdTimestamp: Long,
    @ColumnInfo(name = "encrypted_timestamp") val encryptedTimestamp: Long,
    @ColumnInfo(name = "original_relative_path") val originalRelativePath: String,
    @ColumnInfo(name = "iv_base64") val ivBase64: String
)

fun VaultEntity.toDomain(): VaultItem {
    return VaultItem(
        id = id,
        originalName = originalName,
        mimeType = mimeType,
        fileSize = fileSize,
        encryptedFilePath = encryptedFilePath,
        createdTimestamp = createdTimestamp,
        encryptedTimestamp = encryptedTimestamp,
        originalRelativePath = originalRelativePath,
        ivBase64 = ivBase64
    )
}

fun VaultItem.toEntity(): VaultEntity {
    return VaultEntity(
        id = id,
        originalName = originalName,
        mimeType = mimeType,
        fileSize = fileSize,
        encryptedFilePath = encryptedFilePath,
        createdTimestamp = createdTimestamp,
        encryptedTimestamp = encryptedTimestamp,
        originalRelativePath = originalRelativePath,
        ivBase64 = ivBase64
    )
}
