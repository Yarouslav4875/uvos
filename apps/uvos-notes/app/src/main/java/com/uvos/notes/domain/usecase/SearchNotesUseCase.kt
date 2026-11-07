package com.uvos.notes.domain.usecase

import com.uvos.notes.data.repository.NotesRepositoryImpl
import com.uvos.notes.domain.model.Note

class SearchNotesUseCase(
    private val repository: NotesRepositoryImpl
) {
    suspend operator fun invoke(query: String): List<Note> {
        return repository.searchNotes(query)
    }
}
