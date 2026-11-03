package com.uvos.files.domain.model

data class FileItem(
    val name: String,
    val absolutePath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModifiedEpochMs: Long,
    val fileType: FileType,
    val permissions: PosixPermissions? = null,
    val owner: String = "root",
    val group: String = "root",
    val isHidden: Boolean = name.startsWith(".")
) {
    val formattedSize: String
        get() {
            if (isDirectory) return "<DIR>"
            if (sizeBytes < 1024) return "$sizeBytes B"
            val kb = sizeBytes / 1024.0
            if (kb < 1024) return "%.1f KB".format(kb)
            val mb = kb / 1024.0
            if (mb < 1024) return "%.1f MB".format(mb)
            val gb = mb / 1024.0
            return "%.2f GB".format(gb)
        }

    val isConfigFile: Boolean
        get() {
            val lower = name.lowercase()
            return lower.endsWith(".conf") ||
                    lower.endsWith(".prop") ||
                    lower.endsWith(".rc") ||
                    lower.endsWith(".xml") ||
                    lower.endsWith(".sh") ||
                    lower.endsWith(".json") ||
                    lower.endsWith(".txt") ||
                    lower.endsWith(".log") ||
                    lower == "hosts" ||
                    lower == "build.prop"
        }
}
