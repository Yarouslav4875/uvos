package com.uvos.notes.domain.usecase

import com.uvos.notes.data.repository.NotesRepositoryImpl
import com.uvos.notes.domain.model.Note

class SaveNoteUseCase(
    private val repository: NotesRepositoryImpl
) {
    suspend operator fun invoke(note: Note): Note {
        return repository.saveNote(note)
    }
}
