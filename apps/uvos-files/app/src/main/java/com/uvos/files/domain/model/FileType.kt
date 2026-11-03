package com.uvos.files.domain.model

enum class FileType {
    DIRECTORY,
    FILE,
    SYMLINK,
    BLOCK_DEVICE,
    CHARACTER_DEVICE,
    SOCKET,
    UNKNOWN
}
