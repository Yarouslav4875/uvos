package com.uvos.notes.domain.usecase

import com.uvos.notes.data.repository.NotesRepositoryImpl
import com.uvos.notes.domain.model.Note

class EncryptNoteUseCase(
    private val repository: NotesRepositoryImpl
) {
    suspend fun encrypt(note: Note): Note {
        val encryptedNote = note.copy(
            isEncrypted = true,
            folder = "Vault"
        )
        return repository.saveNote(encryptedNote)
    }

    suspend fun decrypt(note: Note): Note {
        return repository.unlockNote(note)
    }
}
