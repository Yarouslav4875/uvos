package com.uvos.notes.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uvos.notes.data.repository.NotesRepositoryImpl
import com.uvos.notes.domain.model.Note
import com.uvos.notes.domain.model.NoteFolder
import com.uvos.notes.domain.usecase.EncryptNoteUseCase
import com.uvos.notes.domain.usecase.SaveNoteUseCase
import com.uvos.notes.domain.usecase.SearchNotesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val folders: List<NoteFolder> = emptyList(),
    val selectedFolder: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

class NotesViewModel(
    private val repository: NotesRepositoryImpl,
    private val saveNoteUseCase: SaveNoteUseCase,
    private val encryptNoteUseCase: EncryptNoteUseCase,
    private val searchNotesUseCase: SearchNotesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState(isLoading = true))
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            repository.notesFlow.collect { allNotes ->
                applyFilterAndSearch(allNotes, _uiState.value.selectedFolder, _uiState.value.searchQuery)
            }
        }
        viewModelScope.launch {
            repository.foldersFlow.collect { allFolders ->
                _uiState.value = _uiState.value.copy(folders = allFolders)
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.syncNotes()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(150) // Small debounce for snappy typing
            val results = if (query.isBlank()) {
                null
            } else {
                searchNotesUseCase(query)
            }
            applyFilterAndSearch(
                allNotes = results,
                folder = _uiState.value.selectedFolder,
                query = query
            )
        }
    }

    fun onSelectFolder(folder: String?) {
        _uiState.value = _uiState.value.copy(selectedFolder = folder)
        viewModelScope.launch {
            val baseList = if (_uiState.value.searchQuery.isBlank()) {
                null
            } else {
                searchNotesUseCase(_uiState.value.searchQuery)
            }
            applyFilterAndSearch(baseList, folder, _uiState.value.searchQuery)
        }
    }

    private suspend fun applyFilterAndSearch(allNotes: List<Note>?, folder: String?, query: String) {
        val list = allNotes ?: if (query.isNotBlank()) searchNotesUseCase(query) else emptyList()
        val baseNotes = if (query.isBlank() && allNotes == null) {
            repository.searchNotes("")
        } else {
            list
        }

        val filtered = when {
            folder.isNullOrBlank() || folder == "Все" -> baseNotes
            folder == "Сейф" || folder == "Vault" -> baseNotes.filter { it.isEncrypted }
            else -> baseNotes.filter { it.folder.equals(folder, ignoreCase = true) && !it.isEncrypted }
        }

        _uiState.value = _uiState.value.copy(notes = filtered)
    }

    fun saveNote(note: Note, onComplete: ((Note) -> Unit)? = null) {
        viewModelScope.launch {
            val saved = saveNoteUseCase(note)
            onComplete?.invoke(saved)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note.id, note.filePath)
        }
    }

    fun unlockNote(note: Note, onUnlocked: (Note) -> Unit) {
        viewModelScope.launch {
            val decrypted = encryptNoteUseCase.decrypt(note)
            onUnlocked(decrypted)
        }
    }

    fun createNote(folder: String = "General", isEncrypted: Boolean = false): Note {
        val id = UUID.randomUUID().toString()
        val title = if (isEncrypted) "Секретная заметка" else "Новая заметка"
        return Note(
            id = id,
            title = title,
            content = "# $title\n\n",
            folder = if (isEncrypted) "Vault" else folder,
            isEncrypted = isEncrypted,
            modifiedAt = System.currentTimeMillis()
        )
    }

    class Factory(
        private val repository: NotesRepositoryImpl,
        private val saveNoteUseCase: SaveNoteUseCase,
        private val encryptNoteUseCase: EncryptNoteUseCase,
        private val searchNotesUseCase: SearchNotesUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NotesViewModel(repository, saveNoteUseCase, encryptNoteUseCase, searchNotesUseCase) as T
        }
    }
}
