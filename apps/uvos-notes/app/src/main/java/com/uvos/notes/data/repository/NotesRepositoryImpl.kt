package com.uvos.notes.data.repository

import com.uvos.notes.data.local.CryptoManager
import com.uvos.notes.data.local.NoteFileManager
import com.uvos.notes.data.local.NotesIndexDatabase
import com.uvos.notes.domain.model.Note
import com.uvos.notes.domain.model.NoteFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class NotesRepositoryImpl(
    private val fileManager: NoteFileManager,
    private val indexDb: NotesIndexDatabase,
    private val cryptoManager: CryptoManager
) {
    private val _notesFlow = MutableStateFlow<List<Note>>(emptyList())
    val notesFlow: Flow<List<Note>> = _notesFlow.asStateFlow()

    private val _foldersFlow = MutableStateFlow<List<NoteFolder>>(emptyList())
    val foldersFlow: Flow<List<NoteFolder>> = _foldersFlow.asStateFlow()

    suspend fun syncNotes() = withContext(Dispatchers.IO) {
        val diskNotes = fileManager.loadAllNotes()
        indexDb.rebuildIndex(diskNotes)
        _notesFlow.value = diskNotes
        _foldersFlow.value = fileManager.listFolders()
    }

    suspend fun saveNote(note: Note): Note = withContext(Dispatchers.IO) {
        val saved = fileManager.saveNote(note)
        indexDb.indexNote(saved)

        val current = _notesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == saved.id || (it.filePath == saved.filePath && it.filePath.isNotBlank()) }
        if (index >= 0) {
            current[index] = saved
        } else {
            current.add(0, saved)
        }
        _notesFlow.value = current.sortedByDescending { it.modifiedAt }
        _foldersFlow.value = fileManager.listFolders()
        saved
    }

    suspend fun deleteNote(id: String, filePath: String) = withContext(Dispatchers.IO) {
        fileManager.deleteNoteFile(filePath)
        indexDb.deleteNote(id)

        _notesFlow.value = _notesFlow.value.filterNot { it.id == id || it.filePath == filePath }
        _foldersFlow.value = fileManager.listFolders()
    }

    suspend fun searchNotes(query: String): List<Note> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            return@withContext _notesFlow.value
        }
        indexDb.searchNotes(query)
    }

    suspend fun unlockNote(note: Note): Note = withContext(Dispatchers.IO) {
        fileManager.unlockEncryptedNote(note)
    }

    fun getNoteById(id: String): Note? {
        return _notesFlow.value.find { it.id == id }
    }

    suspend fun createFolder(folderName: String) = withContext(Dispatchers.IO) {
        fileManager.getFolderDir(folderName)
        _foldersFlow.value = fileManager.listFolders()
    }
}
