package com.uvos.notes.data.local

import android.content.Context
import com.uvos.notes.domain.model.Note
import com.uvos.notes.domain.model.NoteFolder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

class NoteFileManager(
    private val context: Context,
    private val cryptoManager: CryptoManager
) {

    private val baseDir: File
        get() {
            val dir = File(context.filesDir, "notes")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    init {
        // Ensure standard folders exist
        getFolderDir("General")
        getFolderDir("Work")
        getFolderDir("Vault")
    }

    fun getFolderDir(folderName: String): File {
        val sanitized = sanitizeFolderName(folderName)
        val dir = File(baseDir, sanitized)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun listFolders(): List<NoteFolder> {
        val folders = mutableListOf<NoteFolder>()
        val dirs = baseDir.listFiles { f -> f.isDirectory } ?: emptyArray()
        for (dir in dirs) {
            val count = dir.listFiles { f -> f.isFile && (f.name.endsWith(".md") || f.name.endsWith(".uvsec")) }?.size ?: 0
            folders.add(NoteFolder(name = dir.name, noteCount = count))
        }
        if (folders.none { it.name.equals("General", ignoreCase = true) }) {
            folders.add(0, NoteFolder(name = "General", noteCount = 0))
        }
        return folders.sortedBy { it.name }
    }

    fun loadAllNotes(): List<Note> {
        val notes = mutableListOf<Note>()
        val folders = baseDir.listFiles { f -> f.isDirectory } ?: emptyArray()

        for (folder in folders) {
            val files = folder.listFiles { f ->
                f.isFile && (f.name.endsWith(".md") || f.name.endsWith(".uvsec"))
            } ?: emptyArray()

            for (file in files) {
                try {
                    val note = readNoteFile(file, folder.name)
                    notes.add(note)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Also check base directory for unorganized files
        val rootFiles = baseDir.listFiles { f ->
            f.isFile && (f.name.endsWith(".md") || f.name.endsWith(".uvsec"))
        } ?: emptyArray()
        for (file in rootFiles) {
            try {
                notes.add(readNoteFile(file, "General"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return notes.sortedByDescending { it.modifiedAt }
    }

    private fun readNoteFile(file: File, folderName: String): Note {
        val isEncrypted = file.name.endsWith(".uvsec")
        val rawName = file.nameWithoutExtension

        if (isEncrypted) {
            val bytes = FileInputStream(file).use { it.readBytes() }
            return Note(
                id = generateIdForFile(file),
                title = rawName,
                content = "", // Content stays encrypted until explicitly unlocked
                folder = folderName,
                filePath = file.absolutePath,
                isEncrypted = true,
                modifiedAt = file.lastModified(),
                snippet = "[Шифрованная заметка AES-256-GCM]"
            )
        } else {
            val content = file.readText(Charsets.UTF_8)
            val title = extractTitleFromContent(content, rawName)
            return Note(
                id = generateIdForFile(file),
                title = title,
                content = content,
                folder = folderName,
                filePath = file.absolutePath,
                isEncrypted = false,
                modifiedAt = file.lastModified()
            )
        }
    }

    fun saveNote(note: Note): Note {
        val targetFolder = if (note.isEncrypted) "Vault" else sanitizeFolderName(note.folder.ifBlank { "General" })
        val folderDir = getFolderDir(targetFolder)
        val extension = if (note.isEncrypted) ".uvsec" else ".md"

        val sanitizedTitle = sanitizeFileName(note.title.ifBlank { "Заметка_${System.currentTimeMillis()}" })
        val targetFile = File(folderDir, "$sanitizedTitle$extension")

        // If note had a previous file path that changed, remove old file
        if (note.filePath.isNotBlank()) {
            val oldFile = File(note.filePath)
            if (oldFile.exists() && oldFile.absolutePath != targetFile.absolutePath) {
                oldFile.delete()
            }
        }

        if (note.isEncrypted) {
            val payload = cryptoManager.encryptNote(note.content)
            val bytes = cryptoManager.serializePayload(payload)
            FileOutputStream(targetFile).use { it.write(bytes) }
        } else {
            targetFile.writeText(note.content, Charsets.UTF_8)
        }

        return note.copy(
            id = generateIdForFile(targetFile),
            folder = targetFolder,
            filePath = targetFile.absolutePath,
            modifiedAt = targetFile.lastModified()
        )
    }

    fun unlockEncryptedNote(note: Note): Note {
        if (!note.isEncrypted || note.filePath.isBlank()) return note
        val file = File(note.filePath)
        if (!file.exists()) return note

        val bytes = FileInputStream(file).use { it.readBytes() }
        val payload = cryptoManager.deserializePayload(bytes)
        val decryptedText = cryptoManager.decryptNote(payload)
        return note.copy(content = decryptedText)
    }

    fun deleteNoteFile(filePath: String): Boolean {
        if (filePath.isBlank()) return false
        val file = File(filePath)
        return if (file.exists()) file.delete() else false
    }

    private fun extractTitleFromContent(content: String, fallback: String): String {
        val firstLine = content.lineSequence().firstOrNull { it.isNotBlank() }?.trim() ?: return fallback
        if (firstLine.startsWith("#")) {
            return firstLine.replace(Regex("^#+\\s*"), "").trim()
        }
        return if (firstLine.length > 50) firstLine.take(47) + "..." else firstLine
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|\\r\\n]"), "_").trim().ifBlank { "note" }
    }

    private fun sanitizeFolderName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|\\r\\n.]"), "_").trim().ifBlank { "General" }
    }

    private fun generateIdForFile(file: File): String {
        return file.absolutePath.hashCode().toString()
    }
}
