package com.uvos.notes.domain.model

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val folder: String = "General",
    val filePath: String = "",
    val isEncrypted: Boolean = false,
    val modifiedAt: Long = System.currentTimeMillis(),
    val snippet: String? = null
) {
    val wordCount: Int
        get() {
            if (isEncrypted && content.isEmpty()) return 0
            val trimmed = content.trim()
            if (trimmed.isEmpty()) return 0
            return trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        }

    val charCount: Int
        get() = if (isEncrypted && content.isEmpty()) 0 else content.length

    val readingTimeMinutes: Int
        get() = ((wordCount + 199) / 200).coerceAtLeast(1)
}
